package hongik21.fit_a_pet.benchmark;

import hongik21.fit_a_pet.IntegrationSupport;
import hongik21.fit_a_pet.accounts.entity.*;
import hongik21.fit_a_pet.accounts.repository.MemberRepository;
import hongik21.fit_a_pet.accounts.service.TokenService;
import hongik21.fit_a_pet.posts.domain.*;
import hongik21.fit_a_pet.posts.dto.PostListResponse;
import hongik21.fit_a_pet.posts.repository.PostRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.lang.management.ManagementFactory;

@Tag("benchmark")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties={"server.address=127.0.0.1","jwt.access-token-expiration=3600000","jwt.refresh-token-expiration=3600000"})
@Import(BenchmarkServerTest.Fixture.class)
class BenchmarkServerTest extends IntegrationSupport {
    @LocalServerPort int port;
    @Autowired JdbcTemplate sql;
    @Autowired MemberRepository members;
    @Autowired TokenService tokens;
    @Autowired BCryptPasswordEncoder passwords;
    @Test void serveDisposableFixture() throws Exception {
        int rows=Integer.getInteger("benchmark.rows",10000);
        if(rows<100 || rows>1000000)throw new IllegalArgumentException("rows must be 100..1000000");
        var member=members.save(Member.builder().name("benchmark").nickname("benchmark").email("benchmark@test.com")
                .password(passwords.encode("synthetic-benchmark-password")).memberType(MemberType.GENERAL)
                .createdAt(LocalDateTime.of(2026,11,1,0,0)).build());
        Random random=new Random(20261107);
        var batch=new ArrayList<Object[]>();
        for(int i=0;i<rows;i++)batch.add(new Object[]{new String[]{"FREE","INFO","QUESTION"}[random.nextInt(3)],
                "synthetic-post-"+i,"x".repeat(128),LocalDateTime.of(2026,11,1,0,0).plusSeconds(i),member.getId()});
        sql.batchUpdate("insert into post(post_category,post_title,post_content,post_date,member_id) values(?,?,?,?,?)",batch);
        sql.execute("drop index idx_post_category_cursor on post");sql.execute("drop index idx_post_cursor on post");
        var pair=tokens.login(member);
        Path directory=Path.of("build/benchmark");Files.createDirectories(directory);Files.deleteIfExists(directory.resolve("stop"));
        Properties runtime=new Properties();runtime.setProperty("baseUrl","http://127.0.0.1:"+port);
        runtime.setProperty("accessToken",pair.accessToken());runtime.setProperty("rows",Integer.toString(rows));
        runtime.setProperty("pid",Long.toString(ProcessHandle.current().pid()));
        runtime.setProperty("mysqlContainer",MYSQL.getContainerId());runtime.setProperty("redisContainer",REDIS.getContainerId());
        try(var out=Files.newOutputStream(directory.resolve("runtime.properties"))){runtime.store(out,"Synthetic disposable fixture only");}
        long deadline=System.nanoTime()+Duration.ofSeconds(Integer.getInteger("benchmark.timeoutSeconds",1200)).toNanos();
        while(!Files.exists(directory.resolve("stop")) && System.nanoTime()<deadline)Thread.sleep(500);
        Files.deleteIfExists(directory.resolve("runtime.properties"));
    }
    @RestController
    @RequestMapping("/benchmark")
    public static class Fixture {
        @Autowired PostRepository posts;
        @Autowired JdbcTemplate sql;
        @Autowired EntityManagerFactory emf;
        @GetMapping("/baseline") @Transactional(readOnly=true)
        public List<PostListResponse> baseline(){
            // Exact original behavior: fetch every post + author then filter in Java, no pagination.
            return posts.findAllWithMembers().stream().filter(p->p instanceof FreePost)
                    .map(p->PostListResponse.builder().postId(p.getPostId()).postTitle(p.getPostTitle())
                            .postCategory(PostCategoryType.FREE).nickname(p.getMember().getNickname()).postDate(p.getPostDate()).build()).toList();
        }
        @PostMapping("/index") public Map<String,Object> index(){
            sql.execute("create index idx_post_category_cursor on post(post_category,post_date,post_id)");
            sql.execute("create index idx_post_cursor on post(post_date,post_id)");
            return Map.of("indexes",sql.queryForList("show index from post"));
        }
        @GetMapping("/plan") public List<Map<String,Object>> plan(){
            return sql.queryForList("explain analyze select p.post_id,p.post_title,p.post_date,m.nickname from post p join member m on m.id=p.member_id where p.post_category='FREE' order by p.post_date desc,p.post_id desc limit 21");
        }
        @GetMapping("/key-plan") public List<Map<String,Object>> keyPlan(){
            return sql.queryForList("explain analyze select p.post_id,p.post_date from post p where p.post_category='FREE' order by p.post_date desc,p.post_id desc limit 21");
        }
        @GetMapping("/fetch-plan") public List<Map<String,Object>> fetchPlan(){
            var ids=sql.queryForList("select post_id from post where post_category='FREE' order by post_date desc,post_id desc limit 20",Long.class);
            String literals=ids.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
            return sql.queryForList("explain analyze select p.*,m.* from post p join member m on m.id=p.member_id where p.post_id in ("+literals+")");
        }
        @GetMapping("/metrics") public Map<String,Object> metrics(){
            var stats=emf.unwrap(SessionFactory.class).getStatistics();
            var gc=ManagementFactory.getGarbageCollectorMXBeans().stream().map(g->Map.of("name",g.getName(),"count",g.getCollectionCount(),"ms",g.getCollectionTime())).toList();
            var os=(com.sun.management.OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
            return Map.of("heapUsed",ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed(),
                    "processCpuNanos",os.getProcessCpuTime(),"sqlStatements",stats.getPrepareStatementCount(),"gc",gc);
        }
    }
}
