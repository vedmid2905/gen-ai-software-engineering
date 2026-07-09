# Project Agents Summary

## Overview
This project implements a multi-agent banking transaction processing pipeline that transforms raw transaction data into validated, risk-scored, and auditable processing outcomes.

## Agent Roles
- Transaction Validator Agent: validates required fields, monetary values, and currency codes.
- Fraud Detector Agent: scores transactions for suspicious activity using amount, timing, and geography heuristics.
- Compliance and Settlement Agent: applies final policy decisions and determines whether a transaction is approved, flagged, or rejected.
- Reporting Agent: aggregates results into a summary report and writes final outputs to shared/results/.

## Project-Specific Context
The pipeline consumes sample-transactions.json from the repository root and processes each transaction through a shared-directory message protocol. The system must preserve auditability and produce structured JSON outputs suitable for tests, MCP integration, and manual review.
