# Design Document — Intelligent Customer Support System

## Overview

The Intelligent Customer Support System is a Java 17 / Spring Boot 3 REST API that manages the full lifecycle of customer support tickets. The system exposes a conventional REST interface for creating, reading, updating, and deleting individual tickets, plus a bulk-import endpoint that accepts CSV, JSON, and XML files. After a ticket is persisted it can be automatically classified — either immediately on creation (via the `autoClassify` flag) or on demand — using a keyword-driven classifier that assigns a `category` and `priority`. Every classification decision is audited to an append-only log.

The design follows a standard layered Spring Boot architecture:

```
Controller → Service → Repository
                ↓
         Classifier / Import Pipeline
                ↓
         Classification Logger
```

Key quality goals:
- **Correctness** — every field accepted by the API is validated before persistence; no invalid data reaches the database.
- **Auditability** — every classification event (automatic or manual override) is logged with ticket id, category, priority, confidence score, and timestamp.
- **Extensibility** — the import pipeline is designed around an `ImportParser` strategy interface so new formats can be added without changing the controller or service.
- **Resilience** — classification failure must not block ticket creation; the bulk import must continue processing remaining records after a per-record error.

---

## Architecture

### Component Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                       HTTP Client / Postman                     │
└───────────────────────────┬─────────────────────────────────────┘
                            │  REST over HTTP
┌───────────────────────────▼─────────────────────────────────────┐
│                    TicketController                              │
│   POST /tickets          GET /tickets      GET /tickets/:id      │
│   PUT  /tickets/:id      DELETE /tickets/:id                     │
│   POST /tickets/import   POST /tickets/:id/auto-classify         │
└───────────────────────────┬─────────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────────┐
│                     TicketService                                │
│  createTicket()   getTickets()   getTicketById()                 │
│  updateTicket()   deleteTicket() importTickets()                 │
│  classifyTicket()                                                │
└──────┬──────────────┬──────────────────┬────────────────────────┘
       │              │                  │
┌──────▼──────┐  ┌────▼──────────┐  ┌───▼─────────────────────┐
│  Ticket     │  │  Classifier   │  │  ImportParserFactory     │
│  Repository │  │               │  │  ├─ CsvImportParser      │
│  (JPA /     │  │               │  │  ├─ JsonImportParser     │
│  H2/Postgres│  └────┬──────────┘  │  └─ XmlImportParser      │
└─────────────┘       │             └─────────────────────────┘
               ┌──────▼──────┐
               │Classification│
               │   Logger     │
               └─────────────┘
```

### Technology Stack

| Layer | Technology |
|---|---|
| Language / Platform | Java 17, Spring Boot 3.x |
| Persistence | Spring Data JPA + H2 (dev/test), PostgreSQL (prod) |
| CSV parsing | OpenCSV 5.x |
| JSON parsing | Jackson Databind (already a Spring Boot transitive dependency) |
| XML parsing | JAXB / `javax.xml.parsers.DocumentBuilder` |
| Validation | Jakarta Bean Validation 3 (`@Valid`, Hibernate Validator) |
| Build | Maven 3.x |
| Testing | JUnit 5, Mockito, AssertJ, jqwik (property-based testing) |

---

## Components and Interfaces

### TicketController

A `@RestController` annotated class mapped to `/tickets`.

| Method | Path | Description |
|---|---|---|
| `POST` | `/tickets` | Create a single ticket (optional `?autoClassify=true`) |
| `GET` | `/tickets` | List all tickets with optional `?category`, `?priority`, `?status` filters |
| `GET` | `/tickets/{id}` | Get a single ticket by UUID |
| `PUT` | `/tickets/{id}` | Full or partial update |
| `DELETE` | `/tickets/{id}` | Delete a ticket |
| `POST` | `/tickets/import` | Bulk import from CSV / JSON / XML |
| `POST` | `/tickets/{id}/auto-classify` | Trigger on-demand classification |

### TicketService

Orchestrates use-case logic. It is the only caller of `TicketRepository`, `Classifier`, `ClassificationLogger`, and the import pipeline.

```java
public interface TicketService {
    TicketDto createTicket(CreateTicketRequest req, boolean autoClassify);
    List<TicketDto> getTickets(TicketFilter filter);
    TicketDto getTicketById(UUID id);
    TicketDto updateTicket(UUID id, UpdateTicketRequest req);
    void deleteTicket(UUID id);
    BulkImportSummary importTickets(MultipartFile file, boolean autoClassify);
    ClassificationResult classifyTicket(UUID id);
}
```

### TicketRepository

Spring Data JPA `JpaRepository<TicketEntity, UUID>` extended with filter query methods:

```java
List<TicketEntity> findByCategory(Category category);
List<TicketEntity> findByPriority(Priority priority);
List<TicketEntity> findByStatus(Status status);
```

For combined filters a `Specification<TicketEntity>` is composed dynamically.

### Classifier

Stateless Spring `@Component`. Contains the keyword-to-priority and keyword-to-category mapping tables. Exposes:

```java
public interface Classifier {
    ClassificationResult classify(String subject, String description);
}
```

**Priority keyword table** (case-insensitive substring match):

| Priority | Keywords |
|---|---|
| `urgent` | `can't access`, `critical`, `production down`, `security` |
| `high` | `important`, `blocking`, `asap` |
| `low` | `minor`, `cosmetic`, `suggestion` |
| `medium` | *(fallback when no other keyword matches)* |

The classifier applies the highest matching priority tier per Req 11.6.

**Category keyword table** (illustrative — full table defined in configuration):

| Category | Example keywords |
|---|---|
| `account_access` | `login`, `password`, `account`, `locked` |
| `technical_issue` | `error`, `crash`, `not working`, `broken` |
| `billing_question` | `invoice`, `charge`, `payment`, `refund` |
| `feature_request` | `feature`, `enhancement`, `would like`, `can you add` |
| `bug_report` | `bug`, `defect`, `regression`, `unexpected` |
| `other` | *(fallback)* |

### ClassificationLogger

Persists a `ClassificationLogEntry` for every classification event (auto or manual override):

```java
public interface ClassificationLogger {
    void log(UUID ticketId, Category category, Priority priority,
             double confidenceScore, String reasoning, Instant timestamp);
}
```

`ClassificationLogEntry` is stored in its own JPA-managed table `classification_log`.

### ImportParserFactory

Selects the correct `ImportParser` based on MIME type / file extension:

```java
public interface ImportParser {
    List<ParseResult> parse(InputStream inputStream);
}

// Returned by each parser per record
public record ParseResult(TicketCreateRequest ticket, int recordIndex, String error) {}
```

| Content-Type | File extension | Parser |
|---|---|---|
| `text/csv` | `.csv` | `CsvImportParser` |
| `application/json` | `.json` | `JsonImportParser` |
| `application/xml` or `text/xml` | `.xml` | `XmlImportParser` |

Unknown types trigger HTTP 415.

### TicketValidator

A Spring `@Component` that wraps Jakarta Bean Validation. It is called by the service layer before any persistence operation. Returns a `ValidationResult` containing a list of `FieldError` objects.

---

## Data Models

### Ticket (API DTO)

```json
{
  "id": "uuid",
  "customer_id": "string",
  "customer_email": "string (RFC 5321)",
  "customer_name": "string",
  "subject": "string (1–200 chars)",
  "description": "string (10–2000 chars)",
  "category": "account_access | technical_issue | billing_question | feature_request | bug_report | other",
  "priority": "urgent | high | medium | low",
  "status": "new | in_progress | waiting_customer | resolved | closed",
  "created_at": "ISO-8601 datetime",
  "updated_at": "ISO-8601 datetime",
  "resolved_at": "ISO-8601 datetime | null",
  "assigned_to": "string | null",
  "tags": ["string"],
  "metadata": {
    "source": "web_form | email | api | chat | phone",
    "browser": "string",
    "device_type": "desktop | mobile | tablet"
  }
}
```

### TicketEntity (JPA)

```java
@Entity
@Table(name = "tickets")
public class TicketEntity {
    @Id UUID id;
    String customerId;
    String customerEmail;
    String customerName;
    String subject;
    @Column(length = 2000) String description;
    @Enumerated(EnumType.STRING) Category category;
    @Enumerated(EnumType.STRING) Priority priority;
    @Enumerated(EnumType.STRING) Status status;
    Instant createdAt;
    Instant updatedAt;
    Instant resolvedAt;          // nullable
    String assignedTo;           // nullable
    @ElementCollection List<String> tags;
    @Enumerated(EnumType.STRING) Source source;
    String browser;
    @Enumerated(EnumType.STRING) DeviceType deviceType;
}
```

### ClassificationResult (API response)

```json
{
  "ticket_id": "uuid",
  "category": "Category",
  "priority": "Priority",
  "confidence_score": 0.0–1.0,
  "reasoning": "string",
  "keywords_found": ["string"]
}
```

### BulkImportSummary (API response)

```json
{
  "total_records": 5,
  "successful": 4,
  "failed": 1,
  "errors": [
    { "record_index": 2, "message": "subject must not be blank" }
  ]
}
```

### Enumerations

```java
public enum Category   { account_access, technical_issue, billing_question,
                         feature_request, bug_report, other }
public enum Priority   { urgent, high, medium, low }
public enum Status     { new_, in_progress, waiting_customer, resolved, closed }
public enum Source     { web_form, email, api, chat, phone }
public enum DeviceType { desktop, mobile, tablet }
```

*(Note: `new` is a reserved Java keyword; the enum constant is named `new_` with `@JsonProperty("new")` mapping.)*

### ClassificationLogEntry (JPA)

```java
@Entity
@Table(name = "classification_log")
public class ClassificationLogEntry {
    @Id @GeneratedValue UUID id;
    UUID ticketId;
    @Enumerated(EnumType.STRING) Category category;
    @Enumerated(EnumType.STRING) Priority priority;
    double confidenceScore;
    String reasoning;
    Instant timestamp;
}
```

---

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system — essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: New ticket identity and timestamp invariants

*For any* valid `CreateTicketRequest`, the ticket returned by `createTicket()` SHALL have a non-null UUID `id`, a non-null `created_at`, a non-null `updated_at` equal to `created_at`, and `status` equal to `new`.

**Validates: Requirements 1.2, 1.3, 1.4**

---

### Property 2: updated_at advances on every update

*For any* existing ticket and any valid `UpdateTicketRequest`, the `updated_at` field of the updated ticket SHALL be greater than or equal to the `updated_at` of the ticket before the update.

**Validates: Requirements 1.5**

---

### Property 3: resolved_at set on transition to resolved or closed

*For any* existing ticket whose status is not `resolved` or `closed`, when an update sets the status to `resolved` or `closed`, the resulting ticket's `resolved_at` SHALL be non-null and SHALL be a timestamp no earlier than `updated_at` before the update was applied.

**Validates: Requirements 1.6**

---

### Property 4: Validation rejects all constraint violations

*For any* ticket creation or update request that violates at least one of the following constraints — `customer_email` not conforming to RFC 5321, `subject` length outside [1, 200], `description` length outside [10, 2000], any enum field (`category`, `priority`, `status`, `source`, `device_type`) containing a value outside its defined set, or a required field being absent — the system SHALL return HTTP 400 and the ticket SHALL NOT be persisted or modified.

**Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5, 2.6**

---

### Property 5: Filter returns exactly the matching tickets

*For any* combination of `category`, `priority`, and/or `status` query parameters applied to `GET /tickets`, every ticket in the response SHALL match all supplied filter values, and no ticket in the repository that matches all filter values SHALL be absent from the response.

**Validates: Requirements 4.2, 4.3, 4.4**

---

### Property 6: Non-existent resource access always returns HTTP 404

*For any* UUID that does not correspond to a persisted ticket, any request that references that UUID — `GET /tickets/:id`, `PUT /tickets/:id`, `DELETE /tickets/:id`, or `POST /tickets/:id/auto-classify` — SHALL return HTTP 404 with a JSON error body containing a `message` field.

**Validates: Requirements 4.6, 5.2, 6.2, 11.9, 13.5**

---

### Property 7: Classifier priority precedence

*For any* combination of keywords drawn from different priority tiers present in ticket subject and description text, the Classifier SHALL assign the highest applicable priority tier (`urgent` > `high` > `low` > `medium`), regardless of keyword order or position in the text.

**Validates: Requirements 11.2, 11.3, 11.4, 11.5, 11.6**

---

### Property 8: Classification result completeness and keywords accuracy

*For any* ticket whose text contains a known set of keywords, the `ClassificationResult` returned by the Classifier SHALL contain non-null `category`, `priority`, `confidence_score` in [0.0, 1.0], non-null `reasoning`, and `keywords_found` SHALL contain only keywords that actually appear in the ticket text.

**Validates: Requirements 11.1, 11.7**

---

### Property 9: Classification log entry always written

*For any* successful classification event — whether triggered automatically on creation, via `POST /tickets/:id/auto-classify`, or by a manual override via `PUT /tickets/:id` — the `classification_log` table SHALL contain exactly one new entry recording the ticket `id`, assigned `category`, `priority`, `confidence_score`, and a timestamp no earlier than the start of the operation.

**Validates: Requirements 11.8, 12.2**

---

### Property 10: Classification failure does not block ticket creation

*For any* valid `CreateTicketRequest` submitted with `autoClassify=true`, if the Classifier throws an exception or times out, the system SHALL still return HTTP 201 with the persisted ticket, and the ticket SHALL be present in the repository.

**Validates: Requirements 3.2**

---

### Property 11: CSV–JSON round-trip equivalence

*For any* set of valid ticket records representable in both CSV and JSON, parsing the CSV representation SHALL produce a list of `TicketCreateRequest` objects with field values equivalent to parsing the JSON representation of the same records.

**Validates: Requirements 7.4**

---

### Property 12: JSON serialization round-trip

*For any* valid JSON ticket array file, serializing the parsed `TicketCreateRequest` objects back to JSON SHALL produce output whose field values are equivalent to those in the original input.

**Validates: Requirements 8.3**

---

### Property 13: Bulk import summary invariant

*For any* bulk import request containing an arbitrary mix of valid and invalid records, the returned `BulkImportSummary` SHALL satisfy: `total_records == successful + failed`, `errors.length == failed`, and exactly `successful` tickets SHALL be persisted in the repository after the operation.

**Validates: Requirements 10.2, 10.3**

---

### Property 14: Manual override is persisted and logged atomically

*For any* existing classified ticket and any valid explicit `category` or `priority` value supplied in a `PUT /tickets/:id` request, if the `ClassificationLogger` is available, the updated ticket SHALL reflect the supplied values AND a corresponding log entry SHALL exist; if the `ClassificationLogger` is unavailable, the system SHALL return HTTP 503 and the ticket's `category` and `priority` SHALL remain unchanged.

**Validates: Requirements 12.1, 12.2**

---

## Error Handling

### Global Exception Handler

A `@RestControllerAdvice` class (`GlobalExceptionHandler`) centralises error translation:

| Exception | HTTP status | Response body |
|---|---|---|
| `ConstraintViolationException`, `MethodArgumentNotValidException` | 400 | `{"message": "<violations>"}` |
| `TicketNotFoundException` | 404 | `{"message": "Ticket <id> not found"}` |
| `UnsupportedMediaTypeException` | 415 | `{"message": "Unsupported import format: <type>"}` |
| `ClassificationLoggerUnavailableException` | 503 | `{"message": "Classification logger unavailable; override rejected"}` |
| Any other `Throwable` | 500 | `{"message": "An internal error occurred"}` + full stack trace to log |

### Classification Failure Isolation

Per Req 3.2, if the `Classifier` throws or times out during `createTicket()`, the service catches the exception, logs a warning, and returns the already-persisted ticket with `category` and `priority` left at their default/null values. The HTTP response is still 201.

### Bulk Import Error Isolation

Per Req 10.3, each record is processed inside its own try-catch block within the import loop. Validation or parsing failures for one record populate the `errors` array and increment `failed`; they do not abort processing of subsequent records.

### Manual Override Logger Guard

Per Req 12.2, the service calls `ClassificationLogger.log()` before writing the override to `TicketRepository`. If the logger throws, the service propagates a `ClassificationLoggerUnavailableException` (which the global handler maps to HTTP 503) and leaves the ticket unchanged.

---

## Testing Strategy

### Unit Tests (JUnit 5 + Mockito + AssertJ)

Focus on specific scenarios, edge cases, and integration wiring:

- `ClassifierTest` — concrete examples for each keyword tier and category, mixed-keyword tie-breaking, empty text.
- `TicketValidatorTest` — boundary values: `subject` of length 0, 1, 200, 201; `description` of length 9, 10, 2000, 2001; invalid email strings; null required fields.
- `CsvImportParserTest` — malformed rows (mismatched columns, unclosed quote), header-only file, single valid row.
- `JsonImportParserTest` — malformed JSON, empty array, nested field parsing.
- `XmlImportParserTest` — unclosed tags, invalid entity references, mixed valid/invalid elements.
- `TicketServiceTest` — classification-failure-does-not-block-201, logger-guard-returns-503, bulk-import-partial-success.
- `GlobalExceptionHandlerTest` — each mapped exception type produces the correct HTTP status and JSON body.

### Property-Based Tests (jqwik)

The project uses [jqwik](https://jqwik.net/) as the property-based testing library. Each property test is configured to run a minimum of **100 tries**.

Each test is tagged with a comment referencing the design property:
```
// Feature: customer-support-system, Property <N>: <property_text>
```

| Property | Test class | What jqwik generates |
|---|---|---|
| Property 1: New ticket identity & timestamp invariants | `TicketCreationPropertyTest` | Arbitrary valid `CreateTicketRequest` instances |
| Property 2: updated_at advances on update | `TicketUpdatePropertyTest` | Arbitrary existing ticket + arbitrary valid `UpdateTicketRequest` |
| Property 3: resolved_at set on resolved/closed | `TicketUpdatePropertyTest` | Arbitrary existing ticket + update setting status to resolved/closed |
| Property 4: Validation rejects all constraint violations | `ValidationPropertyTest` | Arbitrary invalid field values (bad email, out-of-range lengths, unknown enum values, missing required fields) |
| Property 5: Filter returns exactly matching tickets | `TicketFilterPropertyTest` | Arbitrary ticket lists + arbitrary filter combinations |
| Property 6: Non-existent resource returns HTTP 404 | `NotFoundPropertyTest` | Arbitrary UUIDs not present in the repository |
| Property 7: Classifier priority precedence | `ClassifierPrecedencePropertyTest` | Arbitrary subsets of keywords from multiple priority tiers |
| Property 8: Classification result completeness & accuracy | `ClassifierResultPropertyTest` | Arbitrary ticket text with known keywords |
| Property 9: Classification log always written | `ClassificationLogPropertyTest` | Arbitrary valid ticket + arbitrary classification event |
| Property 10: Classifier failure does not block creation | `ClassificationResiliencePropertyTest` | Arbitrary valid `CreateTicketRequest` + mocked throwing Classifier |
| Property 11: CSV–JSON round-trip equivalence | `CsvRoundTripPropertyTest` | Arbitrary valid `TicketCreateRequest` lists |
| Property 12: JSON serialization round-trip | `JsonRoundTripPropertyTest` | Arbitrary valid `TicketCreateRequest` lists |
| Property 13: Bulk import summary invariant | `BulkImportPropertyTest` | Arbitrary mixed lists of valid and invalid records |
| Property 14: Manual override atomicity | `ManualOverridePropertyTest` | Arbitrary classified ticket + arbitrary valid override values + available/unavailable logger |

### Integration Tests

- Spring Boot `@SpringBootTest` + `MockMvc` or `TestRestTemplate` to exercise full request/response cycles.
- Cover the HTTP-layer behaviours: 201 on create, 404 on missing id, 415 on unsupported import type, 400 on empty file, 204 on delete.
- At least one end-to-end bulk-import test per supported format (CSV, JSON, XML).
- Classification logger 503 guard via a mocked `ClassificationLogger` that throws.
