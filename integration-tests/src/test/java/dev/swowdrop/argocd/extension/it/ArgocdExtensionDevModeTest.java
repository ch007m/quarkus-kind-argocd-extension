package dev.swowdrop.argocd.extension.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.swowdrop.argocd.extension.deployment.ArgocdModel;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.LocalPortForward;
import io.quarkus.test.junit.QuarkusTest;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.jboss.logging.Logger;


@QuarkusTest
public class ArgocdExtensionDevModeTest {

    private static final Logger LOG = Logger.getLogger(ArgocdExtensionDevModeTest.class);
    private static KubernetesClient client;

    @BeforeAll
    public static void getToken() throws IOException, InterruptedException {
        client = new KubernetesClientBuilder()
            .withConfig(ConfigProvider.getConfig().getValue("quarkus.argocd.devservices.kube-config", String.class))
            .build();

        var ARGOCD_NAMESPACE = ConfigProvider.getConfig().getValue("quarkus.argocd.devservices.controller-namespace",String.class);

        // Get the Argocd Server container port and forward it
        int containerPort = client.resources(Pod.class).inNamespace(ARGOCD_NAMESPACE).withName("argocd-server")
            .get().getSpec().getContainers().get(0).getPorts().get(0).getContainerPort();

        InetAddress inetAddress = InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 });
        LocalPortForward portForward = client.pods().inNamespace(ARGOCD_NAMESPACE).withName("argocd-server").portForward(containerPort,
            inetAddress, 8080);
        LOG.infof("Port forwarded at http://127.0.0.1: %s", portForward.getLocalPort());

        var argocd_url = String.format("http://127.0.0.1: %s", portForward.getLocalPort());
        var admin_password = ConfigProvider.getConfig().getValue("quarkus.argocd.devservices.admin-password", String.class);

        ArgocdModel model = new ArgocdModel();
        model.setUsername("admin");
        model.setPassword(admin_password);

        // Get an Argocd Token
        ObjectMapper mapper = new ObjectMapper();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(argocd_url))
            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(model)))
            .header("Content-Type","application/json")
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, response.statusCode());
        LOG.infof("Token : %s",response.body());
    }

    @Test
    public void writeYourOwnDevModeTest() {
        // Write your dev mode tests here - see the testing extension guide https://quarkus.io/guides/writing-extensions#testing-hot-reload for more information
        Assertions.assertTrue(true, "Add dev mode assertions to " + getClass().getName());
    }

    @Test
    public void testArgocdApplication() {
        // TODO
    }
}
