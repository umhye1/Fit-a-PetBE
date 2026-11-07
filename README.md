# Fit a Pet Backend

반려동물·산책 기록·커뮤니티 API를 제공하는 Spring Boot 백엔드다. Java 21, MySQL, Redis를 사용한다.

후속 개선에서 인증 세션과 데이터 접근 처리를 정리하고 게시글 커서 페이징을 적용했다. 목록은 정렬된 키를 먼저 선택한 뒤 최대 20건을 작성자와 함께 조회한다.

- [검증 결과 요약](docs/public-validation.md)
- [조회 성능 측정값](docs/performance-summary.json)
- [Gatling 실험 실행법](benchmarks/README.md)

Java 21과 실행 중인 Docker에서 테스트한다. Testcontainers가 임시 MySQL·Redis를 만들며 테스트 중 외부 SMTP·S3를 사용하지 않는다.

```sh
sh ./gradlew test --no-daemon
sh ./gradlew bootJar --no-daemon
```

macOS에서 기본 Java가 17이라면 실행 전에 `export JAVA_HOME=$(/usr/libexec/java_home -v 21)`로 설정한다. 운영용 DB·Redis·SMTP·S3·JWT 설정은 별도로 제공하며 테스트 자격증명을 사용하지 않는다.

토큰 정책 변경에 따라 적용 시 재로그인이 필요하다. 기존 DB의 인덱스 적용과 프론트엔드 응답 계약 반영은 별도 배포 절차에서 확인한다. 운영 배포는 이 작업에서 수행하지 않았다.
