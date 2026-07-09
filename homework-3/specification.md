# Virtual Card Control and Transaction Visibility Specification

> This specification defines a regulated finance feature for creating, controlling, and reviewing virtual cards. It is written so an implementation team or AI coding partner can build the feature without guessing about business rules, safety constraints, or verification expectations.

## High-Level Objective
- Enable cardholders and internal operations/compliance staff to safely create, manage, and review virtual card activity in a regulated environment so that customers can control spend while the institution maintains auditable, policy-compliant oversight.

## Mid-Level Objectives
- M1. Card lifecycle management works reliably for create, freeze/unfreeze, limit updates, replacement, and closure actions with explicit state transitions.
- M2. Users can view transaction history and submit disputes with consistent filtering, status visibility, and clear error handling.
- M3. Sensitive operations enforce authorization boundaries and reduce misuse risk through role checks, policy rules, and fraud-aware controls.
- M4. All material actions produce immutable audit evidence suitable for internal review, compliance reporting, and incident investigation.
- M5. The feature meets explicit reliability and performance targets under normal and degraded operating conditions.

## Non-Functional & Policy Requirements
- Security and privacy:
  - Sensitive card data must be tokenized or masked in all user-facing responses and logs.
  - No full PAN, CVV, or secret material may be stored in application logs, support tickets, or debug output.
  - Access to card details, limits, disputes, and audit history must be role-based and least-privilege.
- Auditability:
  - Every state-changing action must emit an immutable audit event containing actor, action, target identifier, timestamp, reason code, previous state, new state, and correlation identifier.
  - Audit retention must satisfy internal policy and applicable regulatory expectations; a minimum retention period of 7 years is assumed for this specification.
- Reliability:
  - Mutation operations must be idempotent when retried with the same request identifier.
  - A failed mutation must leave the system in a consistent, recoverable state with no partial application of conflicting updates.
  - Read-after-write consistency for state changes must be achieved within 2 seconds for successful operations.
- Performance targets (assumed targets for a regulated digital banking UX):
  - Card read and list operations: p95 latency under 300 ms.
  - Card mutation operations: p95 latency under 600 ms.
  - Dispute submission and transaction listing: p95 latency under 700 ms.
  - Background reconciliation or audit export jobs: process at least 5,000 records per minute per worker.
  - Rate limiting: 60 requests per minute per authenticated user for mutation endpoints; 300 requests per minute for reads.
- Availability:
  - Read paths target 99.9% monthly availability.
  - Mutation paths target 99.5% monthly availability.

## Implementation Notes
- Money must be represented in minor currency units (for example cents) using integers; floating-point arithmetic is not permitted for financial calculations.
- All card identifiers must be opaque, non-sequential, and non-sensitive; UUID or ULID is preferred.
- State transitions must follow an explicit finite-state model: pending_approval, active, frozen, closed, replaced, disputed.
- The system must reject invalid state transitions with a business error rather than silently coercing the change.
- Idempotency keys are mandatory for create, freeze/unfreeze, limit updates, replacement requests, and dispute submission.
- The system must support optimistic concurrency for state updates using a version or etag field; conflicting writes must return a conflict error and require refresh.
- Error handling must be explicit and user-friendly: distinguish validation problems, authorization problems, conflicts, and downstream dependency failures.
- All writes must be transactional at the service boundary and must emit an audit event as part of the same logical transaction.
- For fraud-like patterns, the system should not perform silent overrides; it must either require manual review or block the action with a documented reason.

## Context

### Beginning Context
- An authenticated customer and internal staff identity model already exists.
- A customer account service, a card inventory service, and an event/audit stream are available as integration points.
- The current workspace contains no implementation artifacts for this feature; only the specification and supporting documents are required.

### Ending Context
- A card record model exists for virtual cards with lifecycle state, policy values, and versioning.
- A dispute record model and transaction view model exist for customer and ops/compliance use.
- An audit event store and an operational dashboard/reporting artifact are available for review.
- The implementation includes validation, error handling, idempotency, and verification fixtures sufficient for testing.

## Low-Level Tasks

### 1. Define the card domain model and policy schema
- Serves: M1, M3
- Deliverables: card entity, state enum, policy attributes, version field, and validation rules.
- Acceptance criteria:
  - The model explicitly supports create, freeze, unfreeze, limit update, replacement, closure, and dispute-related states.
  - Policy values include per-card and per-account limits, freeze eligibility, and dispute window rules.
  - All monetary fields are stored as integers in minor units.

### 2. Implement card creation workflow
- Serves: M1, M4
- Deliverables: create-card request handling, validation, persistence, and success/audit event emission.
- Acceptance criteria:
  - A new card can be created only if the requester is authorized and the account is eligible.
  - The created card is assigned a non-sensitive identifier and default policy values.
  - Duplicate create requests with the same idempotency key return the same result without creating duplicates.

### 3. Implement freeze and unfreeze workflow
- Serves: M1, M3, M4
- Deliverables: state transition logic, role checks, and audit logging for freeze/unfreeze actions.
- Acceptance criteria:
  - Freeze/unfreeze actions are rejected if the card is already in a terminal state or the requester lacks permission.
  - The card state changes are recorded with previous and new state values.
  - A frozen card cannot be used for new authorization attempts until explicitly unfrozen.

### 4. Implement limit update workflow
- Serves: M1, M3, M4
- Deliverables: validation for minimum/maximum amounts, transactional update logic, and conflict handling.
- Acceptance criteria:
  - Limits must be validated against business policy and currency constraints.
  - An invalid or inconsistent update is rejected with a clear error and no partial state change.
  - Concurrent limit updates return a conflict response and preserve the latest committed state.

### 5. Implement transaction listing and filtering
- Serves: M2, M5
- Deliverables: transaction retrieval endpoint, filter parameters, pagination, and secure visibility rules.
- Acceptance criteria:
  - Results are paginated and sorted deterministically by transaction timestamp.
  - Read permissions are enforced based on role and card ownership.
  - Empty results return a clear empty-state response instead of a generic error.

### 6. Implement dispute intake workflow
- Serves: M2, M3, M4
- Deliverables: dispute submission, validation, status tracking, and audit event creation.
- Acceptance criteria:
  - Disputes are created only for eligible transactions and only when the dispute window has not closed.
  - A duplicate dispute request with the same idempotency key does not create more than one dispute record.
  - The dispute status is visible to both the customer and operations/compliance staff with the same source of truth.

### 7. Implement authorization and policy enforcement
- Serves: M3
- Deliverables: role checks, permission matrix, and fraud-aware review steps.
- Acceptance criteria:
  - Customer users cannot modify another customer’s card or view another customer’s dispute history.
  - Internal staff can access review workflows only through their permitted roles.
  - Suspicious trigger patterns such as rapid successive state changes or repeated failed auth attempts are routed to manual review or blocked.

### 8. Implement immutable audit event emission
- Serves: M4
- Deliverables: event schema, append-only storage logic, correlation identifiers, and export support.
- Acceptance criteria:
  - Every state-changing action generates a persisted audit event.
  - Audit records cannot be silently deleted or altered by regular application operations.
  - Audit event payloads omit sensitive card data while preserving enough context for review.

### 9. Implement idempotency, retries, and recovery behavior
- Serves: M1, M5
- Deliverables: request deduplication logic, retry-safe handlers, and reconciliation support.
- Acceptance criteria:
  - A retried mutation with the same idempotency key returns the original outcome rather than double-applying the change.
  - Recovery from transient dependency failures leaves the system in a consistent state.
  - The implementation exposes a clear reason for retryable versus non-retryable failures.

### 10. Implement observability, alerting, and operational runbooks
- Serves: M5
- Deliverables: metrics, logs, alerts, and dashboard guidance for latency, failure rate, and audit lag.
- Acceptance criteria:
  - Metrics exist for request latency, failure rate, auth failures, state transition counts, and audit lag.
  - Alerts are configured for unusual spikes in mutation failures or audit backlog.
  - Operational guidance documents the expected response to a policy violation or stale data conflict.

### 11. Create verification fixtures and acceptance tests
- Serves: M1-M5
- Deliverables: test fixtures, regression cases, and documented manual compliance review steps.
- Acceptance criteria:
  - Happy path, edge case, and failure scenario tests cover card creation, state changes, transactions, disputes, and audit emission.
  - Verification evidence includes sample audit records and reconciliation output for at least one end-to-end scenario.

## Edge Cases and Failure Modes

| Scenario | Expected Behavior | Compliance / Audit Implication |
|---|---|---|
| Empty card state for a new user | Show an explicit empty-state message and no error. | No hidden state change; audit not required. |
| Invalid limit value such as zero or a value above policy maximum | Reject the request with a validation error and preserve the previous limit. | A failed validation attempt is logged as a rejected change attempt. |
| Concurrent freeze and limit update on the same card | Apply one operation at a time using optimistic concurrency and return a conflict if the version is stale. | The conflict is recorded and a follow-up action is required. |
| Duplicate create or dispute submission | Return the original result for the same idempotency key and avoid duplicate records. | Duplicate suppression is auditable through the original request identifier. |
| Stale transaction view during dispute submission | Return the latest committed transaction state or a conflict with a refresh hint. | The system prevents a dispute from being created against an outdated state. |
| Permission boundary violation | Return a 403-style authorization error and suppress any state mutation. | The denied action is captured in audit and monitoring telemetry. |
| Fraud-like pattern such as repeated rapid state changes | Block or require manual review and create a review event. | Clear evidence is preserved for fraud or ops review. |
| Partial dependency failure after a successful write attempt | Roll back the transaction or mark the operation as failed without leaving a half-applied state. | The audit trail reflects the final outcome rather than a silent inconsistency. |

## Verification Plan
- Unit tests:
  - State machine transitions, money validation, dispute eligibility rules, and authorization logic.
- Integration tests:
  - Persistence, event emission, idempotency, and optimistic concurrency behavior.
- End-to-end tests:
  - Customer card creation, limit change, freeze, transaction view, and dispute submission.
- Reconciliation checks:
  - Compare stored card state, audit event count, and dispute records on a daily or periodic basis.
- Manual compliance review:
  - Review a sample of frozen cards, dispute outcomes, and audit exports for completeness and redaction quality.

## Performance and Operational Expectations
- The feature is expected to support the stated latency and availability targets under ordinary load and to degrade gracefully under burst traffic.
- Assumed targets are reasonable for a regulated digital banking experience because users expect near-immediate control over card state, and operations teams need timely visibility into disputes and policy changes.
- If traffic exceeds the expected thresholds, the system should return explicit rate-limit or retry guidance rather than silently delaying the user.
