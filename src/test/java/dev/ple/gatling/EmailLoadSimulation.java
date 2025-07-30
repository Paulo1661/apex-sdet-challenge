package dev.ple.gatling;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.HttpProtocolBuilder;

public class EmailLoadSimulation extends Simulation {
    WireMockServer wireMockServer = new WireMockServer(8089);

    public EmailLoadSimulation() {
        wireMockServer.start();
        configureFor("localhost", 8089);
        // ✅ Valid Request → 202
        stubFor(post(urlEqualTo("/send-email"))
                .withRequestBody(matchingJsonPath("$.to", matching(".+@.+\\..+")))
                .withRequestBody(matchingJsonPath("$.subject"))
                .withRequestBody(matchingJsonPath("$.body"))
                .willReturn(aResponse().withStatus(202)));
    }

    HttpProtocolBuilder httpProtocol = http
        .baseUrl("http://localhost:8089")
        .header("Content-Type", "application/json");

    ScenarioBuilder scn = scenario("Email Load Test")
        .repeat(1).on(
            exec(http("Send Email")
                .post("/send-email")
                .body(StringBody("{ \"to\": \"user@example.com\", \"subject\": \"Load Test\", \"body\": \"This is a performance test\" }"))
                .check(status().is(202))
            )
        );

    {
        setUp(scn.injectOpen(atOnceUsers(100))).protocols(httpProtocol);
    }

    @Override
    public void after() {
        super.before();
        System.out.println("Stopping wireMockServer");
        wireMockServer.stop();
    }
}
