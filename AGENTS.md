# AGENTS.md — shared guidance for AI tool sessions

This file is read by opencode and similar coding agents at session start so they
behave consistently with the project's conventions and environment.

## Environment (Windows 11 host)

The host has the required toolchain installed, but opencode shells launch with a
**stale PATH**. Before invoking `java`, `mvn`, or `docker` in a `bash` block, prefix:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'
$env:PATH = "$env:JAVA_HOME\bin;C:\Maven\apache-maven-3.9.16\bin;C:\Program Files\Docker\Docker\resources\bin;$env:PATH"
```

Docker Desktop (the daemon) must be **running** for `docker compose up` or
Testcontainers to work — verify with `docker info`.

## Build / test commands

- **Prefer the wrapper**: `./mvnw clean verify` (Linux/macOS) or `.\mvnw.cmd clean verify` (Windows).
- **Single module**: `./mvnw -pl modules/order -am test` (`-am` also builds shared-kernel deps).
- **Architecture tests only**: `./mvnw -Dtest='*ArchTest' test`.
- **Skip tests for a fast package**: `./mvnw -DskipTests package`.

Integration tests use **Testcontainers** (PostgreSQL 17, Redis 7, RabbitMQ 3.13)
via `@Testcontainers` + `@ServiceConnection` — the Docker daemon must be running.

## Architecture invariants (DO NOT VIOLATE)

1. **Hexagonal / Ports &amp; Adapters per bounded context.** Inside any
   `modules/&lt;context&gt;/src/main/java/com/engine/&lt;context&gt;/` the package
   layout is fixed:
   - `domain/` — entities, value objects, domain services, ports (in/out). Pure Java.
   - `application/` — commands, queries, orchestration. Depends only on `domain`.
   - `adapters/in/` — REST controllers, `@RabbitListener` consumers.
   - `adapters/out/` — JPA, AMQP producer, external HTTP clients (WebClient + Resilience4j).
2. **`domain` MUST NOT import** Spring, JPA, Jackson, or any `adapters.*` package.
   ArchUnit tests enforce this — keep them green.
3. **Shared Kernel (`shared-kernel`)** is the only module any bounded context may
   depend on. Bounded contexts NEVER import from each other; cross-context
   communication is via domain events published to RabbitMQ.
4. **Mutating financial endpoints** require an `Idempotency-Key` header.
5. **Synchronous publication to RabbitMQ from a DB transaction is FORBIDDEN** —
   write to the outbox table in-transaction; dispatch asynchronously. (Transactional Outbox Pattern.)
6. **DB schema changes are forward-only Flyway migrations** in
   `bootstrap/src/main/resources/db/migration/`. Never edit a migration that's shipped.

## Conventions

- **No Lombok** — use Java 21 records for DTOs/value objects; explicit constructors for entities.
- **No `@Autowired` field injection** — use constructor injection (Spring auto-wires single-constructor beans).
- **No comments** unless a non-obvious invariant is being preserved. Self-documenting names first.
- **Commit messages** follow Conventional Commits, e.g.
  `feat(payment): implement gateway client with Resilience4j circuit breaker`.
  Available scopes: `kernel`, `identity`, `order`, `payment`, `bootstrap`, `build`,
  `ci`, `docs`, `test`, `ops`.
- **Verification gate before any commit suggestion**: run `./mvnw clean verify` (or
  at minimum `./mvnw -DskipTests compile` if Testcontainers can't run) and include
  the result in the change message.

## When in doubt

- Prefer the explicit over the clever.
- Prefer one more type over a `Map&lt;String,Object&gt;`.
- Prefer failing the build over a silent warning.