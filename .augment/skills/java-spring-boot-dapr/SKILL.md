---
name: java-spring-boot-dapr
description: Expert guide for building microservices using Java 26, Spring Boot 4.0.5, and Dapr 1.17.1. Use this skill when the user is developing distributed systems, implementing Dapr building blocks (state, pub/sub, service invocation), or upgrading Spring Boot modules.
---

# Production Java Spring Boot & Dapr Engineering

## Metadata
- **Name:** java-spring-boot-dapr-expert
- **Context:** Java 26 (Standard), Spring Boot 4.0.5, Dapr SDK 1.17.1+
- **Goal:** Build high-performance, resilient microservices utilizing Virtual Threads and Dapr Building Blocks.

## 1. Environment & Stack Standards
- **Runtime:** Java 26 (Enable `-XX:+UseCompactObjectHeaders`).
- **Framework:** Spring Boot 4.0.5 (Jakarta EE 11 / Spring Framework 7).
- **Sidecar:** Dapr 1.17.x (Communication via gRPC on port 50001 preferred).
- **Threading Model:** Project Loom (Virtual Threads) enabled.

## 2. Mandatory Configuration Patterns

### 2.1 Spring Boot `application.yml`
Always ensure these production baselines are set:
 
    yaml
        spring:
          threads:
            virtual:
              enabled: true # Leverage Java 26 Virtual Threads
          application:
            name: ${APP_NAME}

    dapr:
      client:
        grpc:
          endpoint: "localhost:50001" # Production standard over HTTP

### 2.2 Maven pom.xml (Modular Approach)
Avoid monolithic starters. Use granular dependencies:

    <dependencies>
        <dependency>
            <groupId>io.dapr</groupId>
            <artifactId>dapr-sdk-springboot</artifactId>
            <version>1.17.1</version>
        </dependency>
        <!-- Use specific Spring Boot 4 modular starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web-minimal</artifactId>
        </dependency>
    </dependencies>

### 3. Coding Patterns & Principles
### 3.1 Dapr-First Architecture
   Service Invocation: Do NOT use RestTemplate or Feign. Use daprClient.invokeMethod().
   State Store: Use ETag-based concurrency for updates to prevent race conditions.
   Pub/Sub: Use @Topic with CloudEvent<T> envelopes. Handle idempotency at the service layer. 

### 3.2 Reactive Operations
   Always return Mono or Flux when interacting with the DaprClient to maintain non-blocking execution:

       public Mono<String> processOrder(Order order) {
       return daprClient.saveState(STORE_NAME, order.getId(), order)
       .then(daprClient.publishEvent(PUB_SUB, TOPIC, order))
       .thenReturn(order.getId());
       }
### 4. Automation & Validation
### 4.1 Built-in Dapr Config Validator
    The agent should use this logic to validate any generated Dapr component YAMLs (StateStore, PubSub):

      # validate_dapr.py logic (to be executed by agent)
        import yaml
        def validate(file_path):
        required = ["apiVersion", "kind", "metadata", "spec"]
        with open(file_path, 'r') as f:
        data = yaml.safe_load(f)
        if data.get("kind") == "Component":
        missing = [r for r in required if r not in data]
        if missing: raise ValueError(f"Missing fields: {missing}")
### 5. Production Checklist for Agents
   Resiliency: Offload retries/timeouts to Dapr resiliency.yaml. Do not hardcode in Java.
   Observability: Ensure io.micrometer:micrometer-tracing is present for W3C trace propagation.
   Security: Reference secrets via {{secretScope.secretKey}}. Never use plain-text secrets in YAML.
   Startup: Use dapr run --wait-for-sidecar logic in bootstrap scripts.
### 6. Common Commands Reference
   Run Locally: dapr run --app-id my-app --app-port 8080 --resources-path ./components -- java -jar target/*.jar
   Unit Testing: Use io.dapr.client.DaprClient mocks; do not require a running sidecar for unit tests.



