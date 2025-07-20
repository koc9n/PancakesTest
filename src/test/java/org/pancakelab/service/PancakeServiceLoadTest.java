package org.pancakelab.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.pancakelab.model.Order;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PancakeServiceLoadTest {
    private static final int NUM_CONCURRENT_USERS = 50;
    private static final int OPERATIONS_PER_USER = 20;
    private final ExecutorService executorService = Executors.newFixedThreadPool(NUM_CONCURRENT_USERS);
    private final ConcurrentMap<UUID, OrderState> testOrders = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private PancakeService pancakeService;

    @BeforeAll
    void setUp() {
        pancakeService = PancakeService.getInstance();
    }

    @Test
    void testConcurrentOrderCreationAndModification() throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(NUM_CONCURRENT_USERS);
        AtomicInteger successfulOperations = new AtomicInteger(0);
        AtomicInteger failedOperations = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < NUM_CONCURRENT_USERS; i++) {
            futures.add(executorService.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < OPERATIONS_PER_USER; j++) {
                        try {
                            performRandomOperation();
                            successfulOperations.incrementAndGet();
                            Thread.sleep(random.nextInt(10)); // Small random delay
                        } catch (Exception e) {
                            System.err.println("Operation failed: " + e.getMessage());
                            failedOperations.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            }));
        }

        long startTime = System.currentTimeMillis();
        startLatch.countDown();

        // Wait for all operations to complete with timeout
        if (!completionLatch.await(2, TimeUnit.MINUTES)) {
            System.err.println("Test timed out!");
        }
        long duration = System.currentTimeMillis() - startTime;

        cleanupTestOrders();
        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.SECONDS);

        System.out.println("Load Test Results:");
        System.out.println("Total duration: " + duration + "ms");
        System.out.println("Successful operations: " + successfulOperations.get());
        System.out.println("Failed operations: " + failedOperations.get());
        System.out.println("Operations per second: " +
                String.format("%.2f", (successfulOperations.get() * 1000.0) / duration));

        assertEquals(0, failedOperations.get(), "Some operations failed during load test");
    }

    private void performRandomOperation() {
        int operation = random.nextInt(5);
        switch (operation) {
            case 0 -> createNewOrder();
            case 1 -> addPancakeToRandomOrder();
            case 2 -> addIngredientToRandomPancake();
            case 3 -> cancelRandomOrder();
            case 4 -> viewRandomOrder();
        }
    }

    private void createNewOrder() {
        Order order = pancakeService.createOrder(
                random.nextInt(100) + 1,
                random.nextInt(500) + 1
        );
        testOrders.put(order.getId(), new OrderState(order));
    }

    private void addPancakeToRandomOrder() {
        if (testOrders.isEmpty()) {
            createNewOrder();
            return;
        }

        OrderState orderState = getRandomOrderState();
        if (orderState == null || orderState.cancelled) return;

        try {
            UUID pancakeId = pancakeService.addPancakeToOrder(orderState.order.getId());
            orderState.pancakes.add(pancakeId);
        } catch (IllegalArgumentException ignored) {
            // Order might have been cancelled by another thread
        }
    }

    private void addIngredientToRandomPancake() {
        OrderState orderState = getRandomOrderState();
        if (orderState == null || orderState.cancelled || orderState.pancakes.isEmpty()) return;

        UUID pancakeId = orderState.pancakes.iterator().next();
        String[] ingredients = {"dark chocolate", "milk chocolate", "hazelnuts", "whipped cream"};

        try {
            pancakeService.addIngredientToPancake(
                    orderState.order.getId(),
                    pancakeId,
                    ingredients[random.nextInt(ingredients.length)]
            );
        } catch (IllegalArgumentException ignored) {
            // Order or pancake might have been removed by another thread
        }
    }

    private void cancelRandomOrder() {
        OrderState orderState = getRandomOrderState();
        if (orderState == null || orderState.cancelled) return;

        try {
            pancakeService.cancelOrder(orderState.order.getId());
            orderState.cancelled = true;
        } catch (IllegalArgumentException ignored) {
            // Order might have been already cancelled
        }
    }

    private void viewRandomOrder() {
        OrderState orderState = getRandomOrderState();
        if (orderState == null || orderState.cancelled) return;

        try {
            pancakeService.viewOrder(orderState.order.getId());
        } catch (IllegalArgumentException ignored) {
            // Order might have been removed
        }
    }

    private OrderState getRandomOrderState() {
        if (testOrders.isEmpty()) return null;
        List<UUID> orderIds = new ArrayList<>(testOrders.keySet());
        return testOrders.get(orderIds.get(random.nextInt(orderIds.size())));
    }

    private void cleanupTestOrders() {
        testOrders.forEach((orderId, orderState) -> {
            if (!orderState.cancelled) {
                try {
                    pancakeService.cancelOrder(orderId);
                } catch (Exception ignored) {
                    // Order might have been already cancelled
                }
            }
        });
        testOrders.clear();
    }

    private static class OrderState {
        final Order order;
        final Set<UUID> pancakes = ConcurrentHashMap.newKeySet();
        volatile boolean cancelled = false;

        OrderState(Order order) {
            this.order = order;
        }
    }
}
