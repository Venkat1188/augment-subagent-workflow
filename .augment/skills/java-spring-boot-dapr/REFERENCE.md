### REFERENCE.md (Level 3 Resource)
```markdown
# Technical Reference

## Spring Boot 4 Modular Starters
Spring Boot 4.x has moved to a more granular dependency model. Instead of `spring-boot-starter-web`, consider if you only need `spring-boot-starter-web-minimal` for Dapr-only services.

## Java 26 Features
- **Compact Object Headers:** Benefit from reduced heap usage by ensuring `-XX:+UseCompactObjectHeaders` is active (default in Java 26).
- **ZGC Improvements:** No manual tuning required for microservices under 2GB RAM.

## Dapr Component Schema
Ensure all component files in `/components` use `apiVersion: dapr.io/v1alpha1`.