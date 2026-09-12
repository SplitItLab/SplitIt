# Development Guidelines

This is a monorepo containing multiple applications.

```text
/
├── api/
│   └── AGENTS.md
└── web/
    └── AGENTS.md
```

The root `AGENTS.md` defines rules that apply to the **entire repository**.

When working inside a specific application, the application's own `AGENTS.md` contains additional and more specific rules.

---

# Mandatory instructions for subprojects

Before modifying files inside a subproject, **read and follow its `AGENTS.md`**.

## API

When modifying anything under:

```text
/api
```

read and follow:

```text
/api/AGENTS.md
```

The API-specific `AGENTS.md` is authoritative for Spring Boot, Kotlin, Onion Architecture, domain/application/infrastructure boundaries, testing, persistence, security, and other API-specific conventions.

## Web

When modifying anything under:

```text
/web
```

read and follow:

```text
/web/AGENTS.md
```

The Web-specific `AGENTS.md` is authoritative for Next.js, React, TypeScript, frontend architecture, components, state management, styling, testing, and other web-specific conventions.

## Fullstack changes

For changes involving both `/api` and `/web`:

1. Read `/api/AGENTS.md`.
2. Read `/web/AGENTS.md`.
3. Follow both files.
4. Apply the root architectural and design principles to the entire change.
5. Keep the API and web responsibilities clearly separated.
6. Do not duplicate business rules between the API and web unnecessarily.

If a subproject-specific rule conflicts with a root rule, follow the more specific subproject rule for that subproject unless doing so would violate a root-level architectural or design principle.

---

# General engineering principles

These principles apply to **every part of the repository**, regardless of language or framework.

## Understand before changing

Before modifying code:

* Inspect the relevant existing implementation.
* Search for existing abstractions, utilities, services, components, types, tests, and patterns.
* Understand how the existing code works before introducing a new solution.
* Follow established project conventions.
* Prefer modifying or extending existing functionality over creating duplicates.
* Keep changes focused on the requested task.

Do not assume that a new implementation is necessary just because it is easier to write from scratch.

---

# Reuse existing code

**Always search before creating.**

Before introducing a new:

* class,
* function,
* service,
* interface,
* component,
* hook,
* utility,
* type,
* exception,
* validator,
* mapper,
* repository,
* API client,
* test utility,

search the codebase for existing functionality that can be reused.

Prefer:

```text
reuse existing code
        ↓
extend existing code
        ↓
refactor existing code if necessary
        ↓
create new code
```

Avoid duplicate implementations of the same concept.

Do not create abstractions simply because they might be useful in the future.

---

# Design principles

The entire repository should follow strong software design principles.

These principles apply to **both backend and frontend**, regardless of technology.

## Single Responsibility

Every module, class, function, component, and service should have a clear responsibility.

Avoid large classes/components/functions that handle unrelated concerns.

## High cohesion

Keep related behavior together.

A concept should ideally have one clear place where its behavior is defined.

## Low coupling

Minimize unnecessary dependencies between unrelated parts of the system.

Prefer explicit dependencies and well-defined boundaries.

## Dependency inversion

High-level business/application logic should not depend directly on low-level implementation details when an appropriate abstraction provides a meaningful boundary.

Do not introduce interfaces purely for the sake of applying dependency inversion. The abstraction must provide real architectural value.

## Separation of concerns

Keep different concerns separate.

Examples:

```text
Business rules
Application/use-case orchestration
API/HTTP
Persistence
Security
External services
UI
Presentation
Client-side state
```

Do not mix them simply because doing so is convenient.

## Composition over unnecessary inheritance

Prefer composition when it produces a simpler and more maintainable design.

Avoid inheritance hierarchies unless there is a genuine "is-a" relationship.

## Encapsulation

Keep implementation details private.

Expose the smallest useful API.

Do not expose mutable internal state unnecessarily.

## Immutability

Prefer immutable data and explicit state transitions.

This applies to:

* backend domain models,
* application state,
* frontend state,
* DTOs,
* configuration,
* shared data structures.

Prefer immutable values over mutable shared state whenever practical.

---

# Business logic

Business rules should have a **single authoritative location**.

For backend business rules, follow the architecture defined in `/api/AGENTS.md`.

The frontend may perform validation for user experience, but it must not become the authoritative source of business rules when those rules belong to the backend.

For example:

```text
Frontend validation
        ↓
better UX

Backend/domain validation
        ↓
actual business invariant
```

Do not duplicate complex business logic between `/web` and `/api`.

If the same business rule must be represented in both places, keep the backend authoritative.

---

# API ↔ Web boundaries

Keep the boundary between `/api` and `/web` explicit.

The web application should communicate with the API through clearly defined API contracts.

Do not:

* import backend code directly into the web application,
* duplicate backend implementation details in the frontend,
* couple frontend components directly to database concepts,
* expose persistence implementation details as frontend concepts.

When an API contract changes:

* inspect both `/api` and `/web`,
* update all affected consumers,
* update relevant tests,
* keep the change backwards-compatible when practical.

Prefer clear DTO/API contracts over leaking internal domain or persistence models.

---

# Fullstack changes

When implementing a feature that spans the backend and frontend, think about the feature as a complete flow:

```text
User interaction
      ↓
Web UI
      ↓
API request
      ↓
Application use case
      ↓
Domain logic
      ↓
Infrastructure / persistence
      ↓
API response
      ↓
Web state
      ↓
UI
```

Before coding, identify:

* the user-facing behavior,
* the API contract,
* the application use case,
* the domain rules,
* the persistence requirements,
* the frontend state requirements,
* the error cases,
* the tests required at each relevant layer.

Do not implement only the visible UI while ignoring the backend contract, or vice versa.

---

# Keep changes focused

Do not make unrelated changes while implementing a feature.

Avoid:

* unrelated refactors,
* broad renaming,
* formatting unrelated files,
* dependency upgrades,
* framework migrations,
* architecture changes,
* cleanup unrelated to the task.

If an unrelated issue is discovered:

* do not silently fix it,
* mention it separately,
* only fix it if it is required for the requested change or explicitly requested.

---

# Avoid overengineering

Prefer the simplest solution that correctly satisfies the requirements.

Do not introduce:

* unnecessary abstractions,
* unnecessary layers,
* unnecessary interfaces,
* unnecessary patterns,
* unnecessary generic code,
* speculative extensibility,
* premature optimization.

Do not build infrastructure for hypothetical future requirements.

Good architecture is not the same as maximum abstraction.

---

# Dependencies

**Never add a new dependency without explicit user approval.**

This applies to:

* backend dependencies,
* frontend dependencies,
* Gradle dependencies,
* npm/pnpm packages,
* plugins,
* build tools,
* testing libraries,
* UI libraries.

If a new dependency appears necessary:

1. Stop before adding it.
2. Explain why it is needed.
3. Ask for approval.
4. Only add it after approval.

Prefer existing dependencies and platform capabilities.

Do not replace an existing dependency with another one without approval.

Do not update dependency versions unless explicitly requested or required to complete the task.

---

# Configuration and secrets

Never hardcode:

* passwords,
* API keys,
* authentication tokens,
* private keys,
* credentials,
* secrets.

Use the existing environment/configuration mechanisms.

Never commit secrets.

Do not modify environment configuration unnecessarily.

Do not expose secrets to the frontend.

Remember that anything shipped to the browser should be considered publicly observable.

---

# Error handling

Errors should be handled intentionally.

Do not:

* silently swallow errors,
* catch errors without handling them,
* hide failures with arbitrary fallback values,
* expose internal implementation details to users,
* return generic errors when a meaningful error can be provided.

Backend exception handling must follow `/api/AGENTS.md`.

Frontend error handling must follow `/web/AGENTS.md`.

When changing an API error contract, inspect both sides of the API boundary.

---

# Validation

Validation should happen at the appropriate boundary.

Distinguish between:

* user/input validation,
* domain invariants,
* application rules,
* persistence constraints.

Do not rely on frontend validation for backend correctness.

Do not duplicate complex validation logic unnecessarily.

The backend remains authoritative for business invariants.

---

# Testing

**Always write and run tests for changes.**

A feature or bug fix without appropriate tests is incomplete.

Before writing tests, inspect existing tests and reuse:

* test utilities,
* fixtures,
* factories,
* mocks,
* naming conventions,
* testing patterns.

Prefer testing behavior rather than implementation details.

## Bug fixes

When fixing a bug, add a regression test whenever practical.

The test should fail with the old behavior and pass with the fix.

## Fullstack features

For fullstack changes, test the relevant layers independently.

Depending on the change, this may include:

```text
Domain tests
Application/service tests
API/integration tests
Frontend unit tests
Frontend component tests
End-to-end tests
```

Do not automatically add expensive end-to-end tests when a lower-level test provides sufficient confidence.

---

# Code quality

Code should prioritize:

1. Correctness
2. Maintainability
3. Readability
4. Testability
5. Simplicity
6. Performance when relevant

Do not sacrifice clarity for cleverness.

Prefer explicit, readable code over highly compressed code.

Names should communicate intent.

Avoid abbreviations unless they are well established in the project/domain.

---

# Refactoring

Refactor when necessary to:

* implement the requested feature safely,
* preserve architectural boundaries,
* remove duplication introduced by the change,
* improve testability when required.

Avoid unrelated refactoring.

If the requested change reveals a larger architectural problem:

1. Implement the smallest safe change if possible.
2. Explain the larger issue separately.
3. Do not silently perform a large refactor.

---

# Verification

Before considering a task complete:

1. Inspect the final diff.
2. Check that only relevant files were changed.
3. Run the appropriate tests.
4. Run the project's formatting tools.
5. Run static analysis/linting.
6. Fix issues introduced by the change.
7. Re-run the affected checks.

Each subproject's `AGENTS.md` defines its specific verification commands.

---

# Final checklist

Before finishing any task:

* [ ] The relevant subproject `AGENTS.md` was read.
* [ ] Existing code was searched and reused where appropriate.
* [ ] The change follows the existing architecture.
* [ ] Responsibilities are clearly separated.
* [ ] Business logic has a clear authoritative location.
* [ ] Domain/application/infrastructure boundaries are respected where applicable.
* [ ] API/Web boundaries are explicit.
* [ ] No unnecessary duplication was introduced.
* [ ] No unnecessary abstraction was introduced.
* [ ] No unrelated refactoring was performed.
* [ ] No new dependency was added without explicit approval.
* [ ] No secrets or credentials were introduced.
* [ ] Appropriate tests were added or updated.
* [ ] Relevant tests pass.
* [ ] Formatting passes.
* [ ] Static analysis/linting passes.
* [ ] The final diff contains only relevant changes.
