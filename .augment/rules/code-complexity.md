# Code Complexity Code Review Guidelines
### Standards for cyclomatic complexity, nesting depth, and clean abstractions

**Description:** Guidelines to minimize cognitive load, maintainable function lengths, and healthy dependency structures.  
**Applicable Files:** All source code (`.java`, `.ts`, `.py`, `.go`, etc.).

---

## 🌀 Cyclomatic & Cognitive Complexity

### [enforce-complexity-thresholds] - Severity: High
Maintain a low cyclomatic complexity (target: **< 10-15** per function).
- **Impact:** High complexity exponentially increases the number of test cases required and makes code impossible to hold in mental memory.
- **Action:** Use early returns (guard clauses) to flatten logic and extract complex conditional blocks into named helper methods.

### [avoid-nested-ternaries] - Severity: Medium
**Never** nest ternary operators. They are notoriously difficult to debug and read.
- **Solution:** Use `if-else` blocks or assign the intermediate results to well-named variables to clarify intent.

---

## 📏 Function & Parameter Limits

### [enforce-line-limits] - Severity: Medium
Keep functions focused and short (target: **20-50 lines**).
- **Single Responsibility:** If a function needs the word "and" to describe what it does, it is doing too much. Extract coherent sub-tasks into private methods.

### [limit-parameters-and-flags] - Severity: Medium
- **Parameter Count:** More than **3-4 parameters** is a code smell. Use a "Parameter Object" or the Builder pattern.
- **Boolean Flags:** Avoid passing booleans that change a function's behavior (e.g., `processOrder(order, true)`). Instead, create two distinct functions: `processDigitalOrder(order)` and `processPhysicalOrder(order)`.

---

## 🕸️ Nesting & Flow Control

### [limit-nesting-depth] - Severity: High
Limit nesting to a maximum of **3-4 levels**.
- **Action:** Deeply nested loops and conditionals should be refactored using **Guard Clauses**.
- **❌ BAD:** Wrapping your entire function body in an `if (user != null) { ... }` block.
- **✅ GOOD:** `if (user == null) return;` at the very top.

### [avoid-callback-hell] - Severity: High
In **TypeScript/Node.js**, always prefer `async/await` over nested callbacks or manual `.then()` chains. Flat asynchronous code is significantly easier to trace and handles errors more predictably.

---

## 🖇️ Dependency & Coupling

### [prevent-circular-dependencies] - Severity: Critical
Circular dependencies (A depends on B, which depends on A) indicate a fundamental design flaw and often lead to initialization failures in **Spring Boot** or **Node.js**.
- **Action:** Use dependency analysis tools to break cycles, usually by introducing a shared Interface or moving common logic to a third "Leaf" module.

### [minimize-coupling] - Severity: High
Aim for **Loose Coupling** and **High Cohesion**.
- Components should interact via Interfaces/Contracts, not concrete implementations.
- Related functionality should live within the same module, while unrelated logic should be strictly separated to allow for independent changes.

### [respect-dependency-direction] - Severity: High
Dependencies must flow from **High-Level (Domain/Business Logic)** to **Low-Level (Infrastructure/Database)**.
- **Rule:** Your core business entities should never import a database driver or a specific Dapr client. Use the **Dependency Inversion Principle**.
