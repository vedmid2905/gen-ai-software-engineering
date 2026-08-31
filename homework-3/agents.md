# Agent Guidance for Virtual Card Specification Implementation

## Purpose
- This document defines how an AI coding partner should behave when implementing the virtual card control feature described in the specification.

## Assumptions
- The implementation will be a backend-oriented finance feature with authentication, authorization, persistence, and audit/event emission.
- The stack may use a REST or event-driven service architecture; the agent should not assume a specific framework unless the implementation plan explicitly requires one.

## Domain Rules
- Treat financial data as sensitive by default.
- Never log, print, or expose full card numbers, CVV values, or secrets.
- Prefer tokenized or masked references for all customer-visible data.
- Use explicit state transitions and reject invalid transitions rather than trying to infer intent.
- Use idempotency for all state-changing operations to make retries safe.

## Security and Compliance Expectations
- Enforce least privilege for customer and internal roles.
- Preserve audit evidence for every material state change.
- Avoid storing sensitive data in logs, traces, or debugging output.
- Validate all input carefully, especially amount values, state transitions, and dispute eligibility.
- Keep policy rules explicit and configurable rather than hard-coded in multiple places.

## Testing and Verification Expectations
- Implement unit tests for state machine rules, authorization checks, dispute eligibility, and money handling.
- Add integration tests for persistence, event emission, and optimistic concurrency.
- Add end-to-end tests for customer-visible flows and internal review flows.
- Include fixtures for empty states, invalid inputs, duplicate requests, and stale data conflicts.

## Implementation Conventions
- Use integers for monetary values in minor units.
- Use opaque, non-sensitive IDs for cards and disputes.
- Favor deterministic ordering and pagination for transaction and dispute lists.
- Keep error messages actionable and specific without exposing internal secrets.
- Make audit events immutable and traceable through correlation identifiers.

## Edge Cases the Agent Must Handle
- Duplicate requests and retries
- Concurrent updates to the same card
- Invalid or out-of-policy limit changes
- Attempted access across role boundaries
- Dispute submission after the eligibility window closes
- Partial failures during mutation and event publication

## Output Expectations
- The implementation should remain aligned with the layered specification and should not silently change business behavior without updating the spec or documentation.
- Any deviation from the stated non-functional targets should be documented and justified.
