package org.pancakelab.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.pancakelab.model.Ingredient;
import org.pancakelab.model.Order;
import org.pancakelab.model.OrderState;
import org.pancakelab.model.Pancake;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PancakeServiceLoadTest {
    private static final int NUM_CONCURRENT_USERS = 50;
    private static final int OPERATIONS_PER_USER = 20;
    private static final int MAX_RETRIES = 3;
    private final ExecutorService executorService = Executors.newFixedThreadPool(NUM_CONCURRENT_USERS);
    private final Random random = new Random();

    private final List<String> availableIngredients = Arrays.asList(
            "Chocolate",
            "Berries",
            "Cream"
    );
    private PancakeService pancakeService;
    private OrderService orderService;

    @BeforeAll
    void setUp() {
        ServiceFactory serviceFactory = new ServiceFactory();
        orderService = serviceFactory.getOrderService();
        pancakeService = serviceFactory.getPancakeService();
    }

    @Test
    void testConcurrentOrderProcessing() throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(NUM_CONCURRENT_USERS);
        AtomicInteger successfulOperations = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();

        // Create concurrent users
        for (int i = 0; i < NUM_CONCURRENT_USERS; i++) {
            futures.add(executorService.submit(() -> {
                Map<UUID, OrderState> threadLocalOrders = new ConcurrentHashMap<>();
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    for (int j = 0; j < OPERATIONS_PER_USER; j++) {
                        try {
                            processRandomOperation(threadLocalOrders);
                            successfulOperations.incrementAndGet();
                        } catch (Exception e) {
                            // Log but continue with other operations
                            System.err.println("Operation failed: " + e.getMessage());
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    // Cleanup this thread's orders
                    cleanupOrders(threadLocalOrders);
                    completionLatch.countDown();
                }
            }));
        }

        // Start all threads simultaneously
        startLatch.countDown();

        // Wait for all operations to complete
        completionLatch.await(2, TimeUnit.MINUTES);

        // Verify results
        int totalOperations = NUM_CONCURRENT_USERS * OPERATIONS_PER_USER;
        int successRate = (successfulOperations.get() * 100) / totalOperations;
        System.out.println("Success rate: " + successRate + "% (" + successfulOperations.get() + "/" + totalOperations + ")");
        assertTrue(successRate > 90, "Success rate should be above 90%");

        executorService.shutdown();
        assertTrue(executorService.awaitTermination(1, TimeUnit.MINUTES));
    }

    private void processRandomOperation(Map<UUID, OrderState> threadLocalOrders) {
        int retries = 0;
        while (retries < MAX_RETRIES) {
            try {
                switch (random.nextInt(5)) {
                    case 0 -> createOrder(threadLocalOrders);
                    case 1 -> addPancakeToOrder(threadLocalOrders);
                    case 2 -> addIngredientToPancake(threadLocalOrders);
                    case 3 -> completeOrder(threadLocalOrders);
                    case 4 -> cancelOrder(threadLocalOrders);
                }
                return;
            } catch (Exception e) {
                retries++;
                if (retries == MAX_RETRIES) {
                    throw e;
                }
                // Small delay before retry
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ie);
                }
            }
        }
    }

    private void createOrder(Map<UUID, OrderState> threadLocalOrders) {
        Order order = orderService.createOrder(
                random.nextInt(50) + 1,
                random.nextInt(100) + 1
        );
        threadLocalOrders.put(order.getId(), order.getState());
    }

    private void addPancakeToOrder(Map<UUID, OrderState> threadLocalOrders) {
        if (threadLocalOrders.isEmpty()) {
            createOrder(threadLocalOrders);
            return;
        }

        UUID orderId = getRandomOrder(threadLocalOrders);
        if (orderId != null) {
            OrderState state = threadLocalOrders.get(orderId);
            if (state == OrderState.OPEN) {
                try {
                    pancakeService.createPancake(orderId);
                } catch (IllegalStateException e) {
                    // Order might have been completed or cancelled
                    threadLocalOrders.remove(orderId);
                    throw e;
                }
            }
        }
    }

    private void addIngredientToPancake(Map<UUID, OrderState> threadLocalOrders) {
        UUID orderId = getRandomOrder(threadLocalOrders);
        if (orderId == null) return;

        OrderState state = threadLocalOrders.get(orderId);
        if (state == OrderState.OPEN) {
            try {
                List<Pancake> pancakes = pancakeService.getPancakesByOrder(orderId);
                if (!pancakes.isEmpty()) {
                    Pancake pancake = pancakes.get(random.nextInt(pancakes.size()));
                    String ingredient = availableIngredients.get(random.nextInt(availableIngredients.size()));
                    pancakeService.addIngredientToPancake(orderId, pancake.getId(), new Ingredient(ingredient));
                }
            } catch (IllegalStateException e) {
                // Order might have been completed or cancelled
                threadLocalOrders.remove(orderId);
                throw e;
            }
        }
    }

    private void completeOrder(Map<UUID, OrderState> threadLocalOrders) {
        UUID orderId = getRandomOrder(threadLocalOrders);
        if (orderId == null) return;

        OrderState state = threadLocalOrders.get(orderId);
        if (state == OrderState.OPEN) {
            try {
                orderService.completeOrder(orderId);
                threadLocalOrders.put(orderId, OrderState.COMPLETED);
            } catch (IllegalStateException e) {
                // Order might have been cancelled
                threadLocalOrders.remove(orderId);
                throw e;
            }
        }
    }

    private void cancelOrder(Map<UUID, OrderState> threadLocalOrders) {
        UUID orderId = getRandomOrder(threadLocalOrders);
        if (orderId != null) {
            try {
                orderService.cancelOrder(orderId);
                threadLocalOrders.remove(orderId);
            } catch (IllegalStateException ignored) {
                // Order might have been already cancelled or delivered
                threadLocalOrders.remove(orderId);
            }
        }
    }

    private UUID getRandomOrder(Map<UUID, OrderState> threadLocalOrders) {
        if (threadLocalOrders.isEmpty()) {
            return null;
        }
        List<UUID> orderIds = new ArrayList<>(threadLocalOrders.keySet());
        return orderIds.get(random.nextInt(orderIds.size()));
    }

    private void cleanupOrders(Map<UUID, OrderState> threadLocalOrders) {
        for (UUID orderId : threadLocalOrders.keySet()) {
            try {
                orderService.cancelOrder(orderId);
            } catch (Exception ignored) {
                // Order might have been already cancelled or delivered
            }
        }
        threadLocalOrders.clear();
    }
}
