# IDE Source API

Status: Implemented. The API reads and saves KOLL classes, KTL views, JavaScript resources, and CSS resources using environment-scoped API tokens.

## Common

All paths are relative to the application's context path. Requests must identify `envId`; GET endpoints pass it in the query string and POST endpoints use `multipart/form-data`. Do not infer the environment from a browser session.

Send exactly one `Authorization: Bearer <token>` header. GET endpoints require a read-capable token and POST endpoints require a write-capable token. The token owner must have a root or system-administrator profile. Tokens are environment-scoped. Responses use JSON envelopes: success is `{ "success": true, "data": ... }`; errors are `{ "success": false, "messages": ["..."] }`.

Set `BASE_URL` and `KOMMET_API_TOKEN` in Bash, WSL, or Git Bash before using the examples:

```bash
BASE_URL='https://example.com/km-1.0'
```

## List IDE Files

**Purpose:** List editable source files of the requested kind. Each entry contains the simple file name, package, and Kommet ID. JavaScript and CSS resource names retain their file extension when present; their package is derived from the dotted resource name.

**Endpoints:**

| File kind | Endpoint |
|---|---|
| KOLL classes | `/api/ide/class/list` |
| KTL views | `/api/ide/view/list` |
| JavaScript resources | `/api/ide/js/list` |
| CSS resources | `/api/ide/css/list` |

**HTTP method:** `GET`

**Parameters:** Query parameter `envId`.

**Expected payload:** None.

**Example response:** Successful list responses are a JSON array at the root, not the usual `{ "success": true, "data": ... }` envelope. Errors continue to use the standard error envelope.

```json
[
  {
    "name": "InvoiceService",
    "package": "acme.billing",
    "id": "0010000000002"
  }
]
```

**Curl examples:**

```bash
curl -sS -i --get "$BASE_URL/api/ide/class/list" \
  --data-urlencode 'envId=0010000000002' \
  -H "Authorization: Bearer $KOMMET_API_TOKEN"

curl -sS -i --get "$BASE_URL/api/ide/view/list" \
  --data-urlencode 'envId=0010000000002' \
  -H "Authorization: Bearer $KOMMET_API_TOKEN"

curl -sS -i --get "$BASE_URL/api/ide/js/list" \
  --data-urlencode 'envId=0010000000002' \
  -H "Authorization: Bearer $KOMMET_API_TOKEN"

curl -sS -i --get "$BASE_URL/api/ide/css/list" \
  --data-urlencode 'envId=0010000000002' \
  -H "Authorization: Bearer $KOMMET_API_TOKEN"
```

**Error messages:** `400` missing or invalid environment ID; `401` missing/invalid token; `403` missing read permission or root/system-administrator profile; `404` environment not found.

## Read KOLL Class

**Purpose:** Return the source code of one editable KOLL class.

**Endpoint:** `/api/dev/class/get`

**HTTP method:** `GET`

**Parameters:** Query parameters `envId` and `qualifiedName`, such as `acme.billing.InvoiceService`.

**Expected payload:** None.

**Example response:**

```json
{
  "success": true,
  "data": {
    "fileId": "0010000000002",
    "name": "acme.billing.InvoiceService",
    "code": "package acme.billing;\npublic class InvoiceService {\n}"
  }
}
```

**Curl example:**

```bash
curl -sS -i --get "$BASE_URL/api/dev/class/get" \
  --data-urlencode 'envId=0010000000002' \
  --data-urlencode 'qualifiedName=acme.billing.InvoiceService' \
  -H "Authorization: Bearer $KOMMET_API_TOKEN"
```

**Error messages:** `400` missing environment or qualified name; `401` missing/invalid token; `403` missing read permission, profile, or editable-file access; `404` environment or class not found.

## Save KOLL Class

**Purpose:** Create a KOLL class or replace the source code of an editable class. For updates, `fileId` identifies the existing class; when omitted, the class identity is taken from the source code.

**Endpoint:** `/api/dev/class/save`

**HTTP method:** `POST`

**Parameters:** Multipart form fields `envId` and optional `fileId`; the source is the required `file` part and is decoded as UTF-8. Omit `fileId` to create a class; its identity is taken from the source code.

**Example response:**

```json
{
  "success": true,
  "data": {
    "fileId": "0010000000003",
    "name": "acme.billing.InvoiceService",
    "operation": "updated"
  }
}
```

**Curl example:**

```bash
curl -sS -i "$BASE_URL/api/dev/class/save" \
  -H "Authorization: Bearer $KOMMET_API_TOKEN" \
  -F 'envId=0010000000002' \
  -F 'fileId=0010000000003' \
  -F 'file=@InvoiceService.koll;type=text/plain'
```

**Error messages:** `400` missing multipart file, invalid/blank code, or compilation/save failure; `401` missing/invalid token; `403` missing write permission, profile, or editable-file access; `404` environment or class not found.

## Read KTL View

**Purpose:** Return the source code of one editable KTL view.

**Endpoint:** `/api/dev/view/get`

**HTTP method:** `GET`

**Parameters:** Query parameters `envId` and `qualifiedName`, such as `acme.billing.InvoiceDetails`.

**Expected payload:** None.

**Example response:**

```json
{
  "success": true,
  "data": {
    "fileId": "0020000000001",
    "name": "acme.billing.InvoiceDetails",
    "code": "<view name=\"InvoiceDetails\">...</view>"
  }
}
```

**Curl example:**

```bash
curl -sS -i --get "$BASE_URL/api/dev/view/get" \
  --data-urlencode 'envId=0010000000002' \
  --data-urlencode 'qualifiedName=acme.billing.InvoiceDetails' \
  -H "Authorization: Bearer $KOMMET_API_TOKEN"
```

**Error messages:** `400` missing environment or qualified name; `401` missing/invalid token; `403` missing read permission, profile, or editable-file access; `404` environment or view not found.

## Save KTL View

**Purpose:** Create a KTL view or replace the source code of an editable view. For updates, `fileId` identifies the existing view; when omitted, view identity is taken from the source code.

**Endpoint:** `/api/dev/view/save`

**HTTP method:** `POST`

**Parameters:** Multipart form fields `envId` and optional `fileId`; the source is the required `file` part and is decoded as UTF-8. Omit `fileId` to create a view; its identity is taken from the source code.

**Example response:**

```json
{
  "success": true,
  "data": {
    "fileId": "0020000000001",
    "name": "acme.billing.InvoiceDetails",
    "operation": "updated"
  }
}
```

**Curl example:**

```bash
curl -sS -i "$BASE_URL/api/dev/view/save" \
  -H "Authorization: Bearer $KOMMET_API_TOKEN" \
  -F 'envId=0010000000002' \
  -F 'fileId=0020000000001' \
  -F 'file=@InvoiceDetails.ktl;type=text/plain'
```

**Error messages:** `400` missing multipart file, invalid/blank code, or view validation/save failure; `401` missing/invalid token; `403` missing write permission, profile, or editable-file access; `404` environment or view not found.

## Read JavaScript Resource

**Purpose:** Return one JavaScript view resource by its exact resource name.

**Endpoint:** `/api/dev/js/get`

**HTTP method:** `GET`

**Parameters:** Query parameters `envId` and `name`.

**Expected payload:** None.

**Example response:**

```json
{
  "success": true,
  "data": {
    "fileId": "0030000000001",
    "name": "acme.billing.invoice-editor.js",
    "code": "function saveInvoice() {}",
    "mimeType": "application/javascript"
  }
}
```

**Curl example:**

```bash
curl -sS -i --get "$BASE_URL/api/dev/js/get" \
  --data-urlencode 'envId=0010000000002' \
  --data-urlencode 'name=acme.billing.invoice-editor.js' \
  -H "Authorization: Bearer $KOMMET_API_TOKEN"
```

**Error messages:** `400` missing environment or resource name; `401` missing/invalid token; `403` missing read permission or profile; `404` environment or JavaScript resource not found.

## Save JavaScript Resource

**Purpose:** Create or update a JavaScript view resource. Existing resources are identified by `fileId`; new resources require `name`. The endpoint assigns `application/javascript` as the MIME type for new resources.

**Endpoint:** `/api/dev/js/save`

**HTTP method:** `POST`

**Parameters:** Multipart form fields `envId` and optional `fileId` or `name`; the resource content is the required `file` part and is decoded as UTF-8. Supply `fileId` to update or `name` to create. Empty resource files are allowed.

**Example response:**

```json
{
  "success": true,
  "data": {
    "fileId": "0030000000001",
    "name": "acme.billing.invoice-editor.js",
    "mimeType": "application/javascript",
    "operation": "updated"
  }
}
```

**Curl example:**

```bash
curl -sS -i "$BASE_URL/api/dev/js/save" \
  -H "Authorization: Bearer $KOMMET_API_TOKEN" \
  -F 'envId=0010000000002' \
  -F 'name=acme.billing.new-resource.js' \
  -F 'file=@new-resource.js;type=text/javascript'
```

**Error messages:** `400` missing multipart file, missing name when creating, or attempt to change a resource name; `401` missing/invalid token; `403` missing write permission or profile; `404` environment or JavaScript resource not found.

## Read CSS Resource

**Purpose:** Return one CSS view resource by its exact resource name.

**Endpoint:** `/api/dev/css/get`

**HTTP method:** `GET`

**Parameters:** Query parameters `envId` and `name`.

**Expected payload:** None.

**Example response:**

```json
{
  "success": true,
  "data": {
    "fileId": "0030000000002",
    "name": "acme.billing.invoice-editor.css",
    "code": ".invoice { color: #333; }",
    "mimeType": "text/css"
  }
}
```

**Curl example:**

```bash
curl -sS -i --get "$BASE_URL/api/dev/css/get" \
  --data-urlencode 'envId=0010000000002' \
  --data-urlencode 'name=acme.billing.invoice-editor.css' \
  -H "Authorization: Bearer $KOMMET_API_TOKEN"
```

**Error messages:** `400` missing environment or resource name; `401` missing/invalid token; `403` missing read permission or profile; `404` environment or CSS resource not found.

## Save CSS Resource

**Purpose:** Create or update a CSS view resource. Existing resources are identified by `fileId`; new resources require `name`. The endpoint assigns `text/css` as the MIME type for new resources.

**Endpoint:** `/api/dev/css/save`

**HTTP method:** `POST`

**Parameters:** Multipart form fields `envId` and optional `fileId` or `name`; the resource content is the required `file` part and is decoded as UTF-8. Supply `fileId` to update or `name` to create. Empty resource files are allowed.

**Example response:**

```json
{
  "success": true,
  "data": {
    "fileId": "0030000000002",
    "name": "acme.billing.invoice-editor.css",
    "mimeType": "text/css",
    "operation": "updated"
  }
}
```

**Curl example:**

```bash
curl -sS -i "$BASE_URL/api/dev/css/save" \
  -H "Authorization: Bearer $KOMMET_API_TOKEN" \
  -F 'envId=0010000000002' \
  -F 'name=acme.billing.new-resource.css' \
  -F 'file=@new-resource.css;type=text/css'
```

**Error messages:** `400` missing multipart file, missing name when creating, or attempt to change a resource name; `401` missing/invalid token; `403` missing write permission or profile; `404` environment or CSS resource not found.

## Errors

Errors use `{ "success": false, "messages": ["..."] }`. Missing, revoked, expired, or wrong-environment tokens return `401` with `WWW-Authenticate: Bearer`. System classes and views are not exposed or editable through this API. Resource type is determined by the resource MIME type or its `.js` / `.css` name suffix. The HTTPS check is currently disabled in `RestAuthorizationUtil`; `kommet.devapi.allowLoopbackHttp` is intended for local development.
