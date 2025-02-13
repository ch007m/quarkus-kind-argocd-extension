package dev.swowdrop.argocd.extension.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;

@QuarkusTest
public class ArgocdExtensionDevModeTest {

    private final String HelloMessage = "<pre>\n" +
        "Hello World\n" +
        "\n" +
        "\n" +
        "                                       ##         .\n" +
        "                                 ## ## ##        ==\n" +
        "                              ## ## ## ## ##    ===\n" +
        "                           /\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\\___/ ===\n" +
        "                      ~~~ {~~ ~~~~ ~~~ ~~~~ ~~ ~ /  ===- ~~~\n" +
        "                           \\______ o          _,/\n" +
        "                            \\      \\       _,'\n" +
        "                             `'--.._\\..--''\n" +
        "</pre>\n";

    private static String HTTP_PORT;

    @BeforeAll
    public static void setup() {
        HTTP_PORT = ConfigProvider.getConfig().getValue("quarkus.argocd.devservices.http-port", String.class);
    }

    @Test
    public void writeYourOwnDevModeTest() {
        // Write your dev mode tests here - see the testing extension guide https://quarkus.io/guides/writing-extensions#testing-hot-reload for more information
        Assertions.assertTrue(true, "Add dev mode assertions to " + getClass().getName());
    }

    @Test
    public void testHelloWorld() {
        RestAssured
            .given()
              .port(Integer.parseInt(HTTP_PORT))
            .when()
              .get("/")
            .then()
              .statusCode(200)
              .body(is(HelloMessage));
    }
}
