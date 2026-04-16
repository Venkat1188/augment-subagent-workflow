# Naming Conventions Code Review Guidelines
### Standards for variables, functions, classes, files, and constants

**Description:** Guidelines for naming across all project languages to ensure code clarity, consistency, and intent-driven design.  
**Applicable Files:** All source code (`.java`, `.ts`, `.py`, `.go`, etc.).

---

## 📦 Variable Naming

### [use-descriptive-variable-names] - Severity: Medium
Variables must describe their purpose.
- **❌ Avoid:** `data`, `temp`, `info`, or single letters (except `i`, `j` for loop counters).
- **✅ Good:** `activeCustomerCount`, `orderTotalInCents`.

### [use-consistent-casing] - Severity: Medium
Follow language-specific conventions:
- **camelCase:** Java, JavaScript, TypeScript, Kotlin.
- **snake_case:** Python, Ruby, Rust, Go (for non-exported).
- **PascalCase:** Classes and C# properties.

### [use-meaningful-collection-names] - Severity: Medium
Collection names should indicate both content and pluralization.
- **Lists/Arrays:** `customers` (plural), not `customerList`.
- **Maps/Dictionaries:** `userById` or `emailToAccountMap`.

### [use-positive-boolean-names] - Severity: Medium
Booleans should be named as positive questions.
- **✅ Good:** `isActive`, `hasPermission`, `isComplete`.
- **❌ Avoid:** `isNotDisabled` (prevents double-negatives).

---

## 🛠️ Function & Method Naming

### [use-verb-based-function-names] - Severity: Medium
Functions represent actions and should start with a verb.
- **Patterns:** `getUser()`, `calculateTax()`, `validateSchema()`.
- **Consistency:** Use consistent prefixes: `create/delete` for CRUD, `on/handle` for events.

### [avoid-side-effects-in-getters] - Severity: High
Functions named `get...` should be idempotent and read-only.
- **Action:** If a function triggers an I/O operation, network call, or state change, use verbs like `fetch`, `load`, or `sync`.

### [name-booleans-as-questions] - Severity: Medium
Boolean-returning functions should read naturally in conditionals: `if (user.hasAccess())`.

---

## 🏛️ Class & Interface Naming

### [use-noun-based-class-names] - Severity: Medium
Classes represent entities or things.
- **✅ Good:** `OrderProcessor`, `CustomerRepository`, `AuthService`.
- **❌ Avoid:** Verbs like `ProcessOrder` (should be a method inside a class).

### [avoid-vague-suffixes] - Severity: Medium
Avoid vague names like `Manager`, `Helper`, `Util`, or `Handler`.
- **Solution:** Be specific. Instead of `UserManager`, use `UserRegistrationService` or `UserPermissionValidator`.

### [use-suffix-for-patterns] - Severity: Low
Communicate intent by including design pattern names: `PaymentStrategy`, `UserFactory`, `OrderBuilder`.

---

## 📁 File Naming & Structure

### [match-filename-to-export] - Severity: Medium
The file name should match the primary export exactly.
- `UserService.ts` should export `class UserService`.
- `UserAvatar.tsx` should export `function UserAvatar`.

### [use-consistent-file-structure] - Severity: Medium
- **PascalCase:** React Components (`HeaderComponent.tsx`).
- **kebab-case:** Modules and general files in TS/JS (`api-client.ts`).
- **snake_case:** Python files (`data_processor.py`).

---

## 📍 Constants & Magic Values

### [use-uppercase-for-constants] - Severity: Medium
Use `SCREAMING_SNAKE_CASE` for true, global constants.
- **✅ Good:** `MAX_RETRY_ATTEMPTS`, `DEFAULT_TIMEOUT_MS`.
- **Note:** Do not use this for every `const` in JS/TS, only for immutable, global configuration values.

### [avoid-magic-numbers] - Severity: High
Replace raw numbers or strings with named constants.
- **❌ BAD:** `if (status === 3)`.
- **✅ GOOD:** `if (status === OrderStatus.SHIPPED)`.
