# Database Structure API Proposal

Status: Implemented. Database-backed acceptance tests require the configured PostgreSQL fixture; live Tomcat validation is pending.

## Purpose

Provide authenticated API operations for creating or updating custom Kommet types and fields. These operations change an environment's schema, so they must reuse the existing `DataService` persistence and validation paths rather than write database DDL directly.

The API is intended for trusted developer automation. It is not a replacement for record CRUD APIs and does not expose operations on built-in or code-declared types.

## Endpoints

Both endpoints accept and return JSON and require `Content-Type: application/json`.

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/dev/type/save` | Create or update a custom type. |
| `POST` | `/api/dev/field/save` | Create or update one field on an existing custom type. |

Paths are relative to the deployed application's context path. The second path includes the leading slash: `/api/dev/field/save`.

Each request includes an `environmentId` KID. The endpoint must not infer the target environment from an HTTP session or cookie.

## Authentication

Require exactly one `Authorization: Bearer <token>` header on both endpoints. Tokens are the secrets issued for the built-in `kommet.basic.ApiToken` type; the legacy OAuth token store and configured developer secrets are not used.

- `RestAuthenticationService` is a reusable, request-local authenticator. It validates the stored hash and expiration through `ApiTokenService`, loads the token's system user, rejects inactive users, and initializes the user's normal permissions and cascade settings.
- Both schema endpoints require `isWritePermission`. Schema editing also requires the owner's root or system-administrator profile, matching the existing schema-editing controllers. Token capabilities do not grant that profile or substitute a root identity.
- The request's explicit `environmentId` determines the target environment. Token lookup occurs only in that environment's database. No browser session or cookie is used or created, and no session fallback occurs on authentication failure.
- A missing, revoked, expired, unknown, or wrong-environment token receives `401 Unauthorized` with `WWW-Authenticate: Bearer`. A valid token with insufficient capabilities or profile receives `403 Forbidden`. Tokens are environment-scoped database entries, not a global token allowlist; a token existing only in another environment is indistinguishable from an invalid token in the requested environment.
- Secrets are never logged or included in errors. Rotate tokens by issuing a replacement and revoking the old record.
- HTTPS is required by default. For local development only, `kommet.devapi.allowLoopbackHttp=true` allows HTTP from a loopback peer. Keep it disabled in production, including behind a reverse proxy; configure trusted proxy/Tomcat settings so `request.isSecure()` reflects HTTPS correctly.
- Authentication handling for `@Rest`-annotated methods is unchanged. Only the two exact developer endpoint paths bypass the legacy request filter and use this controller's bearer authentication.

## Curl Examples

Run in Bash, WSL, or Git Bash. Set `KOMMET_API_TOKEN` securely in your environment to a write-capable token owned by a root/system-administrator user. Replace the base URL and `environmentId` with your deployment and the token's environment.

Create or update a type:

```bash
BASE_URL='https://example.com/km-1.0'
curl -sS -i "$BASE_URL/api/dev/type/save" \
  -H "Authorization: Bearer $KOMMET_API_TOKEN" \
  -H 'Content-Type: application/json' \
  --data '{"environmentId":"0010000000002","type":{"package":"acme.billing","apiName":"Invoice","label":"Invoice","pluralLabel":"Invoices"}}'
```

After the type exists, create or update a field:

```bash
curl -sS -i "$BASE_URL/api/dev/field/save" \
  -H "Authorization: Bearer $KOMMET_API_TOKEN" \
  -H 'Content-Type: application/json' \
  --data '{"environmentId":"0010000000002","type":{"package":"acme.billing","apiName":"Invoice"},"field":{"apiName":"invoiceNumber","label":"Invoice number","dataType":{"kind":"text","length":80}}}'
```

`--data` selects POST. Successful creation returns `201`; subsequent saves of the same identity return `200`.

## Type Save Payload

Type identity is `(environmentId, package, apiName)`. The pair `package` and `apiName` forms the stable qualified name. If that identity is absent, create a custom type. If it already exists, update mutable metadata. Do not use a supplied label as identity and do not implicitly rename an existing type.

Example:

```json
{
  "environmentId": "0010000000002",
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

Required type properties are `package`, `apiName`, `label`, and `pluralLabel`. `description`, label keys, and sharing configuration are optional. `defaultFieldApiName` and `sharingControlledByFieldApiName`, when supplied, must name fields already present on that type; for a new type, create the fields first and then save the type metadata again if necessary.

The type endpoint does not accept nested fields. This keeps type and field validation explicit and makes each request independently transactional.

## Field Save Payload

Field identity is `(environmentId, type.package, type.apiName, field.apiName)`. The target type must already exist and be a mutable custom type. An absent field identity creates a field; an existing identity updates it. Field API names are stable identifiers and cannot be changed through this upsert operation.

Example:

```json
{
  "environmentId": "0010000000002",
  "type": {
    "package": "acme.billing",
    "apiName": "Invoice"
  },
  "field": {
    "apiName": "invoiceNumber",
    "label": "Invoice number",
    "description": "Unique customer-facing invoice number",
    "labelKey": null,
    "required": true,
    "trackHistory": false,
    "defaultValue": null,
    "unique": true,
    "isDefault": false,
    "dataType": {
      "kind": "text",
      "length": 80,
      "long": false,
      "formatted": false
    }
  }
}
```

Required field properties are `apiName`, `label`, and `dataType`. Optional metadata includes `description`, `labelKey`, `required`, `trackHistory`, `defaultValue`, `unique`, and `isDefault`. The server generates database column names; callers must not supply a `dbColumn`.

### Data Type Union

`dataType.kind` is a case-sensitive discriminator. Only the options for that kind are allowed; unknown options and kinds are rejected rather than ignored.

| `kind` | Additional properties |
| --- | --- |
| `text` | `length` (positive integer, subject to the environment's maximum), `long` (boolean, default `false`), `formatted` (boolean, default `false`). |
| `number` | `decimalPlaces` (non-negative integer), `javaType` (`java.math.BigDecimal`, `java.lang.Double`, `java.lang.Integer`, or `java.lang.Long`). |
| `boolean`, `date`, `datetime`, `email`, `kid`, `blob` | No additional properties. |
| `enumeration` | `values` (array of strings) or `dictionaryId` (KID); `validateValues` (boolean, default `false`). Supply at least one of `values` or `dictionaryId`. |
| `multiEnumeration` | `values` (array of strings). |
| `autoNumber` | `format` (required; for example, `INV-{000000}`). |
| `typeReference` | `targetType` as `{ "package": "...", "apiName": "..." }`; optional `cascadeDelete` (boolean, default `false`). The target type must exist in the same environment. |
| `formula` | `formula` and `returnType` (`TEXT`, `NUMBER`, or `DATETIME`). Formula validation occurs against the saved target type. |
| `inverseCollection` | `inverseType` as `{ "package": "...", "apiName": "..." }` and `inverseProperty` (field API name on the inverse type). |
| `association` | `mode` and relationship references. For `mode: "direct"`, supply `associatedType`. For `mode: "linkingType"`, supply `linkingType`, `selfLinkingFieldApiName`, and `foreignLinkingFieldApiName`. All referenced objects must exist in the same environment. |

The shared Formula engine is unchanged. Its parser currently supports only `TEXT` formulas with text-field concatenation and double-quoted literals. The API returns `400` for `NUMBER` and `DATETIME` formulas, and rejects apostrophes because the existing text parser does not escape them safely. Direct associations create their linking type through `DataService.createLinkingType` in the field-save transaction.

For `enumeration`, values are arrays in JSON and are converted to the model's newline-separated representation. `multiEnumeration` values remain a set of strings. Dictionary, referenced-type, inverse-field, and association references are resolved by the server; clients should not send internal numeric database IDs.

Example reference field:

```json
{
  "kind": "typeReference",
  "targetType": {
    "package": "acme.crm",
    "apiName": "Customer"
  },
  "cascadeDelete": false
}
```

## Upsert and Schema Change Rules

- Type upsert is keyed by its qualified name within the selected environment. Field upsert is keyed by owner type plus field API name. Repeating an identical save is idempotent.
- A type's qualified name and a field's API name are immutable through these endpoints. Renames require a separate explicit operation because they affect references and generated metadata.
- Existing type metadata may be updated without replacing its fields.
- Existing field metadata may be updated when the change does not require unsafe data conversion. Changes to datatype kind, reference target, formula semantics, or storage shape must be rejected with `409 Conflict` unless a separately specified migration strategy can safely convert existing data.
- Changes to `required`, `unique`, defaults, or enumeration choices must be validated against existing records before commit. Validation failure leaves the schema unchanged.
- `isDefault` allows at most one default field per type. `unique: true` creates or updates the corresponding unique-check definition as part of the same transaction.
- A new type save and subsequent field saves are separate transactions. A failed field save does not roll back an earlier type save.
- Built-in types, declared-in-code types, system fields, and reserved API names cannot be modified through the developer API.

## Responses

Return the existing REST envelope shape. A successful response identifies whether the operation created or updated the definition:

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

Field saves return `operation`, `fieldId`, `typeId`, `typeKeyPrefix`, `apiName`, and `dataType.kind` in `data`.

Errors use the existing envelope:

```json
{
  "success": false,
  "messages": ["A concise validation or authorization error"]
}
```

Proposed status codes are `200` for update, `201` for create, `400` for malformed or invalid payloads, `401` for missing/invalid bearer tokens, `403` for an authenticated token without the required permission or environment scope, `404` for a missing environment/type/reference, and `409` for immutable identity or incompatible schema changes.


