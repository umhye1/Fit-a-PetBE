import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import java.nio.file.*;
import java.util.Properties;
import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class PostListSimulation extends Simulation {
    public PostListSimulation() throws Exception {
        Properties runtime = new Properties();
        try(var in=Files.newInputStream(Path.of(System.getenv("FITAPET_RUNTIME")))) { runtime.load(in); }
        String stage=System.getenv().getOrDefault("FITAPET_STAGE","paged");
        double rps=Double.parseDouble(System.getenv().getOrDefault("FITAPET_RPS","10"));
        int seconds=Integer.parseInt(System.getenv().getOrDefault("FITAPET_SECONDS","10"));
        String path=stage.equals("baseline") ? "/benchmark/baseline" : "/posts?categoryType=FREE&size=20";
        var protocol=http.baseUrl(runtime.getProperty("baseUrl"))
                .authorizationHeader("Bearer "+runtime.getProperty("accessToken"));
        var warmup=scenario("warmup").exec(http("warmup").get(path).check(status().is(200)));
        HttpRequestActionBuilder request=http("list").get(path).check(status().is(200));
        if(stage.equals("baseline")) request=request.check(jsonPath("$[0].postCategory").is("FREE"));
        else request=request.check(jsonPath("$.data.items[*]").count().lte(20),jsonPath("$.data.items[0].postCategory").is("FREE"));
        var measured=scenario("measured").exec(request);
        setUp(warmup.injectOpen(constantUsersPerSec(5).during(5))
                .andThen(measured.injectOpen(constantUsersPerSec(rps).during(seconds))))
                .protocols(protocol).maxDuration(java.time.Duration.ofSeconds(seconds + 15)).assertions(details("list").failedRequests().count().is(0L),
                        details("list").responseTime().percentile(95).lt(1000));
    }
}
