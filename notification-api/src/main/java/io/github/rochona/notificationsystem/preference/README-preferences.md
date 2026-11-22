# Preferences Module

## 1. Overview

The Preferences module manages notification channel preferences for end users within each tenant.

It allows tenants to create, retrieve, update, list, and delete notification preferences for their users.

Each preference record controls whether the following notification channels are enabled:

- Email
- SMS
- Push notifications

The module is tenant-scoped. A tenant can manage preferences only for users belonging to its own application context.

The notification system does not maintain central user profiles. Instead, it uses an external `userId` supplied by the tenant. This allows tenants to use their own user identifiers, including UUIDs and other identifier formats.

### Key Features

- Tenant-scoped notification preferences.
- Create or update preferences using a single PUT endpoint.
- Retrieve preferences for an individual user.
- Retrieve all preferences belonging to a tenant.
- Pagination and filtering by user ID.
- Delete preferences for an individual user.
- Independent control over Email, SMS, and Push channels.
- Database-level uniqueness for tenant and user combinations.
- Transactional service operations.
- Handling of concurrent preference creation conflicts.

## 2. Package Structure

```text
preference/
├── controller/
│   └── PreferenceController
│
├── dto/
│   ├── PreferenceRequestDto
│   └── PreferenceResponseDto
│
├── exception/
│   ├── PreferenceAlreadyExistsException
│   └── PreferenceDoesNotExistException
│
├── model/
│   └── Preference
│
├── repository/
│   └── PreferenceRepository
│
├── service/
│   ├── PreferenceService
│   └── PreferenceServiceImpl
│
└── specification/
    └── PreferenceSpecification
```

### Package Responsibilities

| Package         | Responsibility                                             |
| --------------- | ---------------------------------------------------------- |
| `controller`    | Exposes REST endpoints for preference management.          |
| `dto`           | Defines preference request and response structures.        |
| `exception`     | Contains preference-specific business exceptions.          |
| `model`         | Contains the Preference persistence entity.                |
| `repository`    | Handles preference database operations.                    |
| `service`       | Implements preference management and business logic.       |
| `specification` | Builds dynamic database queries for filtering preferences. |

## 3. Preference Data Structure

A preference record represents the notification channel settings for one user within one tenant.

The following fields are exposed through the response DTO:

| Field          | Type    | Description                                            |
| -------------- | ------- | ------------------------------------------------------ |
| `userId`       | String  | Tenant-provided external user identifier.              |
| `emailEnabled` | Boolean | Whether email notifications are enabled.               |
| `smsEnabled`   | Boolean | Whether SMS notifications are enabled.                 |
| `pushEnabled`  | Boolean | Whether push notifications are enabled.                |
| `createdAt`    | Instant | Timestamp when the preference record was created.      |
| `updatedAt`    | Instant | Timestamp when the preference record was last updated. |

The tenant association and internal persistence details are not exposed through the response DTO.

### Request DTO

`PreferenceRequestDto` accepts three fields:

| Field          | Type    | Validation            |
| -------------- | ------- | --------------------- |
| `emailEnabled` | Boolean | Required (`@NotNull`) |
| `smsEnabled`   | Boolean | Required (`@NotNull`) |
| `pushEnabled`  | Boolean | Required (`@NotNull`) |

All three values must be supplied in every request.

A value of `false` is valid. Therefore, tenants can disable all three notification channels simultaneously.

### Response DTO

`PreferenceResponseDto` contains:

- User ID.
- Email preference.
- SMS preference.
- Push preference.
- Creation timestamp.
- Last updated timestamp.

It does not expose the tenant ID or internal database identifier.

## 4. API Endpoints

All preference endpoints use the following base path:

`/preferences`

Every endpoint operates in the context of the authenticated tenant.

The tenant is obtained from the request attribute:

`authenticatedTenant`

### 4.1 Create or Update Preference

**Endpoint:** `PUT /preferences/{userId}`

**Description:**

Creates a new preference record or updates an existing record for the specified user.

This endpoint implements upsert behavior.

The service performs the following operations:

1. Searches for an existing preference using the tenant ID and user ID.
2. If a record exists, retrieves it for updating.
3. If no record exists, creates a new Preference entity and associates it with the authenticated tenant and supplied user ID.
4. Updates all three channel preference values.
5. Saves and flushes the entity.
6. Returns the resulting preference as `PreferenceResponseDto`.

This is a full-replacement operation: all three channel preferences must be supplied in the request.

**Path Parameter:**

| Parameter | Type   | Validation                                 |
| --------- | ------ | ------------------------------------------ |
| `userId`  | String | Must not be blank; maximum 100 characters. |

**Request Body:**

```json
{
  "emailEnabled": true,
  "smsEnabled": false,
  "pushEnabled": true
}
```

**Response:** `200 OK`

```json
{
  "userId": "user-1001",
  "emailEnabled": true,
  "smsEnabled": false,
  "pushEnabled": true,
  "createdAt": "2026-11-15T10:30:00Z",
  "updatedAt": "2026-11-15T10:30:00Z"
}
```

The timestamps above are illustrative.

### 4.2 Get Preference by User ID

**Endpoint:** `GET /preferences/{userId}`

**Description:**

Retrieves the notification preferences for a particular user within the authenticated tenant.

The service searches using both:

- Tenant ID.
- User ID.

If no matching preference exists, the service throws `PreferenceDoesNotExistException`.

**Path Parameter:**

| Parameter | Type   | Validation                                 |
| --------- | ------ | ------------------------------------------ |
| `userId`  | String | Must not be blank; maximum 100 characters. |

**Response:** `200 OK`

```json
{
  "userId": "user-1001",
  "emailEnabled": true,
  "smsEnabled": false,
  "pushEnabled": true,
  "createdAt": "2026-11-15T10:30:00Z",
  "updatedAt": "2026-11-15T10:30:00Z"
}
```

### 4.3 Get All Preferences

**Endpoint:** `GET /preferences`

**Description:**

Retrieves preferences belonging to the authenticated tenant.

The endpoint supports pagination and optional filtering by user ID.

**Query Parameters:**

| Parameter | Type    | Required | Default | Description                          |
| --------- | ------- | -------- | ------- | ------------------------------------ |
| `page`    | Integer | No       | `0`     | Zero-based page number.              |
| `size`    | Integer | No       | `20`    | Number of records per page.          |
| `userId`  | String  | No       | None    | Filters results by an exact user ID. |

Results are sorted by `createdAt` in descending order.

**Example Request:**

```http
GET /preferences?page=0&size=10
```

**Example with User ID Filter:**

```http
GET /preferences?page=0&size=10&userId=user-1001
```

**Response:** `200 OK`

The endpoint returns a Spring Data `Page` of `PreferenceResponseDto` objects.

```json
{
  "content": [
    {
      "userId": "user-1001",
      "emailEnabled": true,
      "smsEnabled": false,
      "pushEnabled": true,
      "createdAt": "2026-11-15T10:30:00Z",
      "updatedAt": "2026-11-15T10:30:00Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "first": true,
  "numberOfElements": 1,
  "empty": false
}
```

The response illustrates the pagination structure; Spring Data may include additional metadata fields.

### Filtering Implementation

The service uses `PreferenceSpecification` to construct dynamic queries.

The following conditions are applied:

1. A mandatory tenant condition is added using `belongsToTenant(tenantId)`.
2. If `userId` is supplied, an additional exact-match condition is added using `hasUserId(userId)`.
3. The resulting specification is passed to the repository along with the `Pageable` object.

This ensures that filtering never removes the tenant-scoping condition.

### 4.4 Delete Preference

**Endpoint:** `DELETE /preferences/{userId}`

**Description:**

Deletes the preference record associated with the specified user within the authenticated tenant.

The repository operation uses both tenant ID and user ID:

`deleteByTenant_IdAndUserId(tenantId, userId)`

This prevents a tenant from deleting another tenant's preference record.

**Path Parameter:**

| Parameter | Type   | Validation                                 |
| --------- | ------ | ------------------------------------------ |
| `userId`  | String | Must not be blank; maximum 100 characters. |

**Response:** `204 No Content`

The endpoint does not return a response body.

The service does not explicitly check whether a preference exists before issuing the delete operation. Therefore, a deletion request for a user without a matching preference does not produce `PreferenceDoesNotExistException` in this implementation.

Deleting a preference does not delete the user's notification history.

## 5. Tenant Isolation

Tenant isolation is a fundamental part of the Preferences module.

The authenticated tenant is obtained from:

`@RequestAttribute("authenticatedTenant")`

The tenant ID is used in preference retrieval, listing, and deletion.

| Operation         | Tenant isolation mechanism                               |
| ----------------- | -------------------------------------------------------- |
| Create preference | Associates the new entity with the authenticated tenant. |
| Update preference | Finds the existing record using tenant ID and user ID.   |
| Get preference    | Searches using tenant ID and user ID.                    |
| List preferences  | Applies a mandatory tenant specification.                |
| Delete preference | Deletes using tenant ID and user ID.                     |

A tenant cannot retrieve, update, or delete another tenant's preference record by supplying that tenant's user ID.

## 6. Database Constraints and Concurrent Creation

The preference design allows only one preference record for a particular combination of tenant and user ID.

The database uses the following unique constraint:

`uk_preference_tenant_user`

This protects the uniqueness of the `(tenant_id, user_id)` combination.

### Concurrent Creation Scenario

Consider two requests attempting to create preferences for the same user under the same tenant at approximately the same time.

Both requests may initially find that no preference exists.

However, only one request can successfully insert the record because of the database uniqueness constraint.

The service handles this scenario in `savePreference()`:

1. Attempts to persist the preference using `saveAndFlush()`.
2. Catches `DataIntegrityViolationException`.
3. Inspects the underlying cause for a Hibernate `ConstraintViolationException`.
4. Checks whether the violated constraint is `uk_preference_tenant_user`.
5. Throws `PreferenceAlreadyExistsException` if that constraint was violated.
6. Rethrows the original exception for other integrity violations.

This provides database-level protection against duplicate preference records even when concurrent requests pass the initial existence check.

## 7. Transaction Management

`PreferenceServiceImpl` is annotated with `@Transactional`.

This provides transaction boundaries for service operations.

The read operations use:

`@Transactional(readOnly = true)`

These include:

- `getPreference()`
- `getAllPreferences()`

The upsert operation and delete operation execute within the service's transactional context.

The use of `saveAndFlush()` in the upsert operation also ensures that database constraint violations can be detected within the `savePreference()` method, allowing the service to translate the known uniqueness violation into a domain-specific exception.

## 8. Exception Handling

The Preferences module defines two custom exceptions:

| Exception                          | Purpose                                                                                                                   |
| ---------------------------------- | ------------------------------------------------------------------------------------------------------------------------- |
| `PreferenceAlreadyExistsException` | Represents a duplicate preference creation conflict, particularly when the tenant-user uniqueness constraint is violated. |
| `PreferenceDoesNotExistException`  | Thrown when an individual preference lookup does not find a matching record.                                              |

These exceptions are handled by the application's global exception-handling layer.

The exact HTTP status codes and error response structure depend on the global exception handler.

## 9. Preference Behavior During Notification Delivery

The Preferences module is designed to work with the Notifications module.

The notification system uses the tenant-provided `userId` to determine whether the corresponding notification channel is enabled.

The implemented system-level behavior is:

- If a preference record exists, its channel settings determine which channels are enabled for that user.
- If no preference record exists, all notification channels are enabled by default.
- If a channel is explicitly disabled, a notification requested for that channel is saved with status `SUPPRESSED` and is not sent.
- If `userId` is absent from a notification request, preference checking is skipped.
- All three channels may be disabled simultaneously.

`NotificationServiceImpl` enforces these settings during notification creation. When `userId` is supplied, it calls `PreferenceService.isChannelEnabled()` with the authenticated tenant ID, the external user ID, and the requested `NotificationChannel`. This keeps the lookup tenant-scoped.

If the requested channel is enabled, or no preference record exists, the existing synchronous delivery flow continues. If the channel is disabled, the notification is persisted as `SUPPRESSED` and the `NotificationSender` is not invoked.

The preference service remains responsible for managing preference records; the Notifications module owns enforcement in the notification workflow.

### Independent Notification History

Preference records and notification history are intentionally independent.

A notification stores its own tenant ID and optional external user ID.

There is no foreign-key dependency from notification history to the preference record.

Consequently:

- Deleting a preference does not delete past notifications.
- Notification history remains available after preference deletion.
- Removing a preference does not erase the user's notification records.

This separation preserves notification history independently of a user's current communication preferences.

## 10. Optimistic Locking
The Preference entity uses JPA optimistic locking through the @Version annotation. This prevents concurrent updates from silently overwriting each other.

When multiple requests attempt to update the same preference record, Hibernate checks the version during persistence. If a request tries to update a stale version, an optimistic locking exception is raised.

This ensures data consistency without requiring pessimistic database locks.


## 11. Design Decisions

The following decisions define the Preferences module's intended behavior.

| Design area                                     | Decision                                       |
| ----------------------------------------------- | ---------------------------------------------- |
| Preference ownership                            | Tenant-scoped.                                 |
| User identity                                   | External `userId` supplied by the tenant.      |
| User profiles                                   | No central user profile management.            |
| Uniqueness                                      | One preference per `(tenant_id, user_id)`.     |
| Update operation                                | PUT with full-replacement/upsert semantics.    |
| Missing preference lookup                       | Returns a domain exception for individual GET. |
| Missing preference during notification delivery | All channels enabled by default.               |
| Channel preferences                             | Email, SMS, and Push.                          |
| All channels disabled                           | Allowed.                                       |
| Preference deletion                             | Deletes only the preference record.            |
| Notification history                            | Independent of preference records.             |
| Pagination                                      | Default page 0, size 20.                       |
| Filtering                                       | Exact user ID filtering.                       |

## 12. Testing

The Preferences module should be tested at both controller and service levels.

The project's testing stack includes:

- JUnit 5
- Mockito
- MockMvc

Important test scenarios include:

### Upsert Tests

- Create a preference when none exists.
- Update an existing preference.
- Verify all three channel values are replaced.
- Verify all three Boolean fields are required.
- Verify tenant association.
- Verify duplicate creation conflicts are handled.

### Retrieval Tests

- Retrieve an existing preference.
- Return the appropriate exception when no preference exists.
- Verify tenant isolation.

### Listing Tests

- Return preferences with default pagination.
- Apply custom pagination.
- Filter by exact user ID.
- Return only records belonging to the authenticated tenant.
- Return an empty page when no records match.

### Delete Tests

- Delete an existing preference.
- Verify that deletion is tenant-scoped.
- Verify that notification history is not deleted.
- Verify behavior when no matching preference exists.

### Notification Preference Enforcement Tests

The Notifications service tests verify the integration with the Preferences service:

- Suppress a notification when its requested channel is disabled.
- Continue delivery when the requested channel is enabled.
- Continue delivery when no preference record exists (default allow).
- Skip preference checking when `userId` is absent.
- Verify that `NotificationSender` is not invoked for a suppressed notification.

## 13. Future Improvements

Potential production-oriented improvements include:

- Auditing preference changes.
- Adding more flexible filtering where required.
- Adding integration tests for concurrent preference creation.
- Improving observability for preference-related failures.
- Reviewing optimistic locking behavior under concurrent updates.
- Adding preference-change events if future delivery workflows require them.

---

## 14. Summary

The Preferences module provides tenant-isolated management of end-user notification preferences.

It supports full-replacement upserts, individual retrieval, paginated listing, exact user ID filtering, and deletion.

Its database uniqueness constraint protects against duplicate tenant-user preference records, while its separation from notification history ensures that preference changes do not affect previously created notifications.

The module provides the preference data and service operations used by the Notifications module to enforce channel choices during notification creation. Disabled channels result in `SUPPRESSED` notifications, while missing preference records default to allowing delivery. The current enforcement occurs in the synchronous notification workflow; the planned asynchronous delivery phase will need to preserve this preference behavior.
