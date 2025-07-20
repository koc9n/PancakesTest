package org.pancakelab.http;

import org.junit.jupiter.api.*;
import org.pancakelab.http.dto.CreatePancakeResponse;
import org.pancakelab.http.dto.OrderResponse;

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

    @BeforeAll
    void setUp() throws Exception {
        server = new PancakeHttpServer(8080, 10);
        server.start();
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @AfterAll
    void tearDown() {
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

        // Extract orderId for subsequent tests
        orderId = JsonUtil.parseResponse(response, OrderResponse.class).orderId();
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
        assertTrue(response.body().contains("pancakeId"));
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
        String pancakeId = JsonUtil.parseResponse(pancakeResponse, CreatePancakeResponse.class).pancakeId();

        // Then add an ingredient
        String ingredientBody = """
                {
                    "ingredient": "dark chocolate"
                }""";

        HttpRequest addIngredientRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/orders/" + orderId + "/pancakes/" + pancakeId + "/ingredient"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(ingredientBody))
                .build();

        HttpResponse<String> response = client.send(addIngredientRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
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

        // Test invalid ingredient (empty)
        String invalidIngredientBody = """
                {
                    "ingredient": ""
                }""";

        request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/orders/" + orderId + "/pancakes/" + UUID.randomUUID() + "/ingredient"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(invalidIngredientBody))
                .build();

        response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("Ingredient cannot be empty"));
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
