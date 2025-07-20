package org.pancakelab.http;

import org.junit.jupiter.api.*;
import org.pancakelab.http.dto.OrderResponse;
import org.pancakelab.http.dto.PancakeResponse;
import org.pancakelab.service.OrderService;
import org.pancakelab.service.PancakeService;
import org.pancakelab.service.ServiceFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PancakeApiTest {
    private static final String BASE_URL = "http://localhost:8080/api";
    private static PancakeHttpServer server;
    private static HttpClient client;
    private String orderId;
    private OrderService orderService;
    private PancakeService pancakeService;

    @BeforeAll
    void setUp() throws Exception {
        ServiceFactory serviceFactory = new ServiceFactory();
        orderService = serviceFactory.getOrderService();
        pancakeService = serviceFactory.getPancakeService();

        server = new PancakeHttpServer(8080, 10, serviceFactory);
        server.start();
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @AfterAll
    void tearDown() {
        if (orderId != null) {
            try {
                orderService.cancelOrder(UUID.fromString(orderId));
            } catch (Exception ignored) {
                // Order might have been already cancelled or delivered
            }
        }
        server.stop();
    }

    @Test
    @Order(1)
    void testCreateOrder() throws Exception {
        String requestBody = """
                {
                    "building": 10,
                    "room": 20
                }""";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/orders"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());
        assertTrue(response.body().contains("orderId"));

        // Extract orderId for subsequent tests using correct field name
        orderId = JsonUtil.parseResponse(response, OrderResponse.class).orderId().toString();
    }

    @Test
    @Order(2)
    void testCreatePancake() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/orders/" + orderId + "/pancakes"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());
        assertTrue(response.body().contains("\"id\""));

        // Verify we can parse it as PancakeResponse
        PancakeResponse pancake = JsonUtil.parseResponse(response, PancakeResponse.class);
        assertNotNull(pancake.id());
    }

    @Test
    @Order(3)
    void testAddIngredient() throws Exception {
        // First create a pancake
        HttpRequest createPancakeRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/orders/" + orderId + "/pancakes"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        HttpResponse<String> pancakeResponse = client.send(createPancakeRequest, HttpResponse.BodyHandlers.ofString());
        String pancakeId = JsonUtil.parseResponse(pancakeResponse, PancakeResponse.class).id().toString();

        // Then add an ingredient with correct field name
        String ingredientBody = """
                {
                    "name": "dark chocolate"
                }""";

        HttpRequest addIngredientRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/orders/" + orderId + "/pancakes/" + pancakeId + "/ingredients"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(ingredientBody))
                .build();

        HttpResponse<String> response = client.send(addIngredientRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode());  // Changed to 201 since we're creating a resource
    }

    @Test
    @Order(4)
    void testGetOrder() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/orders/" + orderId))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        OrderResponse order = JsonUtil.parseResponse(response, OrderResponse.class);
        assertEquals(10, order.building());
        assertEquals(20, order.room());
        assertFalse(order.pancakes().isEmpty());
    }

    @Test
    @Order(5)
    void testValidationErrors() throws Exception {
        // Test invalid building number
        String invalidOrderBody = """
                {
                    "building": -1,
                    "room": 20
                }""";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/orders"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(invalidOrderBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("Building number must be positive"));

        // Create a valid order first for pancake/ingredient testing
        String validOrderBody = """
                {
                    "building": 1,
                    "room": 20
                }""";

        HttpRequest createOrderRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/orders"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(validOrderBody))
                .build();

        HttpResponse<String> orderResponse = client.send(createOrderRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, orderResponse.statusCode());

        OrderResponse order = JsonUtil.parseResponse(orderResponse, OrderResponse.class);
        String testOrderId = order.orderId().toString();

        // Create a pancake for ingredient validation testing
        HttpRequest createPancakeRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/orders/" + testOrderId + "/pancakes"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        HttpResponse<String> pancakeResponse = client.send(createPancakeRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, pancakeResponse.statusCode());

        // Parse the pancake creation response correctly
        String pancakeResponseBody = pancakeResponse.body();
        assertTrue(pancakeResponseBody.contains("\"id\""), "Response should contain id field: " + pancakeResponseBody);

        // Extract pancake ID from the simple JSON response
        String pancakeId = pancakeResponseBody.replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        // Test invalid ingredient (empty) with correct field name
        String invalidIngredientBody = """
                {
                    "name": ""
                }""";

        request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/orders/" + testOrderId + "/pancakes/" + pancakeId + "/ingredients"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(invalidIngredientBody))
                .build();

        response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("Ingredient name cannot be empty"));

        // Clean up the test order
        try {
            orderService.cancelOrder(UUID.fromString(testOrderId));
        } catch (Exception ignored) {
            // Order cleanup - ignore errors
        }
    }

    @Test
    @Order(6)
    void testCancelOrder() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/orders/" + orderId))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(204, response.statusCode());

        // Verify order is gone
        request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/orders/" + orderId))
                .GET()
                .build();

        response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
    }
}
