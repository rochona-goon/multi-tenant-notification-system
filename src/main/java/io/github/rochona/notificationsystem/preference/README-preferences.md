# Phase 4 - Notification Preferences

## Overview

The Notification Preferences module allows tenants to manage their end users' notification channel preferences.

Each tenant can configure whether a particular user should receive notifications through Email, SMS, and Push channels.

The module follows a tenant-scoped design, ensuring that one tenant cannot access or modify another tenant's users' preferences.

## Key Features

- **Tenant-scoped preferences:** Each preference record belongs to a tenant.
- **External User ID:** Uses a tenant-provided `userId` instead of maintaining a central user-management system.
- **Channel-level preferences:** Users can independently enable or disable Email, SMS, and Push notifications.
- **Create or update:** Uses PUT to create a preference record or replace an existing one.
- **Preference retrieval:** Retrieve an individual user's preferences or list all preferences belonging to a tenant.
- **Preference deletion:** Delete a user's preference record without affecting notification history.
- **Input validation:** Validates required channel settings and user IDs.
- **Optimistic locking:** Uses JPA's `@Version` to prevent lost updates during concurrent modifications.
- **Database uniqueness:** Enforces one preference record per tenant and user combination.

## Data Model

### Preference Entity

The `Preference` entity is mapped to the `notification_preferences` table.

| Field | Description |
|---|---|
| `id` | Primary key |
| `tenant` | Associated tenant (mandatory relationship) |
| `userId` | Tenant-provided external user identifier (maximum 100 characters) |
| `emailEnabled` | Whether Email notifications are enabled |
| `smsEnabled` | Whether SMS notifications are enabled |
| `pushEnabled` | Whether Push notifications are enabled |
| `createdAt` | Record creation timestamp |
| `updatedAt` | Last modification timestamp |
| `version` | Optimistic locking version |

### Database Constraints

- Unique constraint on `(tenant_id, user_id)`.
- All three channel preferences are independently configurable.
- All channel preferences can be disabled simultaneously.
- The `userId` is stored as a string to support different identifier formats, including UUIDs.

## API Endpoints

All endpoints require tenant API-key authentication through the `X-API-KEY` header.

| Method | Endpoint | Description |
|---|---|---|
| `PUT` | `/preferences/{userId}` | Create or update user preferences |
| `GET` | `/preferences/{userId}` | Retrieve preferences for a user |
| `GET` | `/preferences` | Retrieve all preferences for the authenticated tenant |
| `DELETE` | `/preferences/{userId}` | Delete a user's preference record |

### 1. Create or Update Preferences

**Request**

`PUT /preferences/{userId}`

```json
{
  "emailEnabled": true,
  "smsEnabled": false,
  "pushEnabled": true
}
```

**Behavior**

- Creates a new preference record if one does not exist.
- Updates the existing record if one already exists.
- Performs a full replacement of the three channel preference values.
- Requires all three fields; missing or null values fail validation.

### 2. Get User Preferences

**Request**

`GET /preferences/{userId}`

Returns the preference record belonging to the authenticated tenant and supplied user ID.

If no record exists, the API returns `404 Not Found`.

### 3. Get All Preferences

**Request**

`GET /preferences`

Returns all stored preference records belonging to the authenticated tenant.

If no records exist, the API returns an empty list.

### 4. Delete User Preferences

**Request**

`DELETE /preferences/{userId}`

Deletes the preference record belonging to the authenticated tenant and supplied user ID.

**Behavior**

- Returns `204 No Content`.
- Deletion is idempotent: the response remains `204` even if the record does not exist.
- Does not delete notification history.

## Tenant Isolation and Security

Tenant identity is derived exclusively from the authenticated API key.

The tenant is retrieved from the `authenticatedTenant` request attribute populated by the tenant authentication interceptor.

The tenant ID is never accepted from the request body or query parameters.

All preference retrieval, update, and deletion operations are scoped by both tenant ID and external user ID.

This prevents cross-tenant access to preference records.

## Default Preference Behavior

The system does not automatically create preference records for every user.

If a notification is associated with a `userId` but no preference record exists, the delivery behavior defaults to all channels enabled.

If a notification does not contain a `userId`, preference checks are not applicable.

Retrieving a nonexistent preference through the GET endpoint still returns `404 Not Found`.

## Concurrency Handling

The `Preference` entity uses JPA optimistic locking through the `@Version` field.

Two important concurrent scenarios are handled:

1. **Concurrent updates:** If multiple requests attempt to update the same existing preference record, optimistic locking prevents a stale update from silently overwriting a newer update.
2. **Concurrent creation:** If multiple requests attempt to create the same tenant-user preference simultaneously, the database unique constraint ensures that only one record is created.

Conflicting operations are translated into appropriate conflict responses (`409 Conflict`).

## Validation and Error Handling

The module validates:

- `userId` must not be blank.
- `userId` must not exceed 100 characters.
- All three channel preference fields are required.
- Channel preference values must be boolean.

Relevant HTTP status codes:

| Status | Scenario |
|---|---|
| `200 OK` | Successful retrieval or upsert |
| `204 No Content` | Successful preference deletion |
| `400 Bad Request` | Invalid request or validation failure |
| `404 Not Found` | Requested preference does not exist |
| `409 Conflict` | Duplicate creation or optimistic locking conflict |
| `500 Internal Server Error` | Unexpected server-side error |

## Testing

The module includes unit and controller tests using:

- JUnit 5
- Mockito
- MockMvc
- AssertJ

### Service Tests

- Create a new preference.
- Update an existing preference.
- Create preferences with all channels disabled.
- Retrieve an existing preference.
- Handle missing preferences.
- Retrieve all preferences.
- Handle an empty preference list.
- Verify tenant-scoped, idempotent deletion.

### Controller Tests

- Successful preference creation and update.
- Validation of missing channel preference fields.
- User ID length validation.
- Successful preference retrieval.
- Handling nonexistent preferences.
- Retrieving all preferences.
- Successful deletion.
- Idempotent deletion.
- Invalid user ID handling.

## Design Decisions

- No central User entity is maintained.
- Tenants manage their own users and provide external user IDs.
- Preferences are tenant-scoped.
- A preference record is optional for a user.
- PUT provides full replacement/upsert semantics.
- Deleting preferences does not affect notification history.
- Notification history maintains its own user ID snapshot and has no foreign-key dependency on the preference record.
- Optimistic locking protects concurrent updates.
- Database uniqueness protects against duplicate preference records.

## Future Enhancements

- Pagination and filtering for preference retrieval.
- Database indexes based on actual query patterns.
- Integration testing with PostgreSQL.
