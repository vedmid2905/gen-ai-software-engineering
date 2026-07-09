# 🏦 Homework 1: Banking Transactions API

> **Student Name**: Kostiantyn Vedmid
> **Date Submitted**: 20.06.2026
> **AI Tools Used**: Kiro, Claude Code

---

## 📋 Project Overview

An in-memory REST API for managing banking transactions, built with **Java 21** and **Spring Boot 3**. The project follows a spec-driven workflow: requirements, a design document, and a task breakdown were authored first (see [`../.kiro/specs/banking-transactions-api/`](../.kiro/specs/banking-transactions-api/)), then implemented layer by layer with AI assistance.

### Features Implemented

- `POST /transactions` — create a deposit, withdrawal, or transfer
- `GET /transactions` — list all transactions, with optional filters:
  - `accountId` — matches `fromAccount` or `toAccount`
  - `type` — exact match on `deposit` / `withdrawal` / `transfer`
  - `from` / `to` — inclusive ISO 8601 date or datetime range
  - filters combine conjunctively (AND)
- `GET /transactions/{id}` — retrieve a single transaction
- `GET /accounts/{accountId}/balance` — current balance; returns a single `balance`/`currency` pair, or a `balances[]` array when the account holds completed transactions in 2+ currencies
- `GET /accounts/{accountId}/summary` *(Task 4, Option A)* — total deposits, total withdrawals, transaction count, and most recent transaction date for an account
- Full request validation (type, amount, currency, account format) with all field errors reported in a single `400` response
- Consistent error handling via a global `@RestControllerAdvice` (400/404/500 with predictable JSON bodies)
- Thread-safe in-memory storage (`ConcurrentHashMap`) — no external database

### Architecture

```
HTTP Layer         → TransactionController, AccountController
Validation Layer   → Validator (invoked before any service logic)
Service Layer      → TransactionService, BalanceService, SummaryService
Domain Logic       → FilterEngine, BalanceCalculator, SummaryCalculator
Storage Layer      → TransactionStore (ConcurrentHashMap-backed singleton)
```

Key decisions:
- **Records for all DTOs and the domain entity** — immutability by default, no boilerplate.
- **Validator collects all errors before returning** rather than short-circuiting, so a single `400` response can list every invalid field at once.
- **Calculators are pure functions** (`FilterEngine`, `BalanceCalculator`, `SummaryCalculator`) — they take data in, return data out, with no dependency on storage or HTTP, which makes them trivial to unit-test.
- **`BigDecimal` + `RoundingMode.HALF_UP`** everywhere money is summed, to avoid floating-point rounding bugs.
- **IDs are always server-generated UUIDv4** — the API never accepts a caller-supplied `id`.

See [`../.kiro/specs/banking-transactions-api/design.md`](../.kiro/specs/banking-transactions-api/design.md) and [`requirements.md`](../.kiro/specs/banking-transactions-api/requirements.md) for the full specification this implementation follows.

### Tech Stack

Java 21, Spring Boot 3.5 (`spring-boot-starter-web`), Maven. Testing with JUnit 5, AssertJ, and [jqwik](https://jqwik.net/) for property-based tests.

### Project Structure

```
homework-1/
├── README.md
├── HOWTORUN.md
├── TASKS.md
├── docs/screenshots/
├── demo/
└── src/                              (Maven project root)
    ├── pom.xml
    └── src/
        ├── main/java/banking/
        │   ├── controller/           TransactionController, AccountController
        │   ├── service/              TransactionService, BalanceService, SummaryService,
        │   │                         FilterEngine, BalanceCalculator, SummaryCalculator
        │   ├── validation/           Validator
        │   ├── store/                TransactionStore
        │   ├── domain/               Transaction
        │   ├── dto/                  request/response records
        │   └── exception/            ValidationException, NotFoundException,
        │                             InvalidIdFormatException, GlobalExceptionHandler
        └── test/java/banking/
            ├── unit/                 TransactionStoreTest
            └── property/             ValidationPropertyTest (jqwik)
```

---

## 🤖 AI-Assisted Development Notes

This project used a spec-first workflow: requirements and a design document were drafted with Kiro before any code was written, then implementation proceeded task-by-task against that spec (`.kiro/specs/banking-transactions-api/tasks.md`). Claude Code was used in a follow-up session to complete the remaining service/controller layer, fix a broken test import, resolve a Maven dependency-resolution issue in the local environment, and verify the running API end-to-end with real HTTP requests.

<div align="center">

*This project was completed as part of the AI-Assisted Development course.*

</div>
