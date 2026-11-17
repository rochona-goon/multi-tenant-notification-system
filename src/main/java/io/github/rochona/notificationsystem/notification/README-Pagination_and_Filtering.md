# Phase 4 – Notification Pagination & Filtering

## Overview

This phase enhances the Notification API with pagination and flexible filtering capabilities.

Tenants can retrieve their notification history using optional filters, allowing them to find notifications based on delivery status, notification channel, user ID, and creation date.

All notification queries are tenant-scoped to ensure that one tenant cannot access another tenant's notification records.

## Features Implemented

### 1. Pagination

The notification listing endpoint supports pagination through the `page` and `size` query parameters.

| Parameter | Default | Validation |
|---|---:|---|
| `page` | 0 | Must be greater than or equal to 0 |
| `size` | 20 | Must be between 1 and 100 |

Notifications are sorted by `createdAt` in descending order by default, so the newest notifications appear first.

Example:

```http
GET /notifications?page=0&size=20
```

### 2. Notification Filtering

The API supports the following optional filters:

| Filter | Type | Description |
|---|---|---|
| `status` | `NotificationStatus` | Filters notifications by delivery status |
| `channel` | `NotificationChannel` | Filters notifications by channel |
| `userId` | String | Filters notifications by external user ID |
| `fromDate` | LocalDate | Returns notifications created on or after this date |
| `toDate` | LocalDate | Returns notifications created on or before this date |

All filters are optional and can be combined.

When multiple filters are provided, they are combined using AND conditions.

### 3. Date Range Filtering

Date filters use the ISO date format:

```text
yyyy-MM-dd
```

Date filtering follows these rules:

- `fromDate` is inclusive and starts at the beginning of the specified day.
- `toDate` is inclusive and includes the entire specified day.
- If only `fromDate` is supplied, notifications from that date onwards are returned.
- If only `toDate` is supplied, notifications up to and including that date are returned.
- If `fromDate` is later than `toDate`, the API returns HTTP 400 Bad Request.

The implementation converts date boundaries to `Instant` values using UTC. The upper boundary is exclusive and represents the start of the day following `toDate`.

### 4. Tenant Isolation

Tenant identity is obtained from the authenticated tenant request attribute, populated through API-key authentication.

The tenant condition is always included in the notification query, regardless of which optional filters are supplied.

A tenant can retrieve only its own notification records.

## API Endpoint

### Get Notifications

**Endpoint:**

```http
GET /notifications
```

**Authentication:**

```http
X-API-Key: <tenant-api-key>
```

**Supported query parameters:**

```text
page
size
status
channel
userId
fromDate
toDate
```

### Example Requests

Retrieve notifications using default pagination:

```http
GET /notifications
```

Filter by notification status:

```http
GET /notifications?status=FAILED
```

Filter by channel and user ID:

```http
GET /notifications?channel=EMAIL&userId=user101
```

Filter by date range:

```http
GET /notifications?fromDate=2026-11-01&toDate=2026-11-13
```

Combine multiple filters:

```http
GET /notifications?status=FAILED&channel=EMAIL&userId=user101&fromDate=2026-11-01&toDate=2026-11-13&page=0&size=20
```

## Implementation Details

### Repository Layer

The `NotificationRepository` extends:

- `JpaRepository<Notification, Long>`
- `JpaSpecificationExecutor<Notification>`

`JpaSpecificationExecutor` enables dynamic query construction using Spring Data JPA Specifications.

### Specification Layer

A dedicated `NotificationSpecification` class contains reusable specifications for:

- Tenant-based filtering
- Notification status
- Notification channel
- User ID
- Creation date range
- Creation date lower and upper boundaries

This approach avoids creating numerous repository methods for every possible combination of optional filters.

### Service Layer

The `NotificationServiceImpl` dynamically builds a `Specification<Notification>`.

The tenant condition is applied first and remains mandatory.

Optional filters are added only when their corresponding values are supplied.

The repository executes the resulting specification with the requested `Pageable` object, and the resulting page is mapped to `NotificationResponseDTO`.

### Controller Layer

The `NotificationController`:

- Accepts pagination and filtering parameters.
- Applies default pagination values.
- Validates page number and page size.
- Validates the date range.
- Converts date parameters using ISO date formatting.
- Retrieves the authenticated tenant.
- Delegates the query to the notification service.

## Error Handling

The following invalid requests return HTTP 400 Bad Request:

| Scenario | Expected HTTP status |
|---|---|
| Negative page number | 400 |
| Page size below 1 | 400 |
| Page size above 100 | 400 |
| Invalid notification status | 400 |
| Invalid notification channel | 400 |
| `fromDate` later than `toDate` | 400 |

Invalid enum query parameters are handled through Spring's request parameter conversion exception handling.

## Testing

Unit and controller tests were added or updated to cover:

- Retrieving notifications with default pagination.
- Retrieving notifications with custom pagination.
- Empty notification results.
- Pagination metadata.
- Maximum page size.
- Invalid page number and page size.
- Filtering by status.
- Filtering by channel.
- Filtering by user ID.
- Combining multiple filters.
- Invalid status and channel values.
- Invalid date range.

The Notification service and controller test suites passed successfully.

**Note:** Actual database-level filtering behavior is intended to be verified through integration tests in a later testing stage.

## Technologies Used

- Java 21
- Spring Boot 4.1.1
- Spring Data JPA
- Hibernate
- PostgreSQL
- JUnit 5
- Mockito
- MockMvc
- Maven

## Outcome

The Notification API now supports paginated, tenant-scoped notification retrieval with flexible filtering.

The use of Spring Data JPA Specifications keeps the implementation extensible, making it easier to introduce additional notification filters in the future without creating a large number of repository methods.