# Implementation Plan: Intelligent Customer Support System

## Overview

Implement a Java 17 / Spring Boot 3 REST API for managing customer support tickets. The implementation follows a layered architecture (Controller → Service → Repository) with a keyword-driven classifier, three import parsers (CSV, JSON, XML), and an append-only classification audit log. Tasks are ordered from project scaffolding through data models, core CRUD operations, classification, bulk import, and finally integration wiring.

## Tasks

- [x] 1. Set up project structure and core domain types
  - [x] 1.1 Initialise Maven project with Spring Boot 3, Spring Data JPA, H2, OpenCSV, Jackson, JAXB, Hibernate Validator, and jqwik dependencies in `pom.xml`
    - Create `src/main/java` package tree: `com.example.support` with sub-packages `controller`, `service`, `repository`, `model`, `dto`, `classifier`, `importer`, `audit`, `exception`, `config`
    - Add H2 datasource configuration to `application.properties` (dev/test) and a `application-prod.properties` stub for PostgreSQL
    - _Requirements: 1.1_

  - [x] 1.2 Define all enumeration types
    - Create `Category`, `Priority`, `Status`, `Source`, `DeviceType` enums in the `model` package
    - Annotate `Status.new_` with `@JsonProperty("new")` to handle the reserved-keyword mapping
    - _Requirements: 1.1, 2.4_

- [x] 2. Implement data models and JPA entities
  - [x] 2.1 Create the `TicketEntity` JPA class
    - Map all fields from the data model (UUID id, customerId, customerEmail, customerName, subject, description, category, priority, status, createdAt, updatedAt, resolvedAt, assignedTo, tags, source, browser, deviceType)
    - Use `@ElementCollection` for `tags`, `@Enumerated(EnumType.STRING)` for enums, `@Column(length=2000)` for description
    - _Requirements: 1.1_

  - [x] 2.2 Create the `ClassificationLogEntry` JPA entity
    - Map fields: id (UUID, generated), ticketId, category, priority, confidenceScore, reasoning, timestamp
    - _Requirements: 11.8, 12.2_

  - [x] 2.3 Create API DTOs and request/response objects
    - `TicketDto` (full ticket response), `CreateTicketRequest` (with Bean Validation annotations), `UpdateTicketRequest`, `ClassificationResult`, `BulkImportSummary`, `ImportError`
    - Add Jakarta validation annotations: `@Email`, `@Size`, `@NotBlank`, `@NotNull` to `CreateTicketRequest` fields
    - _Requirements: 1.1, 2.1, 2.2, 2.3, 2.4, 2.5_

- [x] 3. Implement persistence layer
  - [x] 3.1 Create `TicketRepository` interface
    - Extend `JpaRepository<TicketEntity, UUID>`
    - Add `findByCategory`, `findByPriority`, `findByStatus` query methods
    - Add `findAll(Specification<TicketEntity>)` support via `JpaSpecificationExecutor`
    - _Requirements: 4.1, 4.2, 4.3, 4.4_

  - [x] 3.2 Create `ClassificationLogRepository` interface
    - Extend `JpaRepository<ClassificationLogEntry, UUID>`
    - _Requirements: 11.8, 12.2_

  - [x] 3.3 Create `TicketSpecification` helper
    - Build a `Specification<TicketEntity>` by composing non-null `category`, `priority`, and `status` predicates for combined filtering
    - _Requirements: 4.2, 4.3, 4.4_

- [x] 4. Implement exception types and global error handler
  - [x] 4.1 Create custom exception classes
    - `TicketNotFoundException(UUID id)`, `UnsupportedImportFormatException(String type)`, `ClassificationLoggerUnavailableException`
    - _Requirements: 4.6, 5.2, 6.2, 10.5, 12.2, 13.4, 13.5, 13.6_

  - [x] 4.2 Implement `GlobalExceptionHandler` (`@RestControllerAdvice`)
    - Map `ConstraintViolationException` / `MethodArgumentNotValidException` → 400
    - Map `TicketNotFoundException` → 404 `{"message": "Ticket <id> not found"}`
    - Map `UnsupportedImportFormatException` → 415
    - Map `ClassificationLoggerUnavailableException` → 503
    - Map all other `Throwable` → 500 (log stack trace, do not expose it)
    - _Requirements: 13.1, 13.2, 13.3, 13.4, 13.5, 13.6, 13.7_

- [ ] 5. Implement the Classifier component
  - [x] 5.1 Create `Classifier` interface and `KeywordClassifier` implementation
    - Implement `classify(String subject, String description): ClassificationResult`
    - Define priority keyword tables (urgent / high / low / medium fallback) and category keyword tables as constants
    - Apply case-insensitive substring matching; assign the highest matching priority tier
    - Populate `keywords_found`, `confidence_score`, and `reasoning` in the result
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 11.7_

  - [ ] 5.2 Write property test for classifier priority precedence (Property 7)
    - **Property 7: Classifier priority precedence**
    - Use jqwik to generate arbitrary subsets of keywords from multiple priority tiers; assert that the highest tier wins regardless of keyword order
    - Add comment: `// Feature: customer-support-system, Property 7: Classifier priority precedence`
    - **Validates: Requirements 11.2, 11.3, 11.4, 11.5, 11.6**

  - [ ] 5.3 Write property test for classification result completeness and keywords accuracy (Property 8)
    - **Property 8: Classification result completeness and keywords accuracy**
    - Use jqwik to generate arbitrary ticket text containing known keywords; assert non-null category/priority, confidence_score ∈ [0.0, 1.0], non-null reasoning, and that keywords_found ⊆ actual text keywords
    - Add comment: `// Feature: customer-support-system, Property 8: Classification result completeness and keywords accuracy`
    - **Validates: Requirements 11.1, 11.7**

  - [ ] 5.4 Write unit tests for `KeywordClassifier`
    - Test each priority keyword tier (urgent, high, low, medium fallback)
    - Test each category keyword bucket and `other` fallback
    - Test mixed-tier tie-breaking, empty text, and text with no keywords
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 11.7_

- [ ] 6. Implement the ClassificationLogger component
  - [x] 6.1 Create `ClassificationLogger` interface and `JpaClassificationLogger` implementation
    - Implement `log(UUID ticketId, Category category, Priority priority, double confidenceScore, String reasoning, Instant timestamp)`
    - Persist a `ClassificationLogEntry` via `ClassificationLogRepository`
    - _Requirements: 11.8, 12.2_

  - [ ] 6.2 Write property test for classification log always written (Property 9)
    - **Property 9: Classification log entry always written**
    - Use jqwik to generate arbitrary valid ticket and classification trigger type; assert exactly one new `ClassificationLogEntry` is persisted with matching ticketId, category, priority, confidence_score, and a timestamp ≥ operation start
    - Add comment: `// Feature: customer-support-system, Property 9: Classification log entry always written`
    - **Validates: Requirements 11.8, 12.2**

- [ ] 7. Implement import parsers
  - [x] 7.1 Create `ImportParser` interface, `ParseResult` record, and `ImportParserFactory`
    - Define `parse(InputStream): List<ParseResult>` on the interface
    - `ImportParserFactory.forContentType(String contentType, String fileName)` selects CSV / JSON / XML parser or throws `UnsupportedImportFormatException`
    - _Requirements: 10.1, 10.5_

  - [x] 7.2 Implement `CsvImportParser`
    - Use OpenCSV; treat first row as header; map column names to `CreateTicketRequest` fields
    - Catch per-row parse errors; populate `ParseResult.error` with row index and message; continue processing
    - _Requirements: 7.1, 7.2, 7.3_

  - [ ] 7.3 Write property test for CSV–JSON round-trip equivalence (Property 11)
    - **Property 11: CSV–JSON round-trip equivalence**
    - Use jqwik to generate arbitrary valid `CreateTicketRequest` lists; serialise to CSV and JSON; parse both; assert field-value equivalence across the two parsed lists
    - Add comment: `// Feature: customer-support-system, Property 11: CSV–JSON round-trip equivalence`
    - **Validates: Requirements 7.4**

  - [ ] 7.4 Write unit tests for `CsvImportParser`
    - Test malformed rows (mismatched column counts, unclosed quotes), header-only file, single valid row
    - _Requirements: 7.1, 7.2, 7.3_

  - [ ] 7.5 Implement `JsonImportParser`
    - Use Jackson `ObjectMapper` to parse a JSON array of ticket objects into `List<CreateTicketRequest>`
    - Catch `JsonProcessingException`; populate `ParseResult.error` with parse error location; continue
    - _Requirements: 8.1, 8.2_

  - [ ] 7.6 Write property test for JSON serialization round-trip (Property 12)
    - **Property 12: JSON serialization round-trip**
    - Use jqwik to generate arbitrary valid `CreateTicketRequest` lists; serialise to JSON with Jackson; re-parse; assert field-value equivalence
    - Add comment: `// Feature: customer-support-system, Property 12: JSON serialization round-trip`
    - **Validates: Requirements 8.3**

  - [ ] 7.7 Write unit tests for `JsonImportParser`
    - Test malformed JSON, empty array, nested field parsing
    - _Requirements: 8.1, 8.2, 8.3_

  - [ ] 7.8 Implement `XmlImportParser`
    - Use `DocumentBuilder`/JAXB; parse each `<ticket>` element into `CreateTicketRequest`
    - On malformed elements: log a warning, record error in `ParseResult`, and continue with remaining elements
    - _Requirements: 9.1, 9.2, 9.3_

  - [ ] 7.9 Write unit tests for `XmlImportParser`
    - Test unclosed tags, invalid entity references, and mixed valid/invalid elements
    - _Requirements: 9.1, 9.2_

- [ ] 8. Checkpoint — Parsers and classifier verified
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 9. Implement `TicketService`
  - [ ] 9.1 Implement `createTicket(CreateTicketRequest req, boolean autoClassify)`
    - Auto-assign UUID `id`, set `createdAt` = `updatedAt` = now(), set `status` = `new`
    - Persist via `TicketRepository`; if `autoClassify=true`, invoke `Classifier` in a try-catch — on exception log a warning and return the already-persisted ticket with HTTP 201
    - _Requirements: 1.2, 1.3, 1.4, 3.1, 3.2_

  - [ ] 9.2 Write property test for new ticket identity and timestamp invariants (Property 1)
    - **Property 1: New ticket identity and timestamp invariants**
    - Use jqwik to generate arbitrary valid `CreateTicketRequest` instances; assert id is non-null UUID, createdAt is non-null, updatedAt == createdAt, status == new
    - Add comment: `// Feature: customer-support-system, Property 1: New ticket identity and timestamp invariants`
    - **Validates: Requirements 1.2, 1.3, 1.4**

  - [ ] 9.3 Write property test for classification failure does not block creation (Property 10)
    - **Property 10: Classification failure does not block ticket creation**
    - Use jqwik to generate arbitrary valid `CreateTicketRequest` with `autoClassify=true`; mock `Classifier` to throw; assert HTTP 201 is still returned and ticket is present in repository
    - Add comment: `// Feature: customer-support-system, Property 10: Classification failure does not block ticket creation`
    - **Validates: Requirements 3.2**

  - [ ] 9.4 Implement `getTickets(TicketFilter filter)` and `getTicketById(UUID id)`
    - Compose `TicketSpecification` from non-null filter fields; delegate to `TicketRepository`
    - Throw `TicketNotFoundException` when id not found
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_

  - [ ] 9.5 Write property test for filter returns exactly matching tickets (Property 5)
    - **Property 5: Filter returns exactly the matching tickets**
    - Use jqwik to generate arbitrary ticket lists and arbitrary filter combinations; assert every response ticket matches all supplied filter values and no matching ticket is absent
    - Add comment: `// Feature: customer-support-system, Property 5: Filter returns exactly the matching tickets`
    - **Validates: Requirements 4.2, 4.3, 4.4**

  - [ ] 9.6 Implement `updateTicket(UUID id, UpdateTicketRequest req)`
    - Load existing ticket (throw `TicketNotFoundException` if absent); apply non-null fields; set `updatedAt` = now()
    - If new status is `resolved` or `closed` and `resolvedAt` is null, set `resolvedAt` = now()
    - If request contains explicit `category` or `priority`, call `ClassificationLogger.log()` first; if logger throws, propagate `ClassificationLoggerUnavailableException`
    - _Requirements: 1.5, 1.6, 5.1, 5.2, 5.3, 12.1, 12.2_

  - [ ] 9.7 Write property test for updated_at advances on every update (Property 2)
    - **Property 2: updated_at advances on every update**
    - Use jqwik to generate arbitrary existing ticket + arbitrary valid `UpdateTicketRequest`; assert updatedAt_after >= updatedAt_before
    - Add comment: `// Feature: customer-support-system, Property 2: updated_at advances on every update`
    - **Validates: Requirements 1.5**

  - [ ] 9.8 Write property test for resolved_at set on transition to resolved or closed (Property 3)
    - **Property 3: resolved_at set on transition to resolved or closed**
    - Use jqwik to generate arbitrary non-resolved ticket + update setting status to resolved or closed; assert resolvedAt is non-null and >= updatedAt_before
    - Add comment: `// Feature: customer-support-system, Property 3: resolved_at set on transition to resolved or closed`
    - **Validates: Requirements 1.6**

  - [ ] 9.9 Write property test for manual override atomicity (Property 14)
    - **Property 14: Manual override is persisted and logged atomically**
    - Use jqwik to generate arbitrary classified ticket + arbitrary valid override values + available/unavailable logger; assert: logger available → ticket updated AND log entry exists; logger unavailable → HTTP 503 AND ticket unchanged
    - Add comment: `// Feature: customer-support-system, Property 14: Manual override is persisted and logged atomically`
    - **Validates: Requirements 12.1, 12.2**

  - [ ] 9.10 Implement `deleteTicket(UUID id)`
    - Throw `TicketNotFoundException` if not found; otherwise delete and return void
    - _Requirements: 6.1, 6.2_

  - [ ] 9.11 Implement `classifyTicket(UUID id)`
    - Load ticket (throw `TicketNotFoundException` if absent); invoke `Classifier`; update ticket `category` and `priority`; call `ClassificationLogger.log()`; persist and return `ClassificationResult`
    - _Requirements: 11.1, 11.7, 11.8, 11.9_

  - [ ] 9.12 Implement `importTickets(MultipartFile file, boolean autoClassify)`
    - Select parser via `ImportParserFactory`; iterate `ParseResult` list; validate each `CreateTicketRequest` (Bean Validation); persist valid records; optionally classify; accumulate `BulkImportSummary`
    - Process each record in its own try-catch; failures increment `failed` and append to `errors`; do not abort remaining records
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.6_

- [ ] 10. Implement validation layer
  - [x] 10.1 Create `TicketValidator` component
    - Use Jakarta `Validator` to validate `CreateTicketRequest` and `UpdateTicketRequest`; return a `ValidationResult` containing a list of `FieldError` objects
    - Called by `TicketService` before any persistence in the import pipeline (Bean Validation on `@Valid` parameters handles single-ticket endpoints automatically)
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_

  - [ ] 10.2 Write property test for validation rejects all constraint violations (Property 4)
    - **Property 4: Validation rejects all constraint violations**
    - Use jqwik to generate arbitrary invalid field values (bad email, out-of-range lengths, unknown enum values, missing required fields); assert HTTP 400 is returned and no ticket is persisted
    - Add comment: `// Feature: customer-support-system, Property 4: Validation rejects all constraint violations`
    - **Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5, 2.6**

  - [ ] 10.3 Write unit tests for `TicketValidator`
    - Boundary values: subject length 0, 1, 200, 201; description length 9, 10, 2000, 2001; invalid email strings; null required fields
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_

- [ ] 11. Implement `TicketController`
  - [ ] 11.1 Implement `POST /tickets` endpoint
    - Accept `@Valid @RequestBody CreateTicketRequest` and optional `?autoClassify=true` param; delegate to `TicketService.createTicket()`; return `ResponseEntity<TicketDto>` with status 201
    - _Requirements: 3.1, 3.2, 3.3_

  - [ ] 11.2 Implement `GET /tickets` and `GET /tickets/{id}` endpoints
    - Accept optional `?category`, `?priority`, `?status` query params; delegate to `TicketService.getTickets(filter)`; return 200 with list or single `TicketDto`
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_

  - [ ] 11.3 Write property test for non-existent resource returns HTTP 404 (Property 6)
    - **Property 6: Non-existent resource access always returns HTTP 404**
    - Use jqwik to generate arbitrary UUIDs not present in the repository; assert GET /tickets/{id}, PUT /tickets/{id}, DELETE /tickets/{id}, and POST /tickets/{id}/auto-classify all return 404 with JSON body containing `message` field
    - Add comment: `// Feature: customer-support-system, Property 6: Non-existent resource access always returns HTTP 404`
    - **Validates: Requirements 4.6, 5.2, 6.2, 11.9, 13.5**

  - [ ] 11.4 Implement `PUT /tickets/{id}` endpoint
    - Accept `@Valid @RequestBody UpdateTicketRequest`; delegate to `TicketService.updateTicket()`; return 200 with updated `TicketDto`
    - _Requirements: 5.1, 5.2, 5.3_

  - [ ] 11.5 Implement `DELETE /tickets/{id}` endpoint
    - Delegate to `TicketService.deleteTicket()`; return `ResponseEntity<Void>` with status 204
    - _Requirements: 6.1, 6.2_

  - [ ] 11.6 Implement `POST /tickets/import` endpoint
    - Accept `@RequestParam MultipartFile file` and optional `?autoClassify=true`; return `BulkImportSummary` with status 200
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6_

  - [ ] 11.7 Implement `POST /tickets/{id}/auto-classify` endpoint
    - Delegate to `TicketService.classifyTicket(id)`; return `ClassificationResult` with status 200
    - _Requirements: 11.1, 11.8, 11.9_

- [ ] 12. Checkpoint — All unit and property tests verified
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 13. Write integration tests
  - [ ] 13.1 Write `@SpringBootTest` integration tests for core CRUD flows
    - POST /tickets → 201; GET /tickets → 200 list; GET /tickets/{id} → 200 or 404; PUT /tickets/{id} → 200 or 400/404; DELETE /tickets/{id} → 204 or 404
    - _Requirements: 3.1, 3.3, 4.1, 4.5, 4.6, 5.1, 5.2, 5.3, 6.1, 6.2, 13.1, 13.2, 13.3_

  - [ ] 13.2 Write integration tests for bulk import (CSV, JSON, XML)
    - One end-to-end test per format with mixed valid/invalid records; assert `BulkImportSummary` counts and that only valid records appear in the repository
    - Test empty file → 400; unsupported format → 415
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

  - [ ] 13.3 Write integration test for classification logger 503 guard
    - Mock `ClassificationLogger` to throw; send `PUT /tickets/{id}` with explicit category/priority override; assert HTTP 503 and ticket category/priority unchanged in repository
    - _Requirements: 12.2_

  - [ ] 13.4 Write property test for bulk import summary invariant (Property 13)
    - **Property 13: Bulk import summary invariant**
    - Use jqwik to generate arbitrary mixed lists of valid and invalid records; assert total_records == successful + failed, errors.length == failed, exactly `successful` tickets persisted
    - Add comment: `// Feature: customer-support-system, Property 13: Bulk import summary invariant`
    - **Validates: Requirements 10.2, 10.3**

- [ ] 14. Final checkpoint — Full test suite green
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP
- Each task references specific requirements for traceability
- Property-based tests use jqwik with a minimum of 100 tries per property (`@Property(tries = 100)`)
- Each property test must include the comment `// Feature: customer-support-system, Property <N>: <property_text>`
- The `Status.new_` enum constant requires `@JsonProperty("new")` to avoid the Java reserved-keyword conflict
- H2 is used for dev/test; switch to PostgreSQL via `application-prod.properties` for production
- Classification failure on `autoClassify=true` is intentionally swallowed after persistence (Req 3.2); the ticket is returned with category/priority null
- The manual override logger guard (Req 12.2) calls `ClassificationLogger.log()` *before* writing to `TicketRepository`; if the logger throws, the ticket is not modified

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2"] },
    { "id": 1, "tasks": ["2.1", "2.2", "2.3"] },
    { "id": 2, "tasks": ["3.1", "3.2", "3.3", "4.1", "4.2"] },
    { "id": 3, "tasks": ["5.1", "6.1", "7.1", "10.1"] },
    { "id": 4, "tasks": ["5.2", "5.3", "5.4", "6.2", "7.2", "7.5", "7.8"] },
    { "id": 5, "tasks": ["7.3", "7.4", "7.6", "7.7", "7.9", "10.2", "10.3"] },
    { "id": 6, "tasks": ["9.1", "9.4", "9.10", "9.11", "9.12"] },
    { "id": 7, "tasks": ["9.2", "9.3", "9.5", "9.6"] },
    { "id": 8, "tasks": ["9.7", "9.8", "9.9", "11.1", "11.2", "11.4", "11.5", "11.6", "11.7"] },
    { "id": 9, "tasks": ["11.3", "13.1", "13.2", "13.3", "13.4"] }
  ]
}
```
