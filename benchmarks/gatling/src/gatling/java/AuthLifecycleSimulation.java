import io.gatling.javaapi.core.*;
import java.nio.file.*;
import java.util.Properties;
import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class AuthLifecycleSimulation extends Simulation {
    public AuthLifecycleSimulation() throws Exception {
        Properties runtime=new Properties();
        try(var input=Files.newInputStream(Path.of(System.getenv("FITAPET_RUNTIME")))){runtime.load(input);}
        var journey=scenario("independent sessions")
                .exec(http("login").post("/api/users/login").body(StringBody("{\"email\":\"benchmark@test.com\",\"password\":\"synthetic-benchmark-password\"}"))
                        .asJson().check(status().is(200),jsonPath("$.data.refreshToken").saveAs("refresh")))
                .exitHereIfFailed()
                .exec(http("rotate").post("/api/auth/refresh").body(StringBody("{\"refreshToken\":\"#{refresh}\"}"))
                        .asJson().check(status().is(200),jsonPath("$.data.accessToken").saveAs("access")))
                .exitHereIfFailed()
                .exec(http("protected").get("/api/users/test").header("Authorization","Bearer #{access}").check(status().is(200)))
                .exec(http("logout").post("/api/users/logout").header("Authorization","Bearer #{access}").check(status().is(200)));
        setUp(journey.injectOpen(constantUsersPerSec(5).during(10)))
                .protocols(http.baseUrl(runtime.getProperty("baseUrl")))
                .maxDuration(java.time.Duration.ofSeconds(25))
                .assertions(global().failedRequests().count().is(0L),global().responseTime().percentile(95).lt(1000));
    }
}
