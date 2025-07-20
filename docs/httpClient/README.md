# PancakeLab HTTP Client Test Collection

This folder contains organized HTTP client test files for testing the PancakeLab API using IntelliJ IDEA's built-in HTTP
client.

## 📁 File Organization

### **Core API Test Files:**

- `order-management.http` - Order CRUD operations (7 tests)
- `order-state-management.http` - Order lifecycle management (4 tests)
- `pancake-management.http` - Pancake operations (3 tests)
- `ingredient-management.http` - Ingredient operations (5 tests)
- `validation-tests.http` - Error handling & validation (7 tests)
- `rate-limiting-tests.http` - Performance testing (6 tests)
- `complete-workflow.http` - End-to-end scenario (10 tests)

### **Configuration:**

- `http-client.env.json` - Environment configuration (dev/test/prod)

## 🚀 Quick Start

1. **Start PancakeLab server:**
   ```bash
   mvn exec:java -Dexec.mainClass="org.pancakelab.Main"
   ```

2. **Open any `.http` file in IntelliJ IDEA**

3. **Run individual requests** by clicking the green arrow ▶️

## 📋 Recommended Testing Order

### **Sequential Testing (with variable sharing):**

1. `order-management.http` → Creates `{{orderId}}`
2. `pancake-management.http` → Creates `{{pancakeId}}`
3. `ingredient-management.http` → Uses both variables
4. `order-state-management.http` → Tests order lifecycle
5. `validation-tests.http` → Error scenarios
6. `rate-limiting-tests.http` → Performance limits

### **Independent Testing:**

- Each file can be run independently
- Variables are automatically shared across files
- Use `complete-workflow.http` for full end-to-end testing

## 🔧 Global Variables

The test files use shared variables that are automatically set:

- `{{orderId}}` - Set by order creation requests
- `{{pancakeId}}` - Set by pancake creation requests
- `{{ingredientId}}` - Set by ingredient addition requests
- `{{workflowOrderId}}` - Used in complete workflow tests

## 🌍 Environment Configuration

Switch environments by modifying `http-client.env.json`:

- **dev**: localhost:8080 (default)
- **test**: localhost:8081
- **prod**: production URL (when available)

## ✅ Test Assertions

Each request includes automatic validation:

- Status code verification
- Response body validation
- Business logic checks
- Error message validation

## 📊 API Coverage

### **Endpoints Tested:**

```
Order Management:
  POST   /api/orders
  GET    /api/orders
  GET    /api/orders?state={state}
  GET    /api/orders/{orderId}
  DELETE /api/orders/{orderId}

Order State:
  POST   /api/orders/{orderId}/complete
  POST   /api/orders/{orderId}/prepare
  POST   /api/orders/{orderId}/deliver
  POST   /api/orders/{orderId}/cancel

Pancake Management:
  POST   /api/orders/{orderId}/pancakes
  GET    /api/orders/{orderId}/pancakes
  DELETE /api/orders/{orderId}/pancakes/{pancakeId}

Ingredient Management:
  POST   /api/orders/{orderId}/pancakes/{pancakeId}/ingredients
  DELETE /api/orders/{orderId}/pancakes/{pancakeId}/ingredients/{ingredientId}
```

## 🛡️ Error Scenarios Covered

- Invalid building/room numbers
- Empty/too long ingredient names
- Non-existent resource IDs
- Rate limiting (429 errors)
- Invalid state transitions

## 💡 Tips

- **Run sequentially** for the first time to set up variables
- **Use individual files** for focused testing
- **Check test results** in the response panel
- **Modify requests** as needed for your testing scenarios
