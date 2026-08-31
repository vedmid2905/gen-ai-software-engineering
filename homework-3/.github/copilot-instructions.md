# Copilot Instructions for Finance Feature Work

## Project Intent
- Follow the layered specification in this workspace before making implementation choices.
- Prefer clarity, explicitness, and traceability over cleverness.

## FinTech-Sensitive Defaults
- Treat all cardholder and account data as sensitive.
- Never log PAN, CVV, secret material, or full account numbers.
- Use idempotent writes for state-changing operations.
- Use explicit state machines for card lifecycle changes.
- Keep audit trails immutable and reviewable.

## Implementation Guidance
- Use integer-based monetary handling for all financial calculations.
- Validate business rules before persisting state.
- Prefer transactional writes that also produce audit evidence.
- Use optimistic concurrency control for conflicting updates.
- Keep authorization checks explicit and role-based.

## Quality Bar
- Add tests for happy paths, edge cases, and failure modes.
- Document any deviation from the stated latency, availability, or audit expectations.
- Avoid introducing silent fallback behavior for policy violations or suspicious activity.
