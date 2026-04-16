# TypeScript Code Review Guidelines
### Comprehensive rules for code quality, type safety, and best practices

**Description:** Guidelines for reviewing TypeScript/JavaScript code covering type safety, security, performance, and best practices.  
**Applicable Files:** `**/*.ts`, `**/*.tsx`

---

## 🏗️ Type Safety

### [avoid-any-type] - Severity: High
Avoid using `any` as it disables type checking.
- Use `unknown` for truly unknown types and narrow with type guards.
- If `any` is unavoidable, add a comment explaining why.

### [proper-null-checks] - Severity: High
Always handle `null` and `undefined` explicitly.
- Enable `strictNullChecks` in `tsconfig`.
- Use optional chaining (`?.`) and nullish coalescing (`??`).
- Never use non-null assertions (`!`) without absolute certainty.

### [enable-strict-mode] - Severity: High
Enable strict mode in `tsconfig.json`. This includes:
- `strictNullChecks`, `strictFunctionTypes`, `strictBindCallApply`, `noImplicitAny`, and `noImplicitThis`.

### [avoid-type-assertions] - Severity: Medium
Minimize use of type assertions (`as Type`). They bypass type checking.
- Prefer type guards or type narrowing.
- If required, use `unknown` as an intermediate step.

### [exhaustive-switch-checks] - Severity: Medium
Use exhaustive type checking in switch statements with the `never` type. This ensures compile errors occur when new union members are added but not handled.

---

## 🛡️ Security

### [prevent-xss] - Severity: High
Never use `innerHTML`, `outerHTML`, or `document.write` with user input.
- Use `textContent` for plain text.
- Sanitize HTML with **DOMPurify**.
- In React, avoid `dangerouslySetInnerHTML`.

### [prevent-prototype-pollution] - Severity: High
Validate object keys before using bracket notation with user input.
- Use `Object.hasOwn()` instead of `hasOwnProperty`.
- Consider using a `Map` for user-provided keys.

### [validate-external-data] - Severity: High
Always validate data from external sources (APIs, LocalStorage).
- Use runtime validation libraries like **Zod** or **io-ts**.
- Never trust type assertions for external data.

### [sanitize-urls] - Severity: High
Validate URLs before use. Check for `javascript:` protocols or data URIs. Use the `URL` constructor for parsing.

---

## ⚡ Performance

### [optimize-bundle-size] - Severity: Medium
- Use **named imports** instead of default imports to allow tree-shaking.
- Avoid importing entire libraries (e.g., `lodash`) when specific functions suffice.
- Use **dynamic imports** for code splitting.

### [avoid-memory-leaks] - Severity: High
- Clean up event listeners, subscriptions, and timers in cleanup functions (e.g., `useEffect` return).
- Use `WeakMap` or `WeakSet` for object references that shouldn't prevent garbage collection.

### [memoize-expensive-computations] - Severity: Medium
- In React, use `useMemo` and `useCallback`.
- Cache results of pure functions and be mindful of referential equality in dependency arrays.

---

## 🛠️ Best Practices

### [interface-vs-type] - Severity: Low
- Use **interfaces** for object shapes that might be extended.
- Use **type aliases** for unions, intersections, and mapped types.

### [avoid-enums] - Severity: Low
Prefer **const objects** (`as const`) or **union types** over enums. Enums have runtime overhead and inconsistent behavior.

### [use-readonly] - Severity: Medium
Use the `readonly` modifier for properties that shouldn't change. Leverage `ReadonlyArray` and `Object.freeze()` for immutable data.

### [proper-error-handling] - Severity: Medium
- Create custom error classes extending `Error`.
- Never throw non-Error objects (e.g., `throw "error"`).
- Consider `Result` or `Either` patterns for recoverable errors.

### [discriminated-unions] - Severity: Medium
Use discriminated unions with a literal type discriminator (e.g., `type: 'success' | 'error'`) for type-safe state management.

### [avoid-object-type] - Severity: Medium
Avoid using `Object`, `object`, or `{}` as types.
- Use `Record<string, unknown>` for key-value pairs.
- Use `unknown` for truly unknown shapes.

### [use-branded-types] - Severity: Low
Consider **Branded Types** (e.g., `UserId`, `OrderId`) to prevent accidentally mixing structurally identical IDs.
