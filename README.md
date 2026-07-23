# Distributed Event-Driven Payment & Order Processing Engine

> A production-grade backend showcase built with **Java 21** and **Spring Boot 3**, demonstrating how to solve the hard problems of a modern e-commerce / fintech platform: high traffic spikes, asynchronous transaction processing, partial network failures, concurrent data modifications, and strict consistency requirements under eventual consistency.

[![CI](https://github.com/Kaksi3118/java-payment-order-engine/actions/workflows/build.yml/badge.svg)](https://github.com/Kaksi3118/java-payment-order-engine/actions)
[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.1-green?logo=springboot)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.9.x-blue?logo=apachemaven)](https://maven.apache.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](./LICENSE)

---

## 💡 Why this project?

Most portfolio backends are simple CRUD applications with a thin REST layer over JPA. This project intentionally reaches for the exact architectural patterns used in production payment systems at scale, so that the hard parts — and how to reason about them — are visible in the code:

| Production Challenge | How This Project Solves It |
| --- | --- |
| **Reliable event delivery without 2PC** | **Transactional Outbox Pattern** — DB writes and event publication succeed or fail atomically; a background poller securely drains the outbox to RabbitMQ. |
| **Duplicate processing on retries** | **Idempotency-Key** header + persisted idempotency context for all state-changing financial operations (protects against double-charging). |
| **Concurrent inventory mutation** | Multi-layer concurrency control: **JPA optimistic locking** (`@Version`) + **Redis distributed locks** (Redisson) for critical cross-instance coordination. |
| **Decoupling heavy work** | **CQRS** + **asynchronous messaging via RabbitMQ** with explicit DLQ topologies and retry/backoff semantics. |
| **Flaky third-party APIs** | **Resilience4j**: circuit breaker, bulkhead, rate limiter, retry — wrapping the external payment gateway. |
| **High throughput on blocking I/O** | **Java 21 Virtual Threads** (`spring.threads.virtual.enabled=true`) — JDK-level carrier scheduling, zero reactive rewrite overhead. |
| **Boundary discipline at scale** | **Hexagonal Architecture** enforced by **ArchUnit** tests that fail the build if the `domain` layer ever imports Spring or JPA. |
| **Trustworthy integration tests** | **Testcontainers** automatically spins up **real** PostgreSQL, Redis, and RabbitMQ in Docker — zero flaky in-memory mocks. |

---

## 🏗️ Architecture

The application is structured as a **Modular Monolith** containing three distinct bounded contexts (`identity`, `order`, `payment`). Each context enforces strict **Hexagonal Architecture** (Ports and Adapters). Bounded contexts do not import from each other directly; they communicate entirely via domain events published to RabbitMQ.

### High-Level System Topology

```mermaid
flowchart LR
    Client([Client / API Consumer])

    subgraph App[Spring Boot Application — Modular Monolith]
        direction TB
        REST[REST Controllers<br/>adapters.in.rest]
        AMQP_IN[AMQP Consumers<br/>adapters.in.messaging]
        APP[Application Layer<br/>CQRS - Commands / Queries / Outbox]
        DOMAIN[(Domain Layer<br/>pure Java - entities, ports)]
        OUT[Outbound Adapters<br/>JPA - WebClient - AMQP Producer]
        OUTBOX[(Outbox Table)]

        REST --> APP
        AMQP_IN --> APP
        APP --> DOMAIN
        APP --> OUTBOX
        OUT --> DOMAIN
        OUT --> OUTBOX
    end

    PG[(PostgreSQL 17)]
    Redis[(Redis 7<br/>cache + distributed lock)]
    RabbitMQ[(RabbitMQ 3.13<br/>+ DLQ)]
    Gateway[External Payment Gateway]

    Client -->|HTTPS / JWT| REST
    OUT -->|JPA / Flyway| PG
    OUT -->|Spring Data Redis / Redisson| Redis
    OUTBOX -->|Outbox poller| RabbitMQ
    RabbitMQ -->|events| AMQP_IN
    OUT -->|WebClient + Resilience4j| Gateway

    classDef store fill:#fef3c7,stroke:#d97706,color:#000
    classDef ext fill:#dbeafe,stroke:#2563eb,color:#000
    class PG,Redis,RabbitMQ,OUTBOX store
    class Gateway,Client ext
```

### Bounded Contexts
- **Identity**: Handles user registration, JWT authentication (RS256), and stateless role-based access control (RBAC).
- **Order**: Manages the e-commerce lifecycle (`CREATED → CONFIRMED → SHIPPED → CANCELLED`), integrating with a stubbed inventory system.
- **Payment**: Handles payment processing (`AUTHORIZE → CAPTURE → REFUND`) by integrating with an external payment gateway using `WebClient` wrapped in `Resilience4j` resilience patterns.

---

## 🛠️ Tech Stack

- **Language / Framework:** Java 21 (Records, Pattern Matching, Virtual Threads) + Spring Boot 3.4.1
- **Architecture:** Hexagonal Architecture (Ports & Adapters), CQRS, Domain-Driven Design (DDD)
- **Persistence:** PostgreSQL 17 + Flyway 10 + Spring Data JPA
- **Caching & Locks:** Redis 7 + Redisson
- **Messaging:** RabbitMQ 3.13 + Spring AMQP (Topic Exchanges + DLQ routing)
- **Resilience:** Resilience4j (Circuit Breaker, Retry, Rate Limiter, Bulkhead)
- **Security:** Spring Security + JWT (Stateless)
- **Testing:** JUnit 5, Mockito, AssertJ, Testcontainers, ArchUnit
- **Observability:** Micrometer + Prometheus + Grafana

---

## 🚀 Quick Start

### Prerequisites
- **Java 21** (Temurin or similar)
- **Docker** and Docker Compose (Docker daemon must be running)
- *Note: The project bundles Maven Wrapper (`./mvnw`), so a local Maven installation is not required.*

### 1. Start the Infrastructure
Spin up PostgreSQL, Redis, and RabbitMQ, alongside Prometheus and Grafana for metrics observability:
```bash
docker compose up -d
```
*RabbitMQ Management UI: http://localhost:15672 (login: `poe` / `poe_dev_password`)*  
*Grafana Dashboards: http://localhost:3000 (login: `admin` / `admin`)*  

### 2. Build & Run the Application
Run the full suite of unit and integration tests (which utilize Testcontainers) and start the Spring Boot application:
```bash
# Windows
.\mvnw.cmd clean verify
.\mvnw.cmd -pl bootstrap spring-boot:run

# Linux / macOS
./mvnw clean verify
./mvnw -pl bootstrap spring-boot:run
```
The API is now available at `http://localhost:8080`.

### 3. Stop the Infrastructure
```bash
# Tears down the containers and removes named volumes
docker compose down -v
```

---

## 📂 Repository Structure

The project uses a standard multi-module Maven reactor:
```text
java-payment-order-engine/
├── pom.xml                         # Parent reactor BOM
├── docker-compose.yml              # Core infrastructure dependencies
├── prometheus/ & grafana/          # Observability configuration
├── shared-kernel/                  # Primitives: Money, IDs, DomainEvents
├── modules/
│   ├── identity/                   # JWT Auth & Security
│   ├── order/                      # Orders, Outbox, CQRS
│   └── payment/                    # Payment Gateway, Resilience4j
└── bootstrap/                      # Spring Boot main, wiring, application.yml
```

Each bounded context rigorously follows a Hexagonal internal package structure:
```text
com.engine.<context>/
├── domain/                         # Pure Java: entities, value objects, ports
├── application/                    # Use Cases: commands, queries, orchestration
└── adapters/
    ├── in/                         # Driving: REST controllers, RabbitMQ listeners
    └── out/                        # Driven: JPA, WebClient, AMQP publisher
```

---

## 📝 License

[MIT](./LICENSE) — feel free to use this as a reference or learning resource!