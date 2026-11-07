package hongik21.fit_a_pet;

import io.awspring.cloud.s3.S3Template;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationSupport {
    // Shared for the JVM lifetime; Ryuk removes containers when the test JVM exits.
    protected static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.6");
    protected static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4.6-alpine"))
            .withExposedPorts(6379);
    static { MYSQL.start(); REDIS.start(); }
    @MockitoBean S3Template s3Template;
    @DynamicPropertySource static void services(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", MYSQL::getJdbcUrl);
        r.add("spring.datasource.username", MYSQL::getUsername);
        r.add("spring.datasource.password", MYSQL::getPassword);
        r.add("spring.data.redis.host", REDIS::getHost);
        r.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }
}
