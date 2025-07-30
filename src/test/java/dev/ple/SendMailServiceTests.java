package dev.ple;

import com.github.tomakehurst.wiremock.WireMockServer;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.ensure.Ensure;
import net.serenitybdd.screenplay.rest.abilities.CallAnApi;
import net.serenitybdd.screenplay.rest.interactions.Post;
import org.junit.jupiter.api.*;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static net.serenitybdd.rest.SerenityRest.lastResponse;

@ExtendWith(SerenityJUnit5Extension.class)
public class SendMailServiceTests {
    static WireMockServer wireMockServer = new WireMockServer(8089);
    private final String BASE_URL = "http://localhost:8089";
    Actor actor = Actor.named("Rachel").whoCan(CallAnApi.at(BASE_URL));

    private static final String VALID_EMAIL = """
                {
                    "to": "user@example.com",
                    "subject": "Welcome!",
                    "body": "Thanks for signing up."
                }
            """;

    private static final String RETRY = """
                {
                    "to": "user@example.com",
                    "subject": "Third Attempt",
                    "body": "Thanks for signing up."
                }
            """;

    private static final String MISSING_TO = """
                {
                    "subject": "No Recipient",
                    "body": "Missing 'to' field."
                }
            """;

    private static final String INVALID_EMAIL = """
                {
                    "to": "invalid@email",
                    "subject": "Invalid Email",
                    "body": "Test body"
                }
            """;


    @BeforeAll
    static void startServer(TestInfo testInfo) {
        wireMockServer.start();
        configureFor("localhost", 8089);
    }

    @BeforeEach
    void defineStub(TestInfo testInfo) {
        if (testInfo.getTags().contains("Retry")) {
            // Simulate retry
            stubFor(post(urlEqualTo("/send-email"))
                    .inScenario("Retry Twice Then Succeed")
                    .whenScenarioStateIs(STARTED)
                    .withRequestBody(matchingJsonPath("$.to", matching(".+@.+\\..+")))
                    .withRequestBody(matchingJsonPath("$.subject"))
                    .withRequestBody(matchingJsonPath("$.body"))
                    .willReturn(aResponse()
                            .withStatus(503)
                            .withBody("Queue is full"))
                    .willSetStateTo("Second Attempt"));

            stubFor(post(urlEqualTo("/send-email"))
                    .inScenario("Retry Twice Then Succeed")
                    .whenScenarioStateIs("Second Attempt")
                    .withRequestBody(matchingJsonPath("$.to", matching(".+@.+\\..+")))
                    .withRequestBody(matchingJsonPath("$.subject"))
                    .withRequestBody(matchingJsonPath("$.body"))
                    .willReturn(aResponse()
                            .withStatus(503)
                            .withBody("Queue is full"))
                    .willSetStateTo("Third Attempt"));

            stubFor(post(urlEqualTo("/send-email"))
                    .inScenario("Retry Twice Then Succeed")
                    .whenScenarioStateIs("Third Attempt")
                    .withRequestBody(matchingJsonPath("$.to", matching(".+@.+\\..+")))
                    .withRequestBody(matchingJsonPath("$.subject"))
                    .withRequestBody(matchingJsonPath("$.body"))
                    .willReturn(aResponse()
                            .withStatus(202)
                            .withBody("Accepted")));
        }
    }

    @AfterAll
    static void stopServer() {
        wireMockServer.stop();
    }

    @Test
    @Tag("ValidRequest")
    void validRequestShouldReturn202() {
        // ✅ Valid Request → 202
        stubFor(post(urlEqualTo("/send-email"))
                .withRequestBody(matchingJsonPath("$.to", matching(".+@.+\\..+")))
                .withRequestBody(matchingJsonPath("$.subject"))
                .withRequestBody(matchingJsonPath("$.body"))
                .willReturn(aResponse().withStatus(202)));

        actor.attemptsTo(Post.to("/send-email")
                .with(
                        req ->
                                req.header("Content-Type", "application/json")
                                        .body(VALID_EMAIL)
                )
        );

        actor.attemptsTo(Ensure.that(lastResponse().statusCode()).isEqualTo(202));
    }

    @Test
    void missingToFieldShouldReturn400() {
        // ❌ Missing "to" → 400
        stubFor(post(urlEqualTo("/send-email"))
                .withRequestBody(notMatching(".*\"to\".*"))
                .willReturn(aResponse().withStatus(422).withBody("Missing 'to' field")));

        actor.attemptsTo(Post.to("/send-email")
                .with(
                        req ->
                                req.header("Content-Type", "application/json")
                                        .body(MISSING_TO)
                )
        );
        actor.attemptsTo(Ensure.that(lastResponse().statusCode()).isEqualTo(422));
    }

    @Test
    void invalidEmailFormatShouldReturn400() {
        // ❌ Invalid email format → 400
        stubFor(post(urlEqualTo("/send-email"))
                .withRequestBody(matchingJsonPath("$.to", notMatching(".+@.+\\..+")))
                .willReturn(aResponse().withStatus(422).withBody("Invalid email format")));

        actor.attemptsTo(Post.to("/send-email")
                .with(
                        req ->
                                req.header("Content-Type", "application/json")
                                        .body(INVALID_EMAIL)
                )
        );

        actor.attemptsTo(Ensure.that(lastResponse().statusCode()).isEqualTo(422));
        actor.attemptsTo(Ensure.that(lastResponse().body().asString()).isEqualTo("Invalid email format"));
    }

    @Test
    @Tag("Retry")
    void shouldRetryAndSucceedWhenQueueIsInitiallyFull() throws InterruptedException {

        // Manually retry: 3
        int maxRetries = 3;
        int attempt = 0;
        boolean success = false;

        while (attempt < maxRetries && !success) {
            attempt++;
            actor.attemptsTo(
                    Post.to("/send-email")
                            .with(req -> req
                                    .header("Content-Type", "application/json")
                                    .body(RETRY))
            );

            int status = lastResponse().statusCode();
            if (status == 202) {
                success = true;
            } else {
                Thread.sleep(500); // optional delay between retries
            }
        }
        actor.attemptsTo(Ensure.that(lastResponse().statusCode()).isEqualTo(202));

        verify(3, postRequestedFor(urlEqualTo("/send-email")).withRequestBody(matchingJsonPath("$.subject", equalTo("Third Attempt"))));
    }
}
