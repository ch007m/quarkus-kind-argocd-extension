package dev.swowdrop.argocd.extension.test;

import io.restassured.RestAssured;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

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

    // Start hot reload (DevMode) test with your extension loaded
    @RegisterExtension
    static final QuarkusDevModeTest devModeTest = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Test
    public void writeYourOwnDevModeTest() {
        // Write your dev mode tests here - see the testing extension guide https://quarkus.io/guides/writing-extensions#testing-hot-reload for more information
        Assertions.assertTrue(true, "Add dev mode assertions to " + getClass().getName());
    }

    @Test
    public void testHelloWorld() {
        RestAssured
            .given()
              .port(34851)
            .when()
              .get("/")
            .then()
              .statusCode(200)
              .body(is(HelloMessage));
    }
}
