# React Code Review Guidelines
### Comprehensive rules for React performance, state management, and best practices

**Description:** React best practices covering performance optimization, state management, effects, component design, hooks, and common pitfalls.  
**Applicable Files:** `**/*.jsx`, `**/*.tsx`, `**/components/**/*.{js,ts}`, `**/*Component*`, `**/*Hook*`

---

## ⚡ Performance Rules

### [use-memo-appropriately] - Severity: Medium
Use `useMemo` for expensive computations that don't need to run on every render.
- **When to use:** Computation is expensive, the result must be referentially stable for child components, or inside Context Providers.
- **Note:** Memoization has overhead; profile before adding to verify actual performance benefits.

### [use-callback-for-stable-references] - Severity: Medium
Use `useCallback` for functions passed to memoized children or used in dependency arrays to prevent unnecessary re-renders.
- Do not wrap every function—only those causing re-render issues.
- Always include all used values in the dependency array.

### [implement-virtualization] - Severity: High
Use virtualization (e.g., `react-window`, `@tanstack/virtual`) for long lists (hundreds+ items).
- Only render visible items plus a small buffer to improve initial render time and memory usage.

### [avoid-inline-objects-and-functions] - Severity: Medium
Avoid creating new objects, arrays, or functions inline in JSX props. Each render creates new references, breaking `React.memo` comparisons.
- **Solution:** Move them to `useMemo`/`useCallback` or define them outside the component.

---

## 🏗️ State Management Rules

### [prefer-local-state] - Severity: Medium
Keep state as local as possible. Lift state only when multiple siblings need to share it. Use composition over context to avoid "prop drilling" where possible.

### [use-context-appropriately] - Severity: High
Use Context for **global, infrequently changing data** (e.g., theme, user session).
- Context changes re-render all consumers.
- For frequently changing data, use specialized state management libraries or split contexts to minimize re-render impact.

### [derive-state-dont-sync] - Severity: High
Derive computed values during render instead of syncing state with `useEffect`.
- **❌ BAD:** Storing a filtered list in state and updating it via `useEffect` when the source list changes.
- **✅ GOOD:** `const filteredList = useMemo(() => list.filter(...), [list, query]);`

---

## 🔄 Effects Rules

### [specify-effect-dependencies-correctly] - Severity: Critical
Include **all** values from component scope used in `useEffect` in the dependency array. Missing dependencies cause stale closure bugs.
- **Action:** Strictly follow the `exhaustive-deps` ESLint rule.

### [cleanup-effects-properly] - Severity: High
Always return a cleanup function from `useEffect` for subscriptions, timers, and event listeners.
- **Action:** Use `AbortController` to cancel pending async operations on unmount.

### [handle-effect-race-conditions] - Severity: High
Handle race conditions in async effects where an earlier request might complete after a later one.
- **Solution:** Use a cleanup boolean flag (e.g., `isCancelled`) or libraries like **TanStack Query** that handle this natively.

---

## 🎨 Component Design Rules

### [prefer-composition-over-inheritance] - Severity: Medium
Compose smaller, focused components into larger ones using `props.children` or render props. Extract shared UI logic into custom hooks rather than using class inheritance.

### [keep-components-focused] - Severity: Medium
Components should follow the Single Responsibility Principle. Extract subcomponents when a component grows too large or mixes container (logic) and presentational (UI) concerns.

---

## ⚓ Hooks Rules

### [follow-rules-of-hooks] - Severity: Critical
- Only call hooks at the **top level**.
- Never call them inside loops, conditions, or nested functions.
- Only call them from React functions.

### [extract-custom-hooks] - Severity: Medium
Extract reusable logic into custom hooks (starting with `use`). Hooks are the primary mechanism for code reuse in React.

---

## 🚩 Common Pitfalls

### [avoid-stale-closures] - Severity: High
Ensure closures (in effects or callbacks) capture the latest state by including all dependencies. Use **functional updates** for state:
- `setState(prev => prev + 1)` instead of `setState(count + 1)`.

### [prevent-infinite-loops] - Severity: High
Avoid unconditional `setState` inside `useEffect`. Ensure that objects or arrays used as effect dependencies have stable references (via `useMemo` or definition outside the component).
