# Database Structure API

Status: Implemented. Database-backed tests need the configured PostgreSQL fixture; live Tomcat validation is pending.

## Common

All paths are relative to the application's context path. Requests must identify `envId`; GET endpoints pass it in the query string and POST endpoints in JSON. Do not infer the environment from a browser session.

Send exactly one `Authorization: Bearer <token>` header. GET requires a read-capable token. POST requires a write-capable token owned by a root or system administrator. Tokens are environment-scoped. Responses use JSON envelopes: success is `{ "success": true, "data": ... }`; errors are `{ "success": false, "messages": ["..."] }`.

Set `BASE_URL` and `KOMMET_API_TOKEN` in Bash, WSL, or Git Bash before using the examples:

```bash
BASE_URL='https://example.com/km-1.0'
```

## Read Type

**Purpose:** Return one mutable custom type and its fields using the documented type and field structures.

**Endpoint:** `/api/dev/type`

**HTTP method:** `GET`

**Parameters:** Query parameters `envId` and `qualifiedName`, such as `acme.billing.Invoice`.

**Expected payload:** None.

**Example response:**

```json
{
  "success": true,
  "data": {
    "package": "acme.billing",
    "apiName": "Invoice",
    "label": "Invoice",
    "pluralLabel": "Invoices",
    "description": null,
    "labelKey": null,
    "pluralLabelKey": null,
    "defaultFieldApiName": null,
    "sharingControlledByFieldApiName": null,
    "combineRecordAndCascadeSharing": false,
    "fields": []
  }
}
```

**Curl example:**

```bash
curl -sS -i --get "$BASE_URL/api/dev/type" \
  --data-urlencode 'envId=0010000000002' \
  --data-urlencode 'qualifiedName=acme.billing.Invoice' \
  -H "Authorization: Bearer $KOMMET_API_TOKEN"
```

**Error messages:** `400` missing environment or qualified name; `401` missing/invalid token; `403` missing read permission; `404` environment or type not found.

## Read Field

**Purpose:** Return one field definition by its fully qualified type-and-field name.

**Endpoint:** `/api/dev/field`

**HTTP method:** `GET`

**Parameters:** Query parameters `envId` and `qualifiedName`, such as `acme.billing.Invoice.invoiceNumber`.

**Expected payload:** None.

**Example response:**

```json
{
  "success": true,
  "data": {
    "apiName": "invoiceNumber",
    "label": "Invoice number",
    "description": null,
    "labelKey": null,
    "required": true,
    "trackHistory": false,
    "defaultValue": null,
    "unique": false,
    "isDefault": false,
    "dataType": { "kind": "text", "length": 80, "long": false, "formatted": false }
  }
}
```

**Curl example:**

```bash
curl -sS -i --get "$BASE_URL/api/dev/field" \
  --data-urlencode 'envId=0010000000002' \
  --data-urlencode 'qualifiedName=acme.billing.Invoice.invoiceNumber' \
  -H "Authorization: Bearer $KOMMET_API_TOKEN"
```

**Error messages:** `400` missing environment or malformed qualified name; `401` missing/invalid token; `403` missing read permission; `404` environment, type, or field not found.

## List Types

**Purpose:** List mutable custom types, optionally including their fields.

**Endpoint:** `/api/dev/types`

**HTTP method:** `GET`

**Parameters:** Query parameter `envId`; optional `includeField=1` includes fields. Fields are omitted otherwise.

**Expected payload:** None.

**Example response:**

```json
{
  "success": true,
  "data": [
    {
      "package": "acme.billing",
      "apiName": "Invoice",
      "label": "Invoice",
      "pluralLabel": "Invoices",
      "description": null,
      "labelKey": null,
      "pluralLabelKey": null,
      "defaultFieldApiName": null,
      "sharingControlledByFieldApiName": null,
      "combineRecordAndCascadeSharing": false
    }
  ]
}
```

With `includeField=1`, each type includes its fields:

```json
{
  "success": true,
  "data": [
    {
      "package": "acme.billing",
      "apiName": "Invoice",
      "label": "Invoice",
      "pluralLabel": "Invoices",
      "description": null,
      "labelKey": null,
      "pluralLabelKey": null,
      "defaultFieldApiName": null,
      "sharingControlledByFieldApiName": null,
      "combineRecordAndCascadeSharing": false,
      "fields": [
        {
          "apiName": "invoiceNumber",
          "label": "Invoice number",
          "description": null,
          "labelKey": null,
          "required": true,
          "trackHistory": false,
          "defaultValue": null,
          "unique": true,
          "isDefault": false,
          "dataType": { "kind": "text", "length": 80, "long": false, "formatted": false }
        }
      ]
    }
  ]
}
```

**Curl example:**

```bash
curl -sS -i --get "$BASE_URL/api/dev/types" \
  --data-urlencode 'envId=0010000000002' \
  -H "Authorization: Bearer $KOMMET_API_TOKEN"
```

**Error messages:** `400` missing/invalid environment ID; `401` missing/invalid token; `403` missing read permission; `404` environment not found.

## Save Type

**Purpose:** Create a custom type or update its metadata without replacing its fields.

**Endpoint:** `/api/dev/type/save`

**HTTP method:** `POST`

**Parameters:** JSON body. Type identity is `(envId, type.package, type.apiName)`; names are stable and are not renamed by an upsert.

**Expected payload:** `package`, `apiName`, `label`, and `pluralLabel` are required. Other metadata is optional.

```json
{
  "envId": "0010000000002",
  "type": {
    "package": "acme.billing",
    "apiName": "Invoice",
    "label": "Invoice",
    "pluralLabel": "Invoices",
    "description": "Customer invoice",
    "labelKey": null,
    "pluralLabelKey": null,
    "defaultFieldApiName": null,
    "sharingControlledByFieldApiName": null,
    "combineRecordAndCascadeSharing": false
  }
}
```

**Example response:**

```json
{
  "success": true,
  "data": {
    "operation": "created",
    "typeId": "0010000000002",
    "keyPrefix": "a01",
    "qualifiedName": "acme.billing.Invoice"
  }
}
```

**Curl example:**

```bash
curl -sS -i "$BASE_URL/api/dev/type/save" \
  -H "Authorization: Bearer $KOMMET_API_TOKEN" \
  -H 'Content-Type: application/json' \
  --data '{"envId":"0010000000002","type":{"package":"acme.billing","apiName":"Invoice","label":"Invoice","pluralLabel":"Invoices"}}'
```

**Error messages:** `400` malformed JSON or invalid type definition; `401` missing/invalid token; `403` missing write permission or root/system-administrator profile; `404` environment or referenced field not found.

## Save Field

**Purpose:** Create a field on an existing custom type or update its compatible metadata.

**Endpoint:** `/api/dev/field/save`

**HTTP method:** `POST`

**Parameters:** JSON body. Field identity is `(envId, type.package, type.apiName, field.apiName)`; field API names are stable.

**Expected payload:** `type.package`, `type.apiName`, `field.apiName`, `field.label`, and `field.dataType` are required. The server generates database columns; do not send `dbColumn`.

```json
{
  "envId": "0010000000002",
  "type": { "package": "acme.billing", "apiName": "Invoice" },
  "field": {
    "apiName": "invoiceNumber",
    "label": "Invoice number",
    "description": "Customer-facing invoice number",
    "labelKey": null,
    "required": true,
    "trackHistory": false,
    "defaultValue": null,
    "unique": true,
    "isDefault": false,
    "dataType": { "kind": "text", "length": 80, "long": false, "formatted": false }
  }
}
```

Supported `dataType.kind` values and options: `text` (`length`, `long`, `formatted`); `number` (`decimalPlaces`, `javaType`); `enumeration` (`values` or `dictionaryId`, optional `validateValues`); `multiEnumeration` (`values`); `autoNumber` (`format`); `typeReference` (`targetType`, optional `cascadeDelete`); `formula` (`formula`, `returnType`); `inverseCollection` (`inverseType`, `inverseProperty`); `association` (`mode` and matching type/field references). `boolean`, `date`, `datetime`, `email`, `kid`, and `blob` have no additional options.

**Example response:**

```json
{
  "success": true,
  "data": {
    "operation": "created",
    "fieldId": "0030000000001",
    "typeId": "0010000000002",
    "typeKeyPrefix": "a01",
    "apiName": "invoiceNumber",
    "dataType": { "kind": "text" }
  }
}
```

**Curl example:**

```bash
curl -sS -i "$BASE_URL/api/dev/field/save" \
  -H "Authorization: Bearer $KOMMET_API_TOKEN" \
  -H 'Content-Type: application/json' \
  --data '{"envId":"0010000000002","type":{"package":"acme.billing","apiName":"Invoice"},"field":{"apiName":"invoiceNumber","label":"Invoice number","dataType":{"kind":"text","length":80}}}'
```

**Error messages:** `400` malformed JSON or invalid field definition; `401` missing/invalid token; `403` missing write permission, profile, or mutable-type access; `404` environment, type, or reference not found; `409` incompatible datatype/storage change.

## Errors

Errors use `{ "success": false, "messages": ["..."] }`. Missing, revoked, expired, or wrong-environment tokens return `401` with `WWW-Authenticate: Bearer`. Tokens are environment-scoped, secrets are not logged, and these endpoints do not use browser sessions. Only mutable custom types are exposed; built-in and code-declared types cannot be changed through the API. The HTTPS check is currently disabled in `RestAuthorizationUtil`; `kommet.devapi.allowLoopbackHttp` is intended for local development.


