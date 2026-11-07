# Fit a Pet 조회 성능 실험

Testcontainers MySQL·Redis와 127.0.0.1 HTTP 서버에 합성 게시글을 생성하는 로컬 전용 실험이다. 운영 서버를 대상으로 하지 않는다. 성능 비교용 컨트롤러와 진단 경로는 테스트 소스에만 있고 production jar에 포함되지 않는다.

## 실행

Java 21과 실행 중인 Docker가 필요하다. 터미널 1에서 임시 서버와 합성 데이터 1만 건을 준비한다.

```sh
sh ./gradlew benchmarkServer -PbenchmarkRows=10000 --no-daemon
```

터미널 2에서 같은 Java 환경으로 비교한다.

```sh
python3 benchmarks/run_experiment.py --rps 10 --seconds 20 --repetitions 3
```

데이터는 seed 20261107로 생성한다. 기본 비교 단계는 전체 조회, DB 필터·페이징, 인덱스 추가, 통계 갱신이다. 현재 기본 조회 구현은 키를 먼저 제한한 뒤 해당 ID를 작성자와 조회한다. 이전 단일 조회 방식의 수치와 최종 코드의 실험은 구분한다.

인덱스와 통계를 갖춘 최종 구현을 비교하려면 `--stages indexed analyzed`를 사용할 수 있다. 이미 인덱스를 만든 동일 픽스처에서 통계 갱신만 비교할 때는 `--stages analyzed`를 사용한다.

실행별 워밍업 5초는 warmup, 본 측정은 list 요청으로 분리한다. list 오류 0건, p95 1000ms 미만의 assertion을 사용하며 실패도 결과에 남긴다. 고정 유입률의 실험으로 최대 TPS를 주장하지 않는다.

## 결과와 정리

결과는 build/benchmark/results 아래에 생성한다. 환경, 소스 해시, 반복별 지연, 응답 크기, JVM·GC·SQL 카운터, 대표 실행 계획과 Gatling 보고서를 로컬에서 확인한다. 임시 runtime.properties에는 테스트용 인증 정보가 있으므로 출력하거나 Git에 추가하지 않는다.

기본 runner는 끝나면 자신이 만든 픽스처를 종료한다. 수동 중단했거나 `--keep-server`로 실행한 경우 아래 명령으로 정리한다.

```sh
touch build/benchmark/stop
```

공개용으로 정리한 수치와 한계는 [검증 요약](../docs/public-validation.md)과 [측정값 집계](../docs/performance-summary.json)에 있다. 생성된 상세 로컬 자료를 공개 문서와 혼동하지 않는다.
