package dev.swowdrop.argocd.extension.it;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.swowdrop.argocd.extension.deployment.ArgocdModel;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.*;
import io.quarkiverse.argocd.v1alpha1.Application;
import io.quarkus.test.junit.QuarkusTest;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;

import static dev.swowdrop.argocd.extension.it.ArgocdResourceGenerator.populateApplication;


@QuarkusTest
public class ArgocdExtensionDevModeTest extends BaseHTTP {

    private static final Logger LOG = Logger.getLogger(ArgocdExtensionDevModeTest.class);
    private static KubernetesClient client;
    private static String ARGOCD_TOKEN;
    private static String ARGOCD_API;
    private static String ARGOCD_NAMESPACE = ConfigProvider.getConfig().getValue("quarkus.argocd.devservices.controller-namespace",String.class);

    // TODO: To be reviewed
    @BeforeAll
    public static void getToken() throws IOException, InterruptedException, NoSuchAlgorithmException, KeyManagementException {
        client = new KubernetesClientBuilder()
            .withConfig(Config.fromKubeconfig(ConfigProvider.getConfig().getValue("quarkus.argocd.devservices.kube-config", String.class)))
            .build();

        // Get the Argocd Server container port and forward it
        var argocd_pod = client.resources(Pod.class)
            .inNamespace(ARGOCD_NAMESPACE)
            .withLabel("app.kubernetes.io/name","argocd-server")
            .list().getItems().get(0);

        int containerPort = argocd_pod
              .getSpec()
              .getContainers().get(0).getPorts().get(0).getContainerPort();

        InetAddress inetAddress = InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 });
        LocalPortForward portForward = client.pods().resource(argocd_pod)
            .portForward(containerPort, inetAddress, 8080);
        LOG.infof("Container port: %d forwarded at https://127.0.0.1:%d", containerPort, portForward.getLocalPort());

        // The protocol to be used should be HTTPS to access the Argocd Server
        ARGOCD_API = String.format("https://127.0.0.1:%d/api/v1/session", portForward.getLocalPort());
        var admin_password = ConfigProvider.getConfig().getValue("quarkus.argocd.devservices.admin-password", String.class);

        // Populate the Body message
        ArgocdModel model = new ArgocdModel();
        model.setUsername("admin");
        model.setPassword(admin_password);

        ObjectMapper mapper = new ObjectMapper();
        var argocdRequestBody = mapper.writeValueAsString(model);
        LOG.info("Argocd request body: " + argocdRequestBody);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(ARGOCD_API))
            .POST(HttpRequest.BodyPublishers.ofString(argocdRequestBody))
            .header("Content-Type","application/json")
            .build();
        LOG.info("Posting HTTP request: " + request);

        // Get an Argocd Token for the tests
        HttpResponse<String> response = getHttpClient()
            .send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, response.statusCode());
        ARGOCD_TOKEN = response.body();
    }

    @Test
    public void writeYourOwnDevModeTest() {
        // Write your dev mode tests here - see the testing extension guide https://quarkus.io/guides/writing-extensions#testing-hot-reload for more information
        Assertions.assertTrue(true, "Add dev mode assertions to " + getClass().getName());
    }

    @Test
    public void testArgocdApplication() throws NoSuchAlgorithmException, KeyManagementException, JsonProcessingException {
        ArgoConfigurator argoConfigurator = new ArgoConfigurator();
        argoConfigurator.setDestinationNamespace("argocd");
        argoConfigurator.setApplicationName("test-1");
        argoConfigurator.setApplicationNamespace("argocd");
        argoConfigurator.setGitRevision("master");

        LOG.info(">>> Running the test case - 1");

        client.resource(populateApplication(argoConfigurator))
            .inNamespace(ARGOCD_NAMESPACE)
            .create();

        LOG.info("Checking when Argocd Application will be Healthy");
        try {
            client.resources(Application.class)
                .inNamespace(ARGOCD_NAMESPACE)
                .withName(argoConfigurator.getApplicationName())
                .waitUntilCondition(a ->
                    a != null &&
                        a.getStatus() != null &&
                        a.getStatus().getHealth() != null &&
                        a.getStatus().getHealth().getStatus() != null &&
                        a.getStatus().getHealth().getStatus().equals("Healthy"), 3600, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOG.error(client.getKubernetesSerialization().asYaml(client.genericKubernetesResources("argoproj.io/v1alpha1", "Application").inNamespace(ARGOCD_NAMESPACE).withName(argoConfigurator.getApplicationName()).get()));
        }
        LOG.infof("Argocd Application: {} healthy", argoConfigurator.getApplicationName());

        LOG.info("Checking now when Argocd Application will be synced");
        try {
            client.resources(Application.class)
                .inNamespace(ARGOCD_NAMESPACE)
                .withName(argoConfigurator.getApplicationName())
                .waitUntilCondition(a ->
                    a != null &&
                        a.getStatus() != null &&
                        a.getStatus().getSync() != null &&
                        a.getStatus().getSync().getStatus() != null &&
                        a.getStatus().getSync().getStatus().equals("Synced"), 3600, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOG.error(client.getKubernetesSerialization().asYaml(client.genericKubernetesResources("argoproj.io/v1alpha1", "Application").inNamespace(ARGOCD_NAMESPACE).withName(argoConfigurator.getApplicationName()).get()));
        }
        LOG.infof("Argocd Application: {} synced", argoConfigurator.getApplicationName());

        Application app = client.resources(Application.class)
            .inNamespace(ARGOCD_NAMESPACE)
            .withName(argoConfigurator.getApplicationName()).get();
        LOG.warn(client.getKubernetesSerialization().asYaml(app));
    }
}
