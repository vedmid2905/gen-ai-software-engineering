# API Reference

## Base URL

- Local development: http://localhost:8080

## Authentication

No authentication is required for the current MVP.

## Endpoints

### Create a ticket

POST /tickets

Request body:

```json
{
  "customer_email": "jane@example.com",
  "customer_name": "Jane Doe",
  "subject": "Cannot access my account",
  "description": "I cannot sign in because my password was reset and I still cannot access my account.",
  "category": "account_access",
  "priority": "urgent",
  "status": "new",
  "assigned_to": "support-team",
  "tags": ["login", "security"],
  "source": "web_form",
  "browser": "Chrome",
  "device_type": "desktop"
}
```

Example:

```bash
curl -X POST http://localhost:8080/tickets \
  -H "Content-Type: application/json" \
  -d '{"customer_email":"jane@example.com","customer_name":"Jane Doe","subject":"Cannot access my account","description":"I cannot sign in because my password was reset and I still cannot access my account.","category":"account_access","priority":"urgent","status":"new","assigned_to":"support-team","tags":["login","security"],"source":"web_form","browser":"Chrome","device_type":"desktop"}'
```

### List tickets

GET /tickets?category=account_access&priority=urgent&status=new

Example:

```bash
curl "http://localhost:8080/tickets?category=account_access&priority=urgent&status=new"
```

### Get one ticket

GET /tickets/{id}

Example:

```bash
curl http://localhost:8080/tickets/123e4567-e89b-12d3-a456-426614174000
```

### Update a ticket

PUT /tickets/{id}

Example:

```bash
curl -X PUT http://localhost:8080/tickets/123e4567-e89b-12d3-a456-426614174000 \
  -H "Content-Type: application/json" \
  -d '{"status":"in_progress","assigned_to":"agent-42"}'
```

### Delete a ticket

DELETE /tickets/{id}

Example:

```bash
curl -X DELETE http://localhost:8080/tickets/123e4567-e89b-12d3-a456-426614174000
```

### Bulk import

POST /tickets/import

Example:

```bash
curl -X POST http://localhost:8080/tickets/import \
  -F "file=@demo/sample_tickets.csv"
```

### Auto-classify a ticket

POST /tickets/{id}/auto-classify

Example:

```bash
curl -X POST http://localhost:8080/tickets/123e4567-e89b-12d3-a456-426614174000/auto-classify
```

## Response envelopes

### Success response

```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "status": "new",
  "created_at": "2026-07-05T20:00:00Z"
}
```

### Error response

```json
{
  "message": "Ticket 123e4567-e89b-12d3-a456-426614174000 not found"
}
```

## Supported enums

- Category: account_access, technical_issue, billing_question, feature_request, bug_report, other
- Priority: urgent, high, medium, low
- Status: new, in_progress, waiting_customer, resolved, closed
- Source: web_form, email, api, chat, phone
- Device type: desktop, mobile, tablet
