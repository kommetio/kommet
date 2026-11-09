# Record Import API

Status: Implemented.

## Common

Paths are relative to the application's context path. Send exactly one `Authorization: Bearer <token>` header and identify the target environment explicitly. The token must have write permission, and the token owner must have permission to create records and set the requested fields. Responses use `{ "success": true, "data": ... }` and `{ "success": false, "messages": ["..."] }` envelopes.

Set `BASE_URL` and `KOMMET_API_TOKEN` in Bash, WSL, or Git Bash before using the example:

```bash
BASE_URL='https://example.com/km-1.0'
```

## Upload Records

**Purpose:** Synchronously create records from CSV rows for the specified type in the selected environment.

**Endpoint:** `/api/dev/records/imports`

**HTTP method:** `POST`

**Parameters:** `multipart/form-data` fields: `envId` (required KID), `typeName` (required qualified type name), `file` (required CSV file), and `partialInsert` (optional; `0` by default, or `1`). Optional `mapping` is a JSON object mapping CSV header names to field API names. The CSV must be UTF-8 and include a header row; without `mapping`, headers are field API names.

**Expected payload:** Multipart form fields and a CSV file. No JSON request body.

Processing is synchronous: the request remains open until all rows have been processed. No background job is scheduled or created, and no polling is required. With `partialInsert=0`, any row failure rolls back the entire import. With `partialInsert=1`, valid rows are inserted while invalid rows are reported in `rowErrors`.

Example CSV:

```csv
invoiceNumber,amount
INV-1001,25.50
INV-1002,42.00
```

**Example response:** `200 OK`, returned after processing completes. `partialInsert=0` returns this response only when every row is inserted; if any row fails, the request returns `400` and no rows are inserted. `partialInsert=1` returns `200` with successful and failed row counts.

```json
{
  "success": true,
  "data": {
    "rowsReceived": 2,
    "recordsCreated": 2,
    "rowsFailed": 0,
    "rowErrors": []
  }
}
```

Rows that fail validation are reported in `rowErrors`; other valid rows may still be created. Each entry contains the CSV row identifier and error text:

```json
"rowErrors": [{ "rowId": 3, "error": "invalid field" }]
```

The response is the final result for the entire upload.

In atomic mode, a row failure returns `400` and includes the first failing row:

```json
{
  "success": false,
  "messages": ["Import failed; no records were inserted."],
  "data": {
    "rowsReceived": 2,
    "recordsCreated": 0,
    "rowsFailed": 2,
    "rowErrors": [{ "rowId": 3, "error": "invalid field" }]
  }
}
```

**Curl example:**

```bash
curl -sS -i "$BASE_URL/api/dev/records/imports" \
  -H "Authorization: Bearer $KOMMET_API_TOKEN" \
  -F 'envId=0010000000002' \
  -F 'typeName=acme.billing.Invoice' \
  -F 'file=@invoices.csv;type=text/csv'
```

Set `partialInsert=1` to keep valid rows when other rows fail:

```bash
curl -sS -i "$BASE_URL/api/dev/records/imports" \
  -H "Authorization: Bearer $KOMMET_API_TOKEN" \
  -F 'envId=0010000000002' \
  -F 'typeName=acme.billing.Invoice' \
  -F 'partialInsert=1' \
  -F 'file=@invoices.csv;type=text/csv'
```

**Error messages:** `400` missing parameters, invalid `partialInsert`, invalid field mapping, malformed CSV, or a row failure when `partialInsert=0`; `401` missing/invalid token; `403` insufficient write or field permissions; `404` environment or type not found; `413` upload exceeds the configured request limit. Row-level failures with `partialInsert=1` are returned in `rowErrors` in the completed response.
