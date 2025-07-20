# PancakeLab Architecture Documentation

## Overview

PancakeLab is a REST API service for managing pancake orders with a Router-based architecture and simplified concurrency
model.

## System Architecture

### Layer Structure

```
┌─────────────────────────────────────────────────────────────┐
│                    HTTP Layer                               │
├─────────────────────────────────────────────────────────────┤
│  PancakeHttpServer → ApiHandler → Router → Controllers     │
│       ↓                    ↓           ↓         ↓         │
│  TimeoutHandler     RateLimiter    HttpUtils  JsonUtil     │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                 Controller Layer                            │
├─────────────────────────────────────────────────────────────┤
│            OrderController    PancakeController             │
│                    ↓                    ↓                   │
│              RequestValidator    ValidationException        │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                  Service Layer                              │
├─────────────────────────────────────────────────────────────┤
│      ServiceFactory → OrderService → PancakeService        │
│           ↓              ↓              ↓                   │
│    OrderServiceImpl  PancakeServiceImpl                     │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                   Model Layer                               │
├─────────────────────────────────────────────────────────────┤
│         Order → Pancake → Ingredient                        │
│           ↓       ↓         ↓                               │
│      OrderState (enum)                                      │
└─────────────────────────────────────────────────────────────┘
```

## API Endpoints

### Order Management

```
POST   /api/orders                    → Create order
GET    /api/orders                    → Get all orders
GET    /api/orders/{orderId}          → Get specific order
DELETE /api/orders/{orderId}          → Delete order
```

### Order State Management

```
POST   /api/orders/{orderId}/complete → Complete order
POST   /api/orders/{orderId}/prepare  → Prepare order
POST   /api/orders/{orderId}/deliver  → Start delivery
POST   /api/orders/{orderId}/cancel   → Cancel order
```

### Pancake Management

```
POST   /api/orders/{orderId}/pancakes                    → Create pancake
GET    /api/orders/{orderId}/pancakes                    → Get pancakes
DELETE /api/orders/{orderId}/pancakes/{pancakeId}        → Delete pancake
```

### Ingredient Management

```
POST   /api/orders/{orderId}/pancakes/{pancakeId}/ingredients               → Add ingredient
DELETE /api/orders/{orderId}/pancakes/{pancakeId}/ingredients/{ingredientId} → Remove ingredient
```

## Key Components

### Router-Based Architecture

- **Router**: Handles URL pattern matching with named parameters `{orderId}`, `{pancakeId}`
- **ApiHandler**: Central request dispatcher, replaces old OrderHandler
- **Controllers**: Separated by domain (OrderController, PancakeController)

### Simplified Concurrency Model

- **Before**: Complex atomic operations with retry loops and exponential backoff
- **After**: Simple synchronized methods in Order and Pancake classes
- **Benefits**: Easier to understand, maintain, and debug

### Error Handling

- **ValidationException**: Custom exception with HTTP status codes
- **Centralized**: All controllers use consistent error response format
- **Status Codes**: Proper HTTP status codes (400, 404, 500)

### Request Flow

1. **Client** sends HTTP request
2. **PancakeHttpServer** receives request
3. **TimeoutHandler** wraps with timeout protection
4. **ApiHandler** applies rate limiting
5. **Router** matches URL pattern and extracts parameters
6. **Controller** handles business logic and validation
7. **Service** performs operations on models
8. **Model** executes synchronized operations
9. **Response** sent back through HttpUtils

## DTOs (Data Transfer Objects)

- **CreateOrderRequest**: `{building: int, room: int}`
- **OrderResponse**: `{orderId: UUID, building: int, room: int, state: OrderState, pancakes: List}`
- **PancakeResponse**: `{id: UUID, ingredients: List}`
- **AddIngredientRequest**: `{name: String}`

## Configuration

- **Server Port**: 8080 (default)
- **Thread Pool**: 10 threads (default)
- **Request Timeout**: Configurable
- **Rate Limiting**: Per-client IP
- **Backlog Size**: Configurable

## Testing

- **Integration Tests**: PancakeApiTest with full HTTP flow
- **Unit Tests**: Service layer tests
- **Load Tests**: PancakeServiceLoadTest for performance

## Deployment

```bash
mvn clean compile
mvn test
mvn exec:java -Dexec.mainClass="org.pancakelab.Main"
```

Server starts on http://localhost:8080/api
