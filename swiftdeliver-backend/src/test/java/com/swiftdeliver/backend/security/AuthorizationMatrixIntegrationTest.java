package com.swiftdeliver.backend.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the real application (in-memory database, demo data seeded) and talks to it over HTTP with
 * real JWT logins, as the different roles would. Every test here is an attack that used to work.
 *
 * Demo data: customers amina / karim / leila, vendors sofia (companies 1 and 3) and youssef
 * (company 2), delivery owners nadia / omar, drivers ayoub / hamza / ..., password SwiftDeliver@2026!.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:swiftdeliver_it;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "jwt.secret=test-only-secret-0123456789abcdef0123456789abcdef",
                "spring.profiles.active=test"
        })
class AuthorizationMatrixIntegrationTest {

    private static final String DEMO_PASSWORD = "SwiftDeliver@2026!";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final Map<String, String> TOKENS = new HashMap<>();

    @Autowired
    Environment environment;

    private static String base;

    @BeforeAll
    static void clearTokens() {
        TOKENS.clear();
    }

    private String baseUrl() {
        if (base == null) {
            base = "http://localhost:" + environment.getProperty("local.server.port") + "/api";
        }
        return base;
    }

    // ------------------------------------------------------------------ helpers

    private record Reply(int status, String body) {
        JsonNode json() {
            try {
                return JSON.readTree(body);
            } catch (Exception e) {
                throw new AssertionError("Not JSON: " + body, e);
            }
        }
    }

    private Reply call(String method, String path, String token, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl() + path));
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(), response.body());
    }

    private String token(String username, String password) throws Exception {
        String cached = TOKENS.get(username);
        if (cached != null) {
            return cached;
        }
        Reply reply = call("POST", "/auth/login", null,
                "{\"usernameOrEmail\":\"" + username + "\",\"password\":\"" + password + "\"}");
        assertEquals(200, reply.status(), "login as " + username);
        String token = reply.json().path("accessToken").asText();
        TOKENS.put(username, token);
        return token;
    }

    private String demo(String username) throws Exception {
        return token(username, DEMO_PASSWORD);
    }

    private String admin() throws Exception {
        return token("admin", "admin123!");
    }

    /** A new open-bid order placed by the given customer; returns its id. */
    private long placeOrder(String customer) throws Exception {
        Reply reply = call("POST", "/orders", demo(customer), """
                {"orderType":"MARKETPLACE","routingMode":"OPEN_BID","pickupAddress":"Atlas Market",
                 "deliveryAddress":"1 Test Street","recipientName":"R","recipientPhone":"1",
                 "orderAmount":100,"deliveryFee":10,"vendorCompany":{"id":1}}""");
        assertEquals(201, reply.status(), reply.body());
        return reply.json().path("id").asLong();
    }

    private JsonNode orderAsAdmin(long id) throws Exception {
        Reply reply = call("GET", "/orders/" + id, admin(), null);
        assertEquals(200, reply.status());
        return reply.json();
    }

    // ------------------------------------------------------------------ order integrity

    // Regression: POST /orders bound the JPA entity, so supplying someone else's id overwrote their order.
    @Test
    void aCustomerCannotOverwriteAnotherCustomersOrderByPostingItsId() throws Exception {
        long victimOrder = placeOrder("customer.karim");
        JsonNode before = orderAsAdmin(victimOrder);

        Reply attack = call("POST", "/orders", demo("customer.amina"), """
                {"id": %d, "orderType":"MARKETPLACE","routingMode":"OPEN_BID","pickupAddress":"HACKED",
                 "deliveryAddress":"HACKED","recipientName":"x","recipientPhone":"1","orderAmount":1,
                 "vendorCompany":{"id":1}}""".formatted(victimOrder));

        assertEquals(201, attack.status());
        assertNotEquals(victimOrder, attack.json().path("id").asLong(), "must create a new order, not reuse the id");
        JsonNode after = orderAsAdmin(victimOrder);
        assertEquals(before.path("deliveryAddress").asText(), after.path("deliveryAddress").asText());
        assertEquals("customer.karim", after.path("customerUser").path("username").asText());
        assertEquals(before.path("orderAmount").asDouble(), after.path("orderAmount").asDouble());
    }

    // Regression: status, rating, review and the total were taken from the client.
    @Test
    void theServerDecidesStatusRatingAndTotalOnCreate() throws Exception {
        Reply reply = call("POST", "/orders", demo("customer.amina"), """
                {"orderType":"MARKETPLACE","routingMode":"OPEN_BID","pickupAddress":"a","deliveryAddress":"b",
                 "recipientName":"r","recipientPhone":"1","orderAmount":100,"deliveryFee":10,"totalAmount":0.01,
                 "status":"DELIVERED","rating":5,"review":"forged","trackingNumber":"MINE-1",
                 "customerUser":{"id":7},"vendorCompany":{"id":1}}""");

        assertEquals(201, reply.status(), reply.body());
        JsonNode order = reply.json();
        assertEquals("OPEN_FOR_BID", order.path("status").asText());
        assertTrue(order.path("rating").isNull() || order.path("rating").isMissingNode());
        assertTrue(order.path("review").isNull() || order.path("review").isMissingNode());
        assertEquals(110.0, order.path("totalAmount").asDouble(), 0.001);
        assertNotEquals("MINE-1", order.path("trackingNumber").asText());
        assertEquals("customer.amina", order.path("customerUser").path("username").asText());
    }

    @Test
    void aNegativeDeliveryFeeIsRejected() throws Exception {
        Reply reply = call("POST", "/orders", demo("customer.amina"), """
                {"orderType":"MARKETPLACE","pickupAddress":"a","deliveryAddress":"b","orderAmount":10,
                 "deliveryFee":-50,"vendorCompany":{"id":1}}""");
        assertEquals(400, reply.status());
    }

    // ------------------------------------------------------------------ cross-tenant writes

    // Regression: PUT /vendor-companies/{id} had no ownership check.
    @Test
    void aVendorCannotEditAnotherVendorsCompany() throws Exception {
        Reply reply = call("PUT", "/vendor-companies/2", demo("vendor.sofia"), "{\"companyName\":\"PWNED\"}");
        assertEquals(403, reply.status());
        Reply company = call("GET", "/vendor-companies/2", admin(), null);
        assertNotEquals("PWNED", company.json().path("companyName").asText());
    }

    @Test
    void aVendorCanEditItsOwnCompanyButNotItsCommission() throws Exception {
        double commission = call("GET", "/vendor-companies/1", admin(), null).json().path("commissionRate").asDouble();

        Reply reply = call("PUT", "/vendor-companies/1", demo("vendor.sofia"),
                "{\"companyName\":\"Atlas Market\",\"businessDescription\":\"Updated by the owner\",\"commissionRate\":0.0}");

        assertEquals(200, reply.status(), reply.body());
        JsonNode after = call("GET", "/vendor-companies/1", admin(), null).json();
        assertEquals("Updated by the owner", after.path("businessDescription").asText());
        assertEquals(commission, after.path("commissionRate").asDouble(), 0.0001, "the platform's commission is not the vendor's to change");
    }

    // Regression: every non-admin product write failed with a lazy-loading error (400), even the owner's.
    @Test
    void productEditsWorkForTheOwnerAndAreForbiddenForEveryoneElse() throws Exception {
        assertEquals(200, call("PUT", "/products/20", demo("vendor.youssef"), "{\"name\":\"Laptop Stand v2\",\"price\":199}").status());
        assertEquals(403, call("PUT", "/products/20", demo("vendor.sofia"), "{\"name\":\"PWNED\"}").status());
        assertEquals(403, call("PUT", "/products/20", demo("customer.amina"), "{\"name\":\"PWNED\"}").status());
    }

    // ------------------------------------------------------------------ order lifecycle

    @Test
    void ordersCanOnlyBeActedOnByTheirOwnPartiesAndAssignedWorkers() throws Exception {
        long karimsOrder = placeOrder("customer.karim");
        long youssefsOrder = findOrderOfCompany(2);

        assertEquals(403, call("PATCH", "/orders/" + karimsOrder + "/cancel?cancellationReason=x", demo("customer.amina"), null).status(),
                "another customer cancels it");
        assertEquals(403, call("PATCH", "/orders/" + karimsOrder + "/rating?rating=1", demo("customer.amina"), null).status(),
                "another customer rates it");
        assertEquals(403, call("PUT", "/orders/" + youssefsOrder, demo("vendor.sofia"), "{\"description\":\"tampered\"}").status(),
                "another vendor edits it");
        assertEquals(403, call("PATCH", "/orders/" + youssefsOrder + "/cancel?cancellationReason=x", demo("vendor.sofia"), null).status(),
                "another vendor cancels it");
        assertEquals(403, call("POST", "/orders/" + youssefsOrder + "/auto-assign", demo("vendor.sofia"), null).status(),
                "another vendor auto-assigns it");
        assertEquals(403, call("PATCH", "/orders/" + youssefsOrder + "/status?status=DELIVERED", demo("driver.ayoub"), null).status(),
                "a driver who is not assigned changes its status");
        assertEquals(200, call("PATCH", "/orders/" + karimsOrder + "/cancel?cancellationReason=changed+my+mind", demo("customer.karim"), null).status(),
                "the owner can still cancel it");
    }

    // Regression: any customer or driver could follow any order's live driver location.
    @Test
    void aStrangerCannotFollowAnotherCustomersDriverLocation() throws Exception {
        long order = findOrderOfCustomerWithDriver("customer.karim", "driver.hamza");
        Reply post = call("POST", "/tracking/orders/" + order + "/location", demo("driver.hamza"),
                "{\"orderNumber\":\"%s\",\"latitude\":33.57,\"longitude\":-7.58}".formatted(orderAsAdmin(order).path("orderNumber").asText()));
        assertEquals(200, post.status(), post.body());

        assertEquals(200, call("GET", "/tracking/orders/" + order + "/location", demo("customer.karim"), null).status(), "the owner");
        assertEquals(403, call("GET", "/tracking/orders/" + order + "/location", demo("customer.amina"), null).status(), "a stranger");
        assertEquals(403, call("POST", "/tracking/orders/" + order + "/location", demo("driver.ayoub"),
                "{\"orderNumber\":\"x\",\"latitude\":1,\"longitude\":1}").status(), "a driver who is not assigned");
    }

    // ------------------------------------------------------------------ the basics

    @Test
    void adminEndpointsAreForbiddenToCustomersAndEverythingNeedsAToken() throws Exception {
        for (String path : new String[]{"/dashboard/overview", "/customer-users", "/super-admins", "/driver-persons", "/orders"}) {
            assertEquals(403, call("GET", path, demo("customer.amina"), null).status(), path);
        }
        assertEquals(403, call("GET", "/orders/1", null, null).status(), "no token");
        assertEquals(200, call("GET", "/dashboard/overview", admin(), null).status());
    }

    @Test
    void clientMistakesAreA400OrA404NotA500() throws Exception {
        long order = placeOrder("customer.amina");
        assertEquals(400, call("PATCH", "/orders/" + order + "/cancel", demo("customer.amina"), null).status(),
                "missing required parameter");
        assertEquals(404, call("GET", "/definitely-not-an-endpoint", admin(), null).status());
    }

    @Test
    void responsesNeverContainPasswordHashes() throws Exception {
        String body = call("GET", "/orders/1", admin(), null).body();
        assertTrue(!body.toLowerCase().contains("\"password\""), "no password field in an order response");
        assertNull(JSON.readTree(body).path("customerUser").get("password"));
    }

    // ------------------------------------------------------------------ lookups into the demo data

    /** The order list is paged: the orders are under "content". */
    private JsonNode allOrders() throws Exception {
        JsonNode page = call("GET", "/orders?size=100", admin(), null).json();
        return page.has("content") ? page.get("content") : page;
    }

    private long findOrderOfCompany(long vendorCompanyId) throws Exception {
        for (JsonNode order : allOrders()) {
            if (order.path("vendorCompany").path("id").asLong() == vendorCompanyId) {
                return order.path("id").asLong();
            }
        }
        throw new AssertionError("no demo order for vendor company " + vendorCompanyId);
    }

    private long findOrderOfCustomerWithDriver(String customer, String driver) throws Exception {
        for (JsonNode order : allOrders()) {
            if (customer.equals(order.path("customerUser").path("username").asText())
                    && driver.equals(order.path("driverPerson").path("username").asText())) {
                return order.path("id").asLong();
            }
        }
        throw new AssertionError("no demo order of " + customer + " delivered by " + driver);
    }
}
