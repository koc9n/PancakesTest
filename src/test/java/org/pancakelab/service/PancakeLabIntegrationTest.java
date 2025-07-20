package org.pancakelab.service;

import org.junit.jupiter.api.*;
import org.pancakelab.model.Ingredient;
import org.pancakelab.model.Order;
import org.pancakelab.model.OrderState;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PancakeLabIntegrationTest {
    private static final Ingredient DARK_CHOCOLATE = new Ingredient("Dark Chocolate");
    private static final Ingredient MILK_CHOCOLATE = new Ingredient("Milk Chocolate");
    private OrderService orderService;
    private PancakeService pancakeService;
    private Order testOrder;

    @BeforeAll
    void setUp() {
        ServiceFactory serviceFactory = new ServiceFactory();
        orderService = serviceFactory.getOrderService();
        pancakeService = serviceFactory.getPancakeService();
    }

    @BeforeEach
    void createOrder() {
        testOrder = orderService.createOrder(10, 20);
    }

    @AfterEach
    void cleanup() {
        if (testOrder != null && testOrder.getId() != null) {
            try {
                orderService.cancelOrder(testOrder.getId());
            } catch (Exception ignored) {
                // Order might have been already cancelled or delivered
            }
        }
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    void testFullOrderFlow() {
        // Create pancake
        UUID pancakeId = pancakeService.createPancake(testOrder.getId());
        assertNotNull(pancakeId);

        // Add ingredients
        pancakeService.addIngredientToPancake(testOrder.getId(), pancakeId, DARK_CHOCOLATE);
        pancakeService.addIngredientToPancake(testOrder.getId(), pancakeId, MILK_CHOCOLATE);

        // Complete order
        orderService.completeOrder(testOrder.getId());
        assertEquals(OrderState.COMPLETED, orderService.getOrder(testOrder.getId()).get().getState());

        // Prepare order
        orderService.prepareOrder(testOrder.getId());
        assertEquals(OrderState.PREPARED, orderService.getOrder(testOrder.getId()).get().getState());

        // Start delivery
        orderService.startDelivery(testOrder.getId());
        assertTrue(orderService.getOrder(testOrder.getId()).isEmpty());
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    void testCancellationFlow() {
        // Create pancake and add ingredients
        UUID pancakeId = pancakeService.createPancake(testOrder.getId());
        pancakeService.addIngredientToPancake(testOrder.getId(), pancakeId, DARK_CHOCOLATE);

        // Cancel order
        orderService.cancelOrder(testOrder.getId());
        assertTrue(orderService.getOrder(testOrder.getId()).isEmpty());

        // Verify pancake is removed
        assertTrue(pancakeService.getPancake(testOrder.getId(), pancakeId).isEmpty());
    }
}
