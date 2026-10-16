# Copilot instructions for Sistema Bancário

This repository is a Java academic project focused on a concurrent banking system. The main goal is to model deposits, withdrawals, and transfers correctly under multithreading, while preserving a consistent balance and transaction history.

## Project context

The project is centered on a classic race-condition exercise: multiple threads operating on the same account balances at the same time must not corrupt the final totals. The expected architecture is a layered Java application with clear separation between domain logic, persistence, simulation, and application entry points.

Primary domain concepts:
- `Cliente`: account holder
- `Conta`: the core entity whose balance changes through synchronized business operations
- `Transacao`: immutable operation record (type, value, timestamp)
- `TipoTransacao`: transaction type enum

Expected architectural layers:
- `domain/`: business entities and validation logic; no JDBC, file I/O, or repository dependencies
- `persistence/` and `persistence/jdbc/`: JDBC repositories (`ContaRepository`, `TransacaoRepository`, `JdbcContaRepository`, `JdbcTransacaoRepository`, `ConnectionFactory`)
- `simulacao/`: concurrent operations and runner logic (`OperacaoBancariaThread`, `SimulacaoRunner`)
- `service/`: orchestration layer between domain and persistence (`BancoService`)
- `io/`: file export logic (`ExtratoExporter`)
- `ui/`: Swing interface for interactive banking operations
- `app/`: application startup (`Main`), which opens the Swing interface

The repository instructions in `AGENT.md` are the canonical source for project constraints. Treat them as the priority specification for naming, package structure, and concurrency requirements.

## Build, test, and lint commands

The project uses Maven and Java 17 (`pom.xml`).
- Run all tests: `mvn test`
- Run one test class: `mvn -Dtest=ContaTest test`
- Run one test method: `mvn "-Dtest=ContaTest#depositarAtualizaSaldoERegistraTransacao" test`
- Build/package: `mvn package`
- Run the desktop app: `mvn exec:java`

No linter or formatter is configured; do not invent lint commands.

## High-level architecture and responsibilities

Keep the work aligned with the layered design in `AGENT.md`. The Swing UI lives in `ui/` and delegates operations to `BancoService`; it must not implement banking rules itself. The current in-memory repositories support the desktop demonstration, while JDBC persistence remains a required project layer.

- Domain layer owns business rules and concurrency safety for balance changes. `Cliente` and `Transacao` are immutable.
- `Conta` is the single place where the balance may change; balance and in-memory statement share its lock. `BigDecimal` represents money and values are limited to two decimal places.
- Transfers acquire both account monitors in ascending account-id order; do not change this ordering or call infrastructure while holding account locks.
- Persistence layer handles JDBC storage for accounts and transactions; do not leak database concerns into domain classes.
- Simulation layer drives concurrent operations and verifies the final balances under load.
- `BancoService` or similar coordination code should combine domain actions with repository persistence, but the domain itself should remain independent from infrastructure.
- Swing classes under `ui/` collect user input and display balances/statements; delegate business operations to `BancoService`.
- Exported statements should be produced in the I/O layer rather than inside domain entities.

This is not a generic CRUD app; the core correctness condition is that concurrent operations leave the expected final balance even under heavy contention. The desktop app uses H2 JDBC persistence, and its simulation and statement export are available through the Swing UI.

## Key conventions and project-specific rules

Follow these repository-specific rules instead of generic Java conventions:

- Preserve the required class and method names exactly when implementing the core project: `Conta`, `Cliente`, `Transacao`, `depositar`, `sacar`, `transferirPara`, `OperacaoBancariaThread`, `ContaRepository`, and `TransacaoRepository`.
- Keep the domain free of JDBC, `java.sql`, file system access, and repository references.
- Use synchronized access for account state changes to avoid race conditions.
- In transfers, acquire account locks in ascending immutable account-ID order to prevent deadlock, regardless of transfer direction.
- Treat each financial operation as an immutable transaction record with type, value, and timestamp.
- Simulations must validate that the final balance matches the expected value after concurrent operations.
- Include a minimum of five JUnit tests, including at least one concurrency-focused test.
- Persist transaction history and keep account statements exportable to a text file.
- Keep package structure aligned with the required architecture: `domain`, `persistence`, `simulacao`, `app`, with `service` and `io` as justified extensions.

## Operational guidance for future Copilot sessions

When making changes in this repository:
- Start from the domain and concurrency requirements before touching repositories or UI/app code.
- Prefer minimal, targeted edits that keep the architecture layered and explicit.
- Verify that concurrent logic does not introduce deadlocks or inconsistent final balances.
- If new classes are added, keep them aligned with the established package boundaries and responsibilities.
- If a change affects statement export, persistence, or threading, validate the change against the concurrency and balance invariants described in `AGENT.md`.

## References

- `AGENT.md` — primary project specification and grading criteria
- project package structure and naming conventions described in the spec

The tests cover domain validation, deposits, withdrawals, transfers, concurrent deposit/withdraw operations, opposite-direction transfers with timeouts, in-memory service behavior, H2 persistence and rollback, simulation balance invariants, and UTF-8 statement export. Keep `AGENT.md` as the authoritative specification when changing any layer.
