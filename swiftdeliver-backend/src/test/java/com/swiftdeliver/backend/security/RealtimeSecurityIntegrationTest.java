package com.swiftdeliver.backend.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The live-tracking feed over STOMP/WebSocket. Before: the HTTP layer refused the browser's
 * {@code ?token=} handshake (so it never connected in the real app), and had it connected,
 * anonymous sockets could have followed every order and every driver.
 *
 * Demo data: customers amina / karim, driver hamza delivers karim's order, admin / admin123!.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:swiftdeliver_ws;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "jwt.secret=test-only-secret-0123456789abcdef0123456789abcdef",
                "spring.profiles.active=test"
        })
class RealtimeSecurityIntegrationTest {

    private static final String DEMO_PASSWORD = "SwiftDeliver@2026!";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @Autowired
    Environment environment;

    // ------------------------------------------------------------------ http helpers

    private int port() {
        return Integer.parseInt(environment.getProperty("local.server.port"));
    }

    private JsonNode http(String method, String path, String token, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port() + "/api" + path));
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertTrue(response.statusCode() < 300, method + " " + path + " -> " + response.statusCode() + " " + response.body());
        return response.body().isBlank() ? null : JSON.readTree(response.body());
    }

    private String login(String username, String password) throws Exception {
        return http("POST", "/auth/login", null,
                "{\"usernameOrEmail\":\"" + username + "\",\"password\":\"" + password + "\"}").path("accessToken").asText();
    }

    private String demo(String username) throws Exception {
        return login(username, DEMO_PASSWORD);
    }

    private long userId(String username) throws Exception {
        JsonNode page = http("GET", "/customer-users", login("admin", "admin123!"), null);
        for (JsonNode user : page.has("content") ? page.get("content") : page) {
            if (username.equals(user.path("username").asText())) {
                return user.path("id").asLong();
            }
        }
        throw new AssertionError("no customer " + username);
    }

    /** Karim's demo order that driver hamza is delivering. */
    private JsonNode karimsOrderWithHamza() throws Exception {
        JsonNode page = http("GET", "/orders?size=100", login("admin", "admin123!"), null);
        for (JsonNode order : page.has("content") ? page.get("content") : page) {
            if ("customer.karim".equals(order.path("customerUser").path("username").asText())
                    && "driver.hamza".equals(order.path("driverPerson").path("username").asText())) {
                return order;
            }
        }
        throw new AssertionError("demo order not found");
    }

    // ------------------------------------------------------------------ stomp helpers

    private static final class Recorder extends StompSessionHandlerAdapter {
        final BlockingQueue<String> errors = new LinkedBlockingQueue<>();
        final BlockingQueue<String> messages = new LinkedBlockingQueue<>();

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            errors.add(String.valueOf(headers.getFirst("message")));
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            errors.add("transport: " + exception.getMessage());
        }
    }

    private StompSession connect(String token, Recorder recorder) throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new MappingJackson2MessageConverter()); // the server publishes JSON
        String url = "ws://localhost:" + port() + "/ws/websocket" + (token == null ? "" : "?token=" + token);
        return client.connectAsync(url, new WebSocketHttpHeaders(), new StompHeaders(), recorder).get(8, TimeUnit.SECONDS);
    }

    private void subscribe(StompSession session, String destination, Recorder recorder) {
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return JsonNode.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                recorder.messages.add(String.valueOf(payload));
            }
        });
    }

    /** True when the server answers a SUBSCRIBE with an ERROR frame (or drops the session). */
    private boolean isRefused(String token, String destination) throws Exception {
        Recorder recorder = new Recorder();
        StompSession session = connect(token, recorder);
        subscribe(session, destination, recorder);
        return recorder.errors.poll(2, TimeUnit.SECONDS) != null;
    }

    // ------------------------------------------------------------------ tests

    // Regression: the HTTP layer demanded an Authorization header on /ws, which a browser cannot send.
    @Test
    void aSocketWithAValidTokenConnects() throws Exception {
        Recorder recorder = new Recorder();
        StompSession session = connect(demo("customer.amina"), recorder);
        assertTrue(session.isConnected());
    }

    @Test
    void anonymousSocketsAreRefused() {
        assertThrows(ExecutionException.class, () -> connect(null, new Recorder()));
        assertThrows(ExecutionException.class, () -> connect("not-a-jwt", new Recorder()));
    }

    @Test
    void aUserCanOnlyFollowTheirOwnPrivateTopic() throws Exception {
        String amina = demo("customer.amina");
        assertFalse(isRefused(amina, "/topic/users/" + userId("customer.amina")), "her own topic");
        assertTrue(isRefused(amina, "/topic/users/" + userId("customer.karim")), "someone else's topic");
    }

    // Regression: the global feeds carry every order and every driver's position.
    @Test
    void theGlobalOrderFeedsAreForAdministratorsOnly() throws Exception {
        String amina = demo("customer.amina");
        assertTrue(isRefused(amina, "/topic/orders"));
        assertTrue(isRefused(amina, "/topic/orders/location"));
        String admin = login("admin", "admin123!");
        assertFalse(isRefused(admin, "/topic/orders"));
        assertFalse(isRefused(admin, "/topic/orders/location"));
    }

    @Test
    void anOrdersFeedIsOnlyForPeopleWhoMayReadTheOrder() throws Exception {
        long karimsOrder = karimsOrderWithHamza().path("id").asLong();
        assertFalse(isRefused(demo("customer.karim"), "/topic/orders/" + karimsOrder), "the customer");
        assertFalse(isRefused(demo("driver.hamza"), "/topic/orders/" + karimsOrder), "the assigned driver");
        assertTrue(isRefused(demo("customer.amina"), "/topic/orders/" + karimsOrder), "a stranger");
        assertTrue(isRefused(demo("customer.amina"), "/topic/anything-else"), "unknown destinations");
    }

    @Test
    void theDriversLocationReachesTheCustomerPrivatelyAndNotAStranger() throws Exception {
        JsonNode order = karimsOrderWithHamza();
        long orderId = order.path("id").asLong();

        Recorder karim = new Recorder();
        subscribe(connect(demo("customer.karim"), karim), "/topic/users/" + userId("customer.karim"), karim);
        Recorder amina = new Recorder();
        subscribe(connect(demo("customer.amina"), amina), "/topic/users/" + userId("customer.amina"), amina);
        Thread.sleep(500); // let the subscriptions register

        http("POST", "/tracking/orders/" + orderId + "/location", demo("driver.hamza"),
                "{\"orderNumber\":\"%s\",\"latitude\":33.57,\"longitude\":-7.58,\"speedKmh\":42}"
                        .formatted(order.path("orderNumber").asText()));

        String received = karim.messages.poll(5, TimeUnit.SECONDS);
        assertNotNull(received, "the customer receives the live location");
        JsonNode event = JSON.readTree(received);
        assertEquals("DRIVER_LOCATION_UPDATE", event.path("type").asText());
        assertEquals(33.57, event.path("latitude").asDouble(), 0.0001);
        assertEquals(null, amina.messages.poll(1, TimeUnit.SECONDS), "a stranger receives nothing");
    }
}
