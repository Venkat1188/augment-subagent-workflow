# MCP Integration Code Review Guidelines
### Standards for linking code changes to external requirements via Model Context Protocol

**Description:** Guidelines that leverage MCP to fetch external context from issue trackers (Linear, Jira) to ensure code implementation aligns with ticket requirements.  
**Applicable Files:** All pull requests, branches, and code changes referencing external tickets.

---

## 🏗️ Requirements & Ticket Verification

### [verify-ticket-requirements] - Severity: High
When a PR references a **Linear** or **Jira** ticket:
- **Action:** Use MCP to fetch the ticket description and acceptance criteria.
- **Verification:** Ensure the code implementation explicitly covers the expected behavior described in the ticket. Flag any discrepancies where the code deviates from the user story.

### [check-ticket-scope] - Severity: Medium
Compare PR diffs against the ticket scope.
- **Audit:** Flag "Scope Creep" where a PR includes changes unrelated to the linked ticket.
- **Completeness:** Ensure all subtasks or checklist items in the ticket are addressed in the code.

---

## 🕵️ Contextual Analysis

### [check-ticket-comments-for-decisions] - Severity: Low
Fetch and review comments within the linked ticket via MCP.
- **Goal:** Understand architectural decisions or implementation changes decided during the ticket's lifecycle. Flag if the code contradicts decisions recorded in ticket discussions.

### [verify-edge-cases-from-ticket] - Severity: High
Review any specific edge cases or "Gotchas" mentioned in the ticket description.
- **Action:** Verify that these scenarios are handled in the logic and covered by corresponding **Integration or Unit Tests**.

---

## 📊 Process & Traceability

### [flag-missing-ticket-reference] - Severity: Low
All production code changes must be traceable to a project management entity.
- **Rule:** If a PR title, description, or branch name lacks a ticket reference (e.g., `ENG-123` or `PROJ-456`), flag it for the author to update.

### [check-ticket-type-validation] - Severity: Medium
- **Bugs:** If the ticket type is "Bug," verify the presence of a **regression test**.
- **Features:** If the ticket is a "Feature," verify that user-facing changes include **documentation updates** and proper **API versioning**.

---

## 🛠️ Usage Requirements
**Note:** These rules only function if the corresponding MCP servers are configured in your environment.
- **Linear:** Requires Linear MCP server.
- **Jira:** Requires Jira MCP server.
- **Setup:** Reference the [Augment MCP Documentation](https://docs.augmentcode.com/) for server configuration.
