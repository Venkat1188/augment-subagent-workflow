# REST API Code Review Guidelines
### Standards for HTTP methods, resource naming, status codes, and HATEOAS

**Description:** Guidelines for designing and reviewing RESTful APIs to ensure consistent resource-based architecture and proper HTTP semantics.  
**Applicable Files:** `**/*controller*`, `**/*handler*`, `**/*route*`, `**/*api*`, `**/openapi*.yaml`, `**/swagger*.yaml`.

---

## 🛠️ HTTP Methods & Semantics

### [use-correct-http-verbs] - Severity: High
Utilize HTTP methods according to their functional purpose:
- **GET:** Retrieve resources. Must be **safe** (no side effects) and **idempotent**.
- **POST:** Create resources or perform non-idempotent actions.
- **PUT:** Replace an entire resource. Must be **idempotent**.
- **PATCH:** Partial updates to a resource.
- **DELETE:** Remove a resource. Must be **idempotent**.

### [ensure-get-idempotency] - Severity: Critical
**Never** use GET to modify server state, create resources, or trigger business logic (e.g., `/api/v1/delete-user?id=123` is a violation). Side effects in GET violate the contract of the web and break caching.

---

## 🗺️ URL & Resource Design

### [use-nouns-for-resources] - Severity: High
Use **plural nouns** for collections and avoid verbs in the URL path.
- **❌ BAD:** `POST /api/v1/getUsers` or `POST /api/v1/createUser`
- **✅ GOOD:** `GET /api/v1/users` or `POST /api/v1/users`

### [maintain-resource-hierarchy] - Severity: Medium
Express relationships through path nesting but limit to **2-3 levels**.
- **Example:** `/users/{id}/orders/{orderId}`.
- For deeper relationships, use query parameters: `/items?category=electronics&sub=phones`.

### [use-kebab-case-in-urls] - Severity: Low
Use lowercase letters and hyphens for multi-word resource names.
- **✅ GOOD:** `/user-profiles`, `/shipping-address`.

---

## 🚦 Status Codes & Error Handling

### [use-appropriate-status-codes] - Severity: High
- **201 Created:** Use for successful POSTs that create a resource. Include a `Location` header.
- **204 No Content:** Use for successful DELETEs or updates where no body is returned.
- **401 vs 403:** Use **401 Unauthorized** for authentication failures and **403 Forbidden** for permission/authorization failures.
- **422 Unprocessable Entity:** Prefer for validation errors over 400.

### [consistent-error-format] - Severity: High
All error responses must follow a unified JSON structure including a machine-readable `code` and a human-readable `message`.

---

## 📦 Request & Response Patterns

### [implement-pagination-filtering-sorting] - Severity: High
Endpoints returning collections **must** implement pagination.
- **Pagination:** Use `limit`/`offset` or `cursor`.
- **Filtering:** Use query parameters: `?status=active`.
- **Dates:** Always use **ISO 8601** format in UTC.

### [support-content-negotiation] - Severity: Medium
Respect the `Accept` and `Content-Type` headers. Default to `application/json`. Return `415 Unsupported Media Type` if the client sends a format the API cannot parse.

---

## 🔗 HATEOAS & Discoverability

### [include-hypermedia-links] - Severity: Low
Where possible, include a `links` or `_links` object in the response to guide the client to related actions (e.g., `self`, `next`, `prev`). This reduces hardcoding of URLs in the frontend.

---

## 📖 API Documentation

### [maintain-openapi-specification] - Severity: High
Every API must have a corresponding **OpenAPI (Swagger)** specification that is kept in sync with the implementation. Use tools like `springdoc-openapi` for Java to automate this.
