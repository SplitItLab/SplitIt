# Development Guidelines

This is a Spring Boot Kotlin project.

The goal is to produce code that is **simple, maintainable, testable, and consistent with the existing architecture and conventions of the project**.

---

# General principles

## Understand before changing

Before modifying the codebase:

* Understand the existing architecture and relevant code paths.
* Search for existing implementations, abstractions, utilities, services, ports, exceptions, tests, and patterns.
* Prefer reusing existing code over creating new code.
* Follow existing project conventions unless there is a clear reason to change them.
* Avoid making unrelated changes.
* Keep changes focused on the requested task.

Do not refactor unrelated code unless it is necessary to correctly implement the requested change.

## Avoid overengineering

Prefer the simplest design that correctly solves the problem.

Do not introduce:

* unnecessary abstractions,
* unnecessary interfaces,
* unnecessary design patterns,
* unnecessary generic types,
* unnecessary layers,
* unnecessary configuration,
* speculative extensibility,
* duplicate models or services.

Every abstraction should have a clear purpose.

Do not introduce a pattern simply because it is considered a "best practice". Apply design principles according to the actual requirements of the project.

---

# Architecture

This project follows **Onion Architecture**.

Respect the existing dependency direction strictly.

The project is organized into:

```text
domain
application
infrastructure
```

## Domain

The `domain` layer contains the core business logic.

It must remain **pure and independent from infrastructure and frameworks**.

The domain must not depend on:

* Spring,
* Spring Boot,
* HTTP,
* controllers,
* persistence implementations,
* databases,
* configuration,
* security infrastructure,
* external APIs,
* framework-specific annotations.

Prefer immutable domain models.

Use:

* `val` instead of `var`,
* immutable collections,
* value objects,
* domain entities,
* domain services,
* explicit state transitions.

Avoid mutable state unless it is genuinely required by the domain.

Business rules belong in the domain whenever possible.

Do not move business rules into controllers, persistence classes, configuration, or infrastructure merely because they are easier to implement there.

## Application

The `application` layer contains application-specific behavior and use cases.

Structure:

```text
application/
├── port/
├── service/
└── exceptions/
```

### Application services

Application services should:

* represent application use cases,
* coordinate domain objects,
* orchestrate operations,
* interact with ports,
* handle application-level concerns.

Application services should **not become containers for business logic** that belongs in the domain.

### Ports

Use ports to define boundaries between the application/domain and external systems.

Ports should represent what the application needs rather than exposing infrastructure implementation details.

Infrastructure implementations should depend on application ports, not the other way around.

## Infrastructure

The `infrastructure` layer contains framework and external-system concerns.

Structure:

```text
infrastructure/
├── config/
├── api/
├── persistence/
└── security/
```

### API

Controllers should be thin.

Controllers should primarily:

* receive HTTP requests,
* validate/map input,
* invoke application services,
* map application results to HTTP responses.

Do not place business logic inside controllers.

### Persistence

Persistence code belongs in `infrastructure.persistence`.

Do not leak persistence entities or framework-specific persistence concerns into the domain unless there is an explicit architectural reason to do so.

Keep database concerns isolated from business logic.

### Security

Authentication, authorization, Spring Security configuration, JWT handling, filters, and related concerns belong in `infrastructure.security`.

Do not couple domain logic to Spring Security.

### Configuration

Spring beans and framework configuration belong in `infrastructure.config`.

Avoid spreading configuration logic throughout the application.

---

# Domain services vs Application services

Use a **domain service** when:

* the operation represents business/domain logic,
* the logic does not naturally belong to a single entity or value object,
* the operation is independent from application orchestration,
* the logic should remain framework-independent.

Use an **application service** when:

* implementing an application use case,
* coordinating multiple domain objects,
* interacting with ports,
* orchestrating domain operations,
* handling application-level workflows.

A useful rule:

> Domain services decide **what is valid according to the business**.
> Application services decide **how a use case is executed**.

Do not put domain business rules into application services just because the application service is convenient.

---

# Immutability

Prefer immutable code throughout the project, especially in the domain.

Prefer:

```kotlin
val
```

over:

```kotlin
var
```

Prefer immutable collections.

Avoid exposing mutable collections from domain objects.

Prefer explicit methods representing state transitions over directly mutating fields.

Do not use mutable state as a shortcut when an immutable design is reasonably simple.

---

# Kotlin conventions

Follow idiomatic Kotlin.

Prefer:

* `val` over `var`,
* expression-oriented code when it improves readability,
* nullable types instead of sentinel values,
* sealed types when modeling a closed set of states,
* value classes/value objects when appropriate,
* small focused functions,
* meaningful names,
* explicit types where they improve readability or prevent ambiguity.

Do not use Kotlin features merely for cleverness.

Code should remain readable to a developer familiar with standard Kotlin.

Avoid deeply nested scope functions when they make control flow harder to understand.

Prefer clear code over overly compact code.

---

# Nullability

Handle nullability explicitly.

Do not:

* use `!!` unless there is a strong and justified reason,
* silently convert missing values into arbitrary defaults,
* use nullable values to represent invalid domain states when a stronger model is possible.

Prefer making invalid states unrepresentable when practical.

---

# Exceptions

Every exception introduced by a change must have an appropriate handling strategy.

When creating an exception:

1. Determine what layer owns the failure.
2. Determine where the exception should be handled.
3. Map it appropriately at the application/API boundary when necessary.
4. Ensure it does not accidentally become an unexpected `500 Internal Server Error`.

Reuse existing exception types and handlers when they already represent the same failure.

Do not create duplicate exceptions for equivalent situations.

Exceptions should represent meaningful failure cases.

Do not use exceptions for normal control flow.

## Exception handling

Do not catch an exception unless there is a reason to handle, transform, log, or recover from it.

Avoid:

```kotlin
try {
    ...
} catch (e: Exception) {
    ...
}
```

when the exception cannot be meaningfully handled.

Never silently swallow exceptions.

Do not log and rethrow exceptions unnecessarily, as this can result in duplicate logs.

---

# API design

Keep HTTP concerns inside the API/infrastructure layer.

Do not expose domain or persistence implementation details unnecessarily through API responses.

Use appropriate HTTP status codes.

Request/response DTOs should be used when they provide a clear boundary between the API and the application/domain.

Do not blindly reuse persistence entities as API DTOs.

Validate external input at the appropriate boundary.

Do not rely exclusively on database constraints to validate application-level business rules.

---

# Validation

Distinguish between:

* input validation,
* domain invariants,
* application rules,
* persistence constraints.

Input validation belongs near the API boundary.

Business invariants belong in the domain.

Application-level constraints belong in the application layer.

Database constraints belong in persistence.

Do not duplicate the same rule across multiple layers unless there is a concrete reason.

---

# Dependency injection

Prefer constructor injection.

Avoid field injection.

Dependencies should be explicit.

Do not use service locators or static access to dependencies when normal dependency injection is sufficient.

Keep dependency graphs simple.

---

# Persistence

Keep persistence concerns isolated from the domain.

Do not make domain objects depend on repositories or database implementations.

Repositories exposed through application/domain ports should express business/application needs rather than database implementation details.

Avoid leaking:

* JPA/Hibernate entities,
* SQL-specific concepts,
* database IDs,
* persistence annotations,
* ORM behavior

into the domain unless the existing architecture explicitly requires it.

Do not introduce a repository abstraction if an existing appropriate port already exists.

---

# Testing

**Always write and run tests for changes.**

A change without appropriate tests is incomplete.

Before implementing a change, inspect existing tests to understand:

* testing conventions,
* naming conventions,
* fixtures,
* test utilities,
* mocking strategy,
* integration test strategy.

Prefer testing behavior rather than implementation details.

## What to test

Tests should cover:

* normal behavior,
* relevant edge cases,
* invalid input,
* business rules,
* expected exceptions,
* important integration boundaries.

When changing a bug, add a regression test that would have failed before the fix.

Do not remove or weaken tests simply to make the implementation pass.

Prefer deterministic tests.

Avoid unnecessary mocking.

Test domain logic independently from infrastructure whenever possible.

## Test pyramid

Prefer:

1. unit tests for domain logic,
2. application service tests,
3. integration tests for infrastructure boundaries,
4. API tests for important HTTP behavior.

Do not use an integration test when a simple unit test provides the same confidence.

---

# Reuse existing code

Before creating anything new, search the repository.

Look for:

* existing services,
* existing ports,
* existing repositories,
* existing value objects,
* existing domain entities,
* existing exceptions,
* existing mappers,
* existing validators,
* existing test fixtures,
* existing utilities,
* existing configuration.

Prefer reusing or extending existing functionality.

Do not create a second implementation of something that already exists.

If an existing implementation is almost correct, consider whether it should be extended rather than duplicated.

---

# Dependencies and libraries

**Do not add new libraries or dependencies without explicit approval.**

If a task appears to require a new dependency:

1. Stop before modifying the dependency configuration.
2. Explain why the dependency is needed.
3. Ask for approval.
4. Only add it after approval.

Prefer existing project dependencies and standard library functionality.

Do not replace an existing library with another library without approval.

Do not update dependency versions unless explicitly requested or necessary for the task.

---

# Build configuration

Avoid unnecessary changes to:

* `build.gradle.kts`,
* Gradle configuration,
* plugins,
* dependency versions,
* application configuration,
* environment configuration.

Only modify build/configuration files when required by the task.

Do not add plugins or Gradle tasks without approval unless they are strictly necessary and already part of the project's established tooling.

---

# Configuration and environment

Do not hardcode:

* credentials,
* API keys,
* secrets,
* environment-specific URLs,
* passwords,
* tokens.

Use the project's existing configuration mechanism.

Do not commit secrets.

Do not change environment configuration unless required by the task.

---

# Logging

Use logging appropriately.

Do not log:

* passwords,
* tokens,
* credentials,
* secrets,
* sensitive user data.

Avoid excessive logging.

Do not use logging as a substitute for proper error handling.

Use appropriate log levels.

---

# Code quality

Prefer:

* small classes,
* small functions,
* meaningful names,
* explicit responsibilities,
* low coupling,
* high cohesion,
* dependency inversion,
* composition over unnecessary inheritance.

Avoid:

* god classes,
* god services,
* huge controllers,
* large conditional blocks,
* duplicated business logic,
* hidden side effects,
* unnecessary mutable state.

Do not optimize prematurely.

Prefer clarity and correctness first.

---

# Refactoring

Refactor only when:

* required for the requested change,
* necessary to preserve architectural boundaries,
* necessary to remove duplication introduced by the change,
* or explicitly requested.

Avoid unrelated refactors.

If a larger refactor is clearly necessary, explain the reason and keep it separate from unrelated improvements.

Do not mix a feature implementation with broad codebase cleanup unless necessary.

---

# Formatting

After modifying Kotlin code, run:

```bash
./gradlew ktlintFormat
```

Do not manually reformat code when ktlint can handle it.

---

# Static analysis

Before finishing a task, run:

```bash
./gradlew ktlintCheck
./gradlew detekt
```

Fix all relevant violations introduced by the changes.

Do not disable, suppress, or weaken static-analysis rules merely to make the build pass unless there is a strong justification.

If a suppression is genuinely necessary, keep it as narrow as possible and explain why.

---

# Tests and verification

After implementing a change:

1. Run the relevant tests.
2. Run formatting.
3. Run static analysis.
4. Fix failures introduced by the change.
5. Re-run the affected checks.

For changes affecting the whole application, run:

```bash
./gradlew test
```

Do not consider a task complete if relevant tests fail.

A task is complete only when:

* the requested behavior is implemented,
* existing functionality has not been unnecessarily broken,
* appropriate tests have been added or updated,
* relevant tests pass,
* formatting passes,
* `ktlintCheck` passes,
* `detekt` passes.

---

# Git and scope

Keep changes focused.

Do not:

* modify unrelated files,
* rewrite unrelated code,
* remove existing functionality without a reason,
* change public APIs unnecessarily,
* rename large portions of the project without being asked.

Do not create commits unless explicitly requested.

Do not modify `.gitignore`, CI configuration, deployment configuration, or repository metadata unless required by the task.

---

# Working with existing conventions

The existing codebase is the source of truth for conventions not explicitly defined here.

When multiple reasonable approaches exist:

1. Prefer the approach already used in the project.
2. Prefer the simplest approach.
3. Prefer the approach that preserves the existing architecture.
4. Prefer the approach that is easiest to test and maintain.

Do not introduce a new convention without a concrete reason.

---

# Before finishing any task

Verify all of the following:

* [ ] Existing code was searched and reused where appropriate.
* [ ] The Onion Architecture is respected.
* [ ] Domain code remains framework-independent.
* [ ] Domain code is immutable where practical.
* [ ] Business rules are in the domain.
* [ ] Domain services are used when appropriate.
* [ ] Application services are used for application orchestration.
* [ ] Controllers remain thin.
* [ ] Persistence concerns remain in infrastructure.
* [ ] Security concerns remain in infrastructure.
* [ ] Exceptions have an appropriate handling strategy.
* [ ] No exceptions are silently swallowed.
* [ ] No unnecessary abstractions were introduced.
* [ ] No duplicated functionality was introduced.
* [ ] No unrelated refactoring was performed.
* [ ] No new dependency was added without explicit approval.
* [ ] Appropriate tests were added or updated.
* [ ] Relevant tests pass.
* [ ] `ktlintFormat` was run.
* [ ] `ktlintCheck` passes.
* [ ] `detekt` passes.
* [ ] No secrets or credentials were introduced.
* [ ] The final change is focused on the requested task.
