#!/usr/bin/env python3
"""Run only against the disposable loopback BenchmarkServerTest fixture."""
import argparse, datetime, hashlib, json, os, pathlib, platform, re, shutil, subprocess, time, urllib.request, urllib.error
ROOT = pathlib.Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('--rps', type=float, default=10)
parser.add_argument('--seconds', type=int, default=10)
parser.add_argument('--repetitions', type=int, default=3)
parser.add_argument('--keep-server', action='store_true')
parser.add_argument('--stages', nargs='+', choices=['baseline','paged','indexed','analyzed'], default=['baseline','paged','indexed','analyzed'])
args = parser.parse_args()
runtime_file = ROOT / 'build/benchmark/runtime.properties'
deadline=time.monotonic()+60
while not runtime_file.exists() and time.monotonic()<deadline: time.sleep(1)
if not runtime_file.exists(): raise SystemExit('Benchmark fixture was not ready within 60 seconds')
props = {}
for line in runtime_file.read_text().splitlines():
    if line and not line.startswith('#'):
        k, v = line.split('=', 1); props[k] = v.replace('\\:', ':').replace('\\=', '=')
base = props['baseUrl']
if not base.startswith('http://127.0.0.1:'):
    raise SystemExit('Only the loopback test fixture is supported')
authorization = 'Bearer ' + props['accessToken']
run_id = datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%SZ')
output = ROOT / 'build/benchmark/results' / run_id
output.mkdir(parents=True)
environment = {k:v for k,v in props.items() if k != 'accessToken'}
environment.update({'rps':args.rps,'seconds':args.seconds,'repetitions':args.repetitions,'seed':20261107,
                    'platform':platform.platform(),'warmupSeconds':5,'measurement':'warm cache loopback HTTP, same host load generator','requestTimeoutSeconds':5,'maxRunSeconds':args.seconds+15,'logging':'WARN','stages':args.stages})
environment['head'] = subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()
environment['diffSha256'] = hashlib.sha256(subprocess.check_output(['git','diff','--','src','build.gradle'],cwd=ROOT)).hexdigest()
source_digest=hashlib.sha256()
for file in sorted((ROOT/'src').rglob('*')):
    if file.is_file() and file.suffix in ('.java','.lua'):
        source_digest.update(str(file.relative_to(ROOT)).encode());source_digest.update(file.read_bytes())
environment['sourceSha256']=source_digest.hexdigest()
environment['javaVersion']=subprocess.check_output([str(pathlib.Path(os.environ['JAVA_HOME'])/'bin/java'),'-version'],stderr=subprocess.STDOUT,text=True)
environment['resources']={'appHeap':'Xms256m Xmx512m','mysqlCpu':1,'mysqlMemoryMiB':512,'redisCpu':0.5,'redisMemoryMiB':128,'loadGenerator':'same host'}
for container,cpu,memory in ((props['mysqlContainer'],'1','512m'),(props['redisContainer'],'0.5','128m')):
    subprocess.run(['docker','update','--cpus',cpu,'--memory',memory,'--memory-swap',memory,container],check=True,stdout=subprocess.DEVNULL)
(output/'environment.json').write_text(json.dumps(environment,indent=2))
def call(path, method='GET'):
    req=urllib.request.Request(base+path,headers={'Authorization':authorization},method=method)
    with urllib.request.urlopen(req,timeout=30) as r:
        body=r.read();return body, len(body)
def snapshot(path, name):
    body,_=call(path);(output/name).write_bytes(body)
def read_stats(report):
    # Gatling 3.14 writes a binary simulation.log. Preserve it; read the generated request report.
    html=next(report.glob('req_list-*.html')).read_text()
    table={}
    for row in re.findall(r'<tr>(.*?)</tr>',html,re.S):
        cells=re.findall(r'<td[^>]*>(.*?)</td>',row,re.S)
        if len(cells)==4:
            clean=[re.sub('<[^>]+>','',c).strip() for c in cells]
            table[clean[0]]=clean[1:]
    chart=html.split("renderTo: 'responsesContainerId'",1)[1]
    points=json.loads(re.search(r"name: 'OK',\s*data: (\[\[.*?\]\]),",chart,re.S).group(1))
    nonzero=[x for x,y in points if y>0]
    seconds=(max(nonzero)-min(nonzero))/1000+1 if nonzero else 1
    successes=int(table['Total count'][1]);total=int(table['Total count'][0])
    return {'requests':total,'successes':successes,'errors':total-successes,
            'p50ms':int(table['50th percentile'][1]) if successes else None,'p95ms':int(table['95th percentile'][1]) if successes else None,
            'p99ms':int(table['99th percentile'][1]) if successes else None,'successRpsBucketed':round(successes/seconds,3),
            'gatlingWholeRunSuccessRps':float(table['Mean count/s'][1])}
summary=[]
try:
    for stage in args.stages:
        if stage=='indexed': (output/'indexes.json').write_bytes(call('/benchmark/index','POST')[0])
        if stage=='analyzed':
            with (output/'analyze-table.txt').open('w') as log:
                subprocess.run(['docker','exec',props['mysqlContainer'],'mysql','-utest','-ptest','test','-e','ANALYZE TABLE post'],stdout=log,stderr=subprocess.STDOUT,check=True)
        snapshot('/benchmark/plan',stage+'-plan.json')
        try:
            snapshot('/benchmark/key-plan',stage+'-key-plan.json')
            snapshot('/benchmark/fetch-plan',stage+'-fetch-plan.json')
        except urllib.error.HTTPError as e:
            if e.code != 404: raise
        # Record raw JFR plus HTTP/SQL/JVM snapshots; keep all files local and synthetic.
        jcmd=pathlib.Path(os.environ['JAVA_HOME'])/'bin/jcmd'
        subprocess.run([str(jcmd),props['pid'],'JFR.stop','name='+stage],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL,check=False)
        with (output/(stage+'-jfr-command.txt')).open('w') as log:
            subprocess.run([str(jcmd),props['pid'],'JFR.start','name='+stage,'settings=profile',
                            'filename="'+str(output/(stage+'.jfr'))+'"'],stdout=log,stderr=subprocess.STDOUT,check=False)
        for repetition in range(1,args.repetitions+1):
            name=f'{stage}-{repetition}'
            snapshot('/benchmark/metrics',name+'-before.json')
            report_dir=ROOT/'benchmarks/gatling/build/reports/gatling'
            before=set(report_dir.glob('*')) if report_dir.exists() else set()
            env=dict(os.environ,FITAPET_RUNTIME=str(runtime_file),FITAPET_STAGE=stage,
                     FITAPET_RPS=str(args.rps),FITAPET_SECONDS=str(args.seconds))
            with (output/(name+'.txt')).open('w') as log:
                process=subprocess.run(['sh',str(ROOT/'gradlew'),'-p',str(ROOT/'benchmarks/gatling'),
                                        'gatlingRun','--simulation','PostListSimulation','--console','plain','--max-workers','1'],
                                       cwd=ROOT,env=env,stdout=log,stderr=subprocess.STDOUT)
            snapshot('/benchmark/metrics',name+'-after.json')
            new=set(report_dir.glob('*'))-before
            reports=[p for p in new if (p/'simulation.log').exists()]
            if not reports:raise RuntimeError('No Gatling raw report: '+name)
            report=max(reports,key=lambda p:p.stat().st_mtime)
            shutil.copytree(report,output/name)
            stats=read_stats(report)
            _,response_bytes=call('/benchmark/baseline' if stage=='baseline' else '/posts?categoryType=FREE&size=20')
            row={'stage':stage,'repetition':repetition,**stats,'responseBytes':response_bytes,'gatlingExit':process.returncode}
            summary.append(row);(output/'summary.json').write_text(json.dumps(summary,indent=2))
            print(json.dumps(row),flush=True)
        with (output/(stage+'-jfr-stop.txt')).open('w') as log:
            subprocess.run([str(jcmd),props['pid'],'JFR.stop','name='+stage],stdout=log,stderr=subprocess.STDOUT,check=False)
finally:
    if 'jcmd' in locals() and 'stage' in locals():
        subprocess.run([str(jcmd),props['pid'],'JFR.stop','name='+stage],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL,check=False)
    # End only the fixture created for this experiment. Its containers are reaped by Testcontainers.
    if not args.keep_server: (ROOT/'build/benchmark/stop').touch()
print('Results: '+str(output),flush=True)
