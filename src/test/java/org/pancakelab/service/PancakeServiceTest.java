package org.pancakelab.service;

import org.junit.jupiter.api.*;
import org.pancakelab.model.Ingredient;
import org.pancakelab.model.Order;
import org.pancakelab.model.OrderState;
import org.pancakelab.model.Pancake;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PancakeServiceTest {
    private static final Ingredient DARK_CHOCOLATE = new Ingredient("Dark Chocolate");
    private static final Ingredient MILK_CHOCOLATE = new Ingredient("Milk Chocolate");
    private static final Ingredient HAZELNUTS = new Ingredient("Hazelnuts");
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
        testOrder = null;
    }

    @Test
    @org.junit.jupiter.api.Order(10)
    void whenCreatingOrder_thenOrderHasCorrectData() {
        assertEquals(10, testOrder.getBuilding());
        assertEquals(20, testOrder.getRoom());
        assertEquals(OrderState.OPEN, testOrder.getState());
        assertTrue(testOrder.getPancakes().isEmpty());
    }

    @Test
    @org.junit.jupiter.api.Order(20)
    void whenAddingPancakesWithIngredients_thenPancakesAreCorrectlyAdded() {
        // Create pancakes with ingredients
        UUID pancake1Id = pancakeService.createPancake(testOrder.getId());
        UUID pancake2Id = pancakeService.createPancake(testOrder.getId());

        // Add ingredients to first pancake
        pancakeService.addIngredientToPancake(testOrder.getId(), pancake1Id, DARK_CHOCOLATE);
        pancakeService.addIngredientToPancake(testOrder.getId(), pancake1Id, HAZELNUTS);

        // Add ingredients to second pancake
        pancakeService.addIngredientToPancake(testOrder.getId(), pancake2Id, MILK_CHOCOLATE);

        // Verify pancakes
        List<Pancake> pancakes = pancakeService.getPancakesByOrder(testOrder.getId());
        assertEquals(2, pancakes.size());

        // Verify first pancake ingredients
        Optional<Pancake> firstPancake = pancakeService.getPancake(testOrder.getId(), pancake1Id);
        assertTrue(firstPancake.isPresent());
        assertEquals(2, firstPancake.get().ingredients().size());
        assertTrue(firstPancake.get().ingredients().stream()
                .anyMatch(i -> i.getName().equals("Dark Chocolate")));
        assertTrue(firstPancake.get().ingredients().stream()
                .anyMatch(i -> i.getName().equals("Hazelnuts")));

        // Verify second pancake ingredients
        Optional<Pancake> secondPancake = pancakeService.getPancake(testOrder.getId(), pancake2Id);
        assertTrue(secondPancake.isPresent());
        assertEquals(1, secondPancake.get().ingredients().size());
        assertTrue(secondPancake.get().ingredients().stream()
                .anyMatch(i -> i.getName().equals("Milk Chocolate")));
    }

    @Test
    @org.junit.jupiter.api.Order(30)
    void whenRemovingIngredients_thenIngredientsAreCorrectlyRemoved() {
        // Create pancake with ingredients
        UUID pancakeId = pancakeService.createPancake(testOrder.getId());

        pancakeService.addIngredientToPancake(testOrder.getId(), pancakeId, DARK_CHOCOLATE);
        pancakeService.addIngredientToPancake(testOrder.getId(), pancakeId, MILK_CHOCOLATE);
        pancakeService.addIngredientToPancake(testOrder.getId(), pancakeId, HAZELNUTS);

        // Get the ingredient IDs
        Optional<Pancake> pancake = pancakeService.getPancake(testOrder.getId(), pancakeId);
        assertTrue(pancake.isPresent());
        assertEquals(3, pancake.get().ingredients().size());

        // Remove one ingredient
        UUID ingredientToRemove = pancake.get().ingredients()
                .stream()
                .filter(i -> i.getName().equals("Hazelnuts"))
                .findFirst()
                .map(Ingredient::getId)
                .orElseThrow();

        pancakeService.removeIngredientFromPancake(testOrder.getId(), pancakeId, ingredientToRemove);

        // Verify
        pancake = pancakeService.getPancake(testOrder.getId(), pancakeId);
        assertTrue(pancake.isPresent());
        assertEquals(2, pancake.get().ingredients().size());
        assertFalse(pancake.get().ingredients().stream()
                .anyMatch(i -> i.getName().equals("Hazelnuts")));
    }

    @Test
    @org.junit.jupiter.api.Order(40)
    void whenOrderStateChanges_thenStateIsCorrectlyUpdated() {
        // Add some pancakes
        UUID pancakeId = pancakeService.createPancake(testOrder.getId());
        pancakeService.addIngredientToPancake(testOrder.getId(), pancakeId, DARK_CHOCOLATE);

        // Complete order
        orderService.completeOrder(testOrder.getId());
        Optional<Order> completedOrder = orderService.getOrder(testOrder.getId());
        assertTrue(completedOrder.isPresent());
        assertEquals(OrderState.COMPLETED, completedOrder.get().getState());

        // Prepare order
        orderService.prepareOrder(testOrder.getId());
        Optional<Order> preparedOrder = orderService.getOrder(testOrder.getId());
        assertTrue(preparedOrder.isPresent());
        assertEquals(OrderState.PREPARED, preparedOrder.get().getState());

        // Start delivery
        orderService.startDelivery(testOrder.getId());
        Optional<Order> deliveredOrder = orderService.getOrder(testOrder.getId());
        assertTrue(deliveredOrder.isEmpty()); // Order should be removed after delivery
    }

    @Test
    @org.junit.jupiter.api.Order(50)
    void whenOrderIsCancelled_thenOrderIsRemovedFromSystem() {
        UUID orderId = testOrder.getId();
        orderService.cancelOrder(orderId);

        Optional<Order> cancelledOrder = orderService.getOrder(orderId);
        assertTrue(cancelledOrder.isEmpty());
    }

    @Test
    @org.junit.jupiter.api.Order(60)
    void whenOrderIsNotOpen_thenCannotModifyPancakes() {
        // Create pancake while order is open
        UUID pancakeId = pancakeService.createPancake(testOrder.getId());

        // Complete the order
        orderService.completeOrder(testOrder.getId());

        // Try to modify after completion
        assertThrows(IllegalStateException.class, () -> pancakeService.createPancake(testOrder.getId()));

        assertThrows(IllegalStateException.class, () -> pancakeService.addIngredientToPancake(testOrder.getId(), pancakeId, DARK_CHOCOLATE));
    }
}
