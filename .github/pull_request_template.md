## Summary

<!-- Brief description of the change and the problem it solves. -->

## Motivation

<!-- Why is this change needed? Link any issue, ADR, or design note. -->

## Changes

- [ ] Domain model
- [ ] Application layer (use case / command / query)
- [ ] Adapter (REST / persistence / messaging / external)
- [ ] Migration (Flyway)
- [ ] Tests (unit / integration with Testcontainers)
- [ ] ArchUnit rule added or maintained
- [ ] Documentation / README updated

## Architecture & Design Decisions

<!-- Which pattern is in play (Outbox, Idempotency, CQRS, Resilience4j)? Why this approach? -->

## Checklist

- [ ] Code follows SOLID / DRY / YAGNI / Clean Code.
- [ ] No framework leakage into `domain` packages (verified by `mvn test -Dtest=*ArchTest`).
- [ ] Public mutating endpoints accept an `Idempotency-Key` header where applicable.
- [ ] New DB schema is covered by a forward-only Flyway migration (no `flyway:clean` in prod).
- [ ] Integration tests use Testcontainers; no in-memory mocks for real infra.
- [ ] Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/).
- [ ] All checks pass locally: `./mvnw clean verify`.

## Test Evidence

<!-- Paste the relevant `./mvnw verify` excerpt (Testcontainers startup, test counts, build SUCCESS). -->