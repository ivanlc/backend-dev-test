package com.ivanlc.backenddevtest.product.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.ivanlc.backenddevtest.product.domain.ProductDetail;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"app.upstream.read-timeout=200ms", "app.upstream.request-timeout=600ms"})
class SimilarProductsIntegrationTest {

    private static final HttpServer upstream = startUpstream();

    @Autowired
    private TestRestTemplate http;

    @DynamicPropertySource
    static void upstreamAddress(DynamicPropertyRegistry registry) {
        registry.add("app.upstream.base-url", () ->
                "http://localhost:" + upstream.getAddress().getPort());
    }

    @AfterAll
    static void stopUpstream() {
        upstream.stop(0);
    }

    @Test
    void returnsDetailsInSimilarityOrderOverRealHttp() {
        ResponseEntity<ProductDetail[]> response = http.getForEntity(
                "/product/normal/similar", ProductDetail[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).extracting(ProductDetail::id).containsExactly("3", "2");
    }

    @Test
    void returnsEmptyArrayForEmptyIds() {
        ResponseEntity<ProductDetail[]> response = http.getForEntity(
                "/product/empty/similar", ProductDetail[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void mapsMissingUpstreamProductTo404() {
        assertThat(http.getForEntity("/product/missing/similar", String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void mapsDetailFailureTo502() {
        assertThat(http.getForEntity("/product/failing/similar", String.class).getStatusCode())
                .isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    void mapsMissingRelatedDetailTo404WithoutPartialArray() {
        ResponseEntity<String> response = http.getForEntity(
                "/product/detailmissing/similar", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("\"status\":404");
        assertThat(response.getBody()).doesNotContain("Blazer");
    }

    @Test
    void mapsMalformedDetailTo502() {
        assertThat(http.getForEntity("/product/invalid/similar", String.class).getStatusCode())
                .isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    void mapsReadTimeoutTo504() {
        assertThat(http.getForEntity("/product/timeout/similar", String.class).getStatusCode())
                .isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
    }

    private static HttpServer startUpstream() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/product/", SimilarProductsIntegrationTest::respond);
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            return server;
        } catch (IOException error) {
            throw new IllegalStateException("Could not start test upstream", error);
        }
    }

    private static void respond(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        switch (path) {
            case "/product/normal/similarids" -> send(exchange, 200, "[3,2]");
            case "/product/empty/similarids" -> send(exchange, 200, "[]");
            case "/product/missing/similarids" -> send(exchange, 404, "{}");
            case "/product/failing/similarids" -> send(exchange, 200, "[6]");
            case "/product/invalid/similarids" -> send(exchange, 200, "[7]");
            case "/product/timeout/similarids" -> send(exchange, 200, "[8]");
            case "/product/detailmissing/similarids" -> send(exchange, 200, "[3,9]");
            case "/product/3" -> {
                pause(80);
                send(exchange, 200,
                        "{\"id\":\"3\",\"name\":\"Blazer\",\"price\":29.99,\"availability\":false}");
            }
            case "/product/2" -> send(exchange, 200,
                    "{\"id\":\"2\",\"name\":\"Dress\",\"price\":19.99,\"availability\":true}");
            case "/product/6" -> send(exchange, 500, "{}");
            case "/product/7" -> send(exchange, 200, "{\"id\":\"7\"}");
            case "/product/8" -> {
                pause(500);
                send(exchange, 200,
                        "{\"id\":\"8\",\"name\":\"Slow\",\"price\":1,\"availability\":true}");
            }
            case "/product/9" -> send(exchange, 404, "{}");
            default -> send(exchange, 404, "{}");
        }
    }

    private static void send(HttpExchange exchange, int status, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, body.length);
        try (var response = exchange.getResponseBody()) {
            response.write(body);
        }
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        }
    }
}
