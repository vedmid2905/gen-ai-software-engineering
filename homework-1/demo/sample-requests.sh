#!/usr/bin/env bash
# Sample API calls for the Banking Transactions API.
# The server must already be running on http://localhost:8080 (see ../HOWTORUN.md).

set -e
BASE_URL="http://localhost:8080"

echo "== Create a deposit =="
curl -s -X POST "$BASE_URL/transactions" \
  -H "Content-Type: application/json" \
  -d '{"type":"deposit","toAccount":"ACC-12345","amount":500.00,"currency":"USD"}'
echo -e "\n"

echo "== Create a withdrawal =="
curl -s -X POST "$BASE_URL/transactions" \
  -H "Content-Type: application/json" \
  -d '{"type":"withdrawal","fromAccount":"ACC-12345","amount":75.25,"currency":"USD"}'
echo -e "\n"

echo "== Create a transfer =="
curl -s -X POST "$BASE_URL/transactions" \
  -H "Content-Type: application/json" \
  -d '{"type":"transfer","fromAccount":"ACC-12345","toAccount":"ACC-67890","amount":100.50,"currency":"USD"}'
echo -e "\n"

echo "== Validation error example (negative amount, unsupported currency) =="
curl -s -X POST "$BASE_URL/transactions" \
  -H "Content-Type: application/json" \
  -d '{"type":"deposit","toAccount":"ACC-12345","amount":-5,"currency":"XXX"}'
echo -e "\n"

echo "== List all transactions =="
curl -s "$BASE_URL/transactions"
echo -e "\n"

echo "== Filter by account =="
curl -s "$BASE_URL/transactions?accountId=ACC-12345"
echo -e "\n"

echo "== Filter by type =="
curl -s "$BASE_URL/transactions?type=deposit"
echo -e "\n"

echo "== Get account balance =="
curl -s "$BASE_URL/accounts/ACC-12345/balance"
echo -e "\n"

echo "== Get account summary =="
curl -s "$BASE_URL/accounts/ACC-12345/summary"
echo -e "\n"

echo "== Get account balance, malformed id -> 400 =="
curl -s -w " [%{http_code}]\n" "$BASE_URL/accounts/bad-id/balance"

echo "== Get account balance, unknown account -> 404 =="
curl -s -w " [%{http_code}]\n" "$BASE_URL/accounts/ACC-99999/balance"
