# Notifications Module

## 1. Overview

The Notifications module is responsible for creating, sending, retrieving, filtering, and managing notifications within the Multi-tenant Notification System.

It supports multiple notification channels and allows tenants to create notifications using either:

- Direct notification content supplied in the API request.
- Reusable notification templates with dynamic variable substitution.

Every notification is associated with a tenant. Tenant identification is handled through API-key authentication, and the authenticated tenant is made available to the controller through the `authenticatedTenant` request attribute.

The module also supports:

- Notification status tracking.
- Tenant-scoped notification retrieval.
- Pagination and filtering.
- Retrieval of individual notifications.
- Conditional notification deletion.

## 2. Package Structure

The Notifications module follows a layered package structure.

```text
notification/
├── controller/
│   └── NotificationController
│
├── dto/
│   ├── NotificationRequestDTO
│   ├── NotificationRequestValidator
│   ├── NotificationResponseDTO
│   └── ValidNotificationRequest
│
├── enums/
│   ├── NotificationChannel
│   └── NotificationStatus
│
├── exception/
│   ├── NotificationNotDeletableException
│   └── NotificationNotFoundException
│
├── model/
│   └── Notification
│
├── repository/
│   └── NotificationRepository
│
├── sender/
│   ├── EmailNotificationSender
│   └── NotificationSender
│
├── service/
│   ├── NotificationService
│   └── NotificationServiceImpl
│
└── specification/
    └── NotificationSpecification
```

### Package Responsibilities

| Package         | Responsibility                                                                |
| --------------- | ----------------------------------------------------------------------------- |
| `controller`    | Exposes notification REST endpoints and handles HTTP requests.                |
| `dto`           | Contains request/response DTOs and request validation logic.                  |
| `enums`         | Defines supported notification channels and statuses.                         |
| `exception`     | Contains notification-specific exceptions.                                    |
| `model`         | Contains the Notification JPA entity.                                         |
| `repository`    | Handles notification persistence and retrieval.                               |
| `sender`        | Defines the notification sending abstraction and email sender implementation. |
| `service`       | Implements notification creation, retrieval, filtering, and deletion.         |
| `specification` | Builds dynamic database queries for notification filtering.                   |

## 3. Notification Channels

The system defines the following notification channels through `NotificationChannel`:

- `EMAIL`
- `SMS`
- `PUSH`

The channel is supplied when creating a notification and stored with the notification record.

The current sender implementation shown in the package structure includes `EmailNotificationSender`, which implements the `NotificationSender` abstraction.

The interface provides a boundary between notification management and the actual delivery mechanism.

## 4. Notification Status

The notification lifecycle is represented by `NotificationStatus`.

The statuses used by the current implementation are:

| Status      | Description                                                      |
| ----------- | ---------------------------------------------------------------- |
| `PENDING`    | Initial status assigned to a newly created notification.                         |
| `SUPPRESSED` | The requested channel is disabled in the user's preferences; sending is skipped. |
| `SENT`       | The notification sender completed without throwing an exception.                 |
| `FAILED`     | An exception occurred during the sending attempt.                                 |
| `DELIVERED`  | Represents successful delivery confirmation; not currently set by this workflow. |

### Current Status Transition

The current implementation sends notifications synchronously during notification creation.

```text
                Notification Created
                        |
                        v
                     PENDING
                        |
                        v
                 Attempt Sending
                   /        \
                  /          \
             Success        Exception
                |               |
                v               v
              SENT            FAILED
```

`DELIVERED` is part of the status model, but the supplied `NotificationServiceImpl` does not currently transition notifications to this status.

## 5. API Endpoints

All notification endpoints use the following base path:

`/notifications`

All endpoints require tenant authentication. The authenticated tenant is obtained from the `authenticatedTenant` request attribute.

### 5.1 Create Notification

**Endpoint:** `POST /notifications/create`

**Description:**

Creates and sends a notification for the authenticated tenant.

The API supports two notification creation modes.

#### Mode A: Direct Notification

The tenant supplies the notification content directly in the API request.

The service uses the supplied subject and message without template rendering.

#### Mode B: Template-based Notification

The tenant supplies a template ID and a map of variables.

The service:

1. Retrieves the requested template using the template ID and authenticated tenant.
2. Renders the template subject using the supplied variables.
3. Renders the plain-text body.
4. Renders the HTML body.
5. Creates the notification using the rendered content.
6. Checks the user's channel preference when `userId` is supplied.
7. Saves the notification. If the requested channel is disabled, it is saved as `SUPPRESSED` and delivery stops here.
8. If delivery is allowed, invokes `NotificationSender`.
9. Updates its status to `SENT` or `FAILED`.
10. Saves the updated notification.

The template lookup is tenant-scoped, so a tenant cannot use another tenant's template.

#### Preference Check

After the notification content and tenant association are populated, `NotificationServiceImpl` checks the user's preference before saving for delivery:

- If `userId` is absent, preference checking is skipped.
- If `userId` is supplied, the preference is looked up using both the authenticated tenant ID and the supplied `userId`.
- If no preference record exists, the channel is treated as enabled by default and normal delivery continues.
- If the requested channel is enabled, normal delivery continues.
- If the requested channel is disabled, the notification is saved with status `SUPPRESSED` and `NotificationSender` is not invoked.

This check is performed for the notification's requested channel (`EMAIL`, `SMS`, or `PUSH`). A suppressed notification remains in notification history independently of the preference record.

#### Request DTO

The endpoint accepts a `NotificationRequestDTO`.

| Field        | Data Type            | Validation                   | Description                                           |
| ------------ | -------------------- | ---------------------------- | ----------------------------------------------------- |
| `subject`    | String               | No field-level constraint    | Notification subject.                                 |
| `message`    | String               | No field-level constraint    | Plain-text notification content.                      |
| `userId`     | String               | Maximum 100 characters       | Optional external user identifier.                    |
| `channel`    | NotificationChannel  | Required                     | Notification delivery channel.                        |
| `recipient`  | String               | Required, valid email format | Notification recipient.                               |
| `templateId` | Long                 | No field-level constraint    | Template identifier for template-based notifications. |
| `variables`  | Map\<String, Object> | No field-level constraint    | Dynamic values used during template rendering.        |

**Important validation details:**

- `channel` cannot be null.
- `recipient` cannot be blank and must satisfy email validation.
- `userId` cannot exceed 100 characters.
- `subject` and `message` do not have individual `@NotBlank` constraints.
- The DTO uses the class-level `@ValidNotificationRequest` annotation, implemented through `NotificationRequestValidator`, for request-mode validation.

The exact mode-specific rules are enforced by that custom validator.

#### Direct Notification Request

Example illustrating the direct-content fields:

```json
{
  "subject": "Booking Confirmation",
  "message": "Your booking has been confirmed.",
  "recipient": "customer@example.com",
  "channel": "EMAIL",
  "userId": "customer-123"
}
```

#### Template-based Notification Request

Example illustrating the template fields:

```json
{
  "templateId": 1,
  "variables": {
    "customerName": "John",
    "bookingId": "BK1001"
  },
  "recipient": "customer@example.com",
  "channel": "EMAIL",
  "userId": "customer-123"
}
```

These examples illustrate the two request modes; acceptance of each request is subject to the rules in `NotificationRequestValidator`.

#### Response

**HTTP Status:** `201 Created`

The endpoint returns a `NotificationResponseDTO`.

| Field                | Data Type           | Description                               |
| -------------------- | ------------------- | ----------------------------------------- |
| `id`                 | Long                | Generated notification identifier.        |
| `userId`             | String              | Associated external user ID, if supplied. |
| `subject`            | String              | Notification subject.                     |
| `message`            | String              | Plain-text notification content.          |
| `htmlMessage`        | String              | HTML notification content, if available.  |
| `recipient`          | String              | Notification recipient.                   |
| `channel`            | NotificationChannel | Notification delivery channel.            |
| `notificationStatus` | NotificationStatus  | Current notification status.              |

The creation controller explicitly maps all these fields from the saved notification.

### 5.2 Get All Notifications

**Endpoint:** `GET /notifications`

**Description:**

Retrieves notifications belonging to the authenticated tenant.

The endpoint supports pagination and optional filtering.

#### Query Parameters

| Parameter  | Required | Default | Description                                   |
| ---------- | -------- | ------- | --------------------------------------------- |
| `page`     | No       | `0`     | Zero-based page number.                       |
| `size`     | No       | `20`    | Number of records per page.                   |
| `status`   | No       | —       | Filter by notification status.                |
| `channel`  | No       | —       | Filter by notification channel.               |
| `userId`   | No       | —       | Filter by external user ID.                   |
| `fromDate` | No       | —       | Filter notifications created from this date.  |
| `toDate`   | No       | —       | Filter notifications created until this date. |

Date parameters use the ISO date format:

`yyyy-MM-dd`

#### Pagination Rules

- Default page number: `0`
- Default page size: `20`
- Minimum page size: `1`
- Maximum page size: `100`
- Default sorting: `createdAt DESC`

The API rejects:

- Negative page numbers.
- Page sizes below 1.
- Page sizes above 100.

#### Filtering

The endpoint supports combining multiple filters in a single request.

For example, a tenant can retrieve notifications matching a particular status and channel, belonging to a specific user, and created within a specified date range.

The service constructs a dynamic JPA `Specification` using `NotificationSpecification`.

The following predicates are applied:

- `belongsToTenant(tenantId)` — mandatory tenant restriction.
- `hasStatus(status)` — optional status filter.
- `hasChannel(channel)` — optional channel filter.
- `hasUserId(userId)` — optional user filter.
- `createdFrom(fromDate)` — optional lower date boundary.
- `createdUntil(toDate)` — optional upper date boundary.

Only filters with supplied values are added to the query.

The date range is validated before executing the query. If both dates are provided, `fromDate` cannot be after `toDate`.

The resulting records are mapped to `NotificationResponseDTO` and returned as a Spring Data `Page`.

**Important:** Tenant scoping is mandatory, regardless of which optional filters are supplied.

#### Example Request

```http
GET /notifications?page=0&size=10&status=SENT&channel=EMAIL&userId=customer-123&fromDate=2026-01-01&toDate=2026-01-31
```

### 5.3 Get Notification by ID

**Endpoint:** `GET /notifications/{id}`

**Description:**

Retrieves a notification by its ID, provided that it belongs to the authenticated tenant.

The service uses:

`findByIdAndTenant(id, tenant)`

If the notification does not exist or does not belong to the authenticated tenant, a `NotificationNotFoundException` is thrown.

**Success Response:** `200 OK`

The response contains notification details mapped to `NotificationResponseDTO`.

### 5.4 Delete Notification

**Endpoint:** `DELETE /notifications/{id}`

**Description:**

Deletes a notification belonging to the authenticated tenant.

Deletion is restricted to notifications whose status is `PENDING`.

#### Deletion Rules

1. The service retrieves the notification using its ID and authenticated tenant.
2. If the notification is not found within that tenant's scope, `NotificationNotFoundException` is thrown.
3. The service checks whether the notification status is `PENDING`.
4. If the status is anything other than `PENDING`, `NotificationNotDeletableException` is thrown.
5. The repository performs a conditional deletion using the notification ID, tenant, and `PENDING` status.
6. If the conditional deletion fails, `NotificationNotDeletableException` is thrown.

The repository-level status condition provides an additional safeguard against deleting a notification whose status changed after the initial lookup.

**Success Response:** `204 No Content`

## 6. Tenant Isolation

Tenant isolation is enforced throughout the Notifications module.

The authenticated tenant is obtained from the request attribute:

`authenticatedTenant`

The tenant is passed from the controller to the service layer.

Tenant restrictions are applied to:

- Notification creation.
- Notification listing.
- Notification retrieval by ID.
- Notification deletion.
- Template lookup during template-based notification creation.

For notification listing, the tenant restriction is always included in the JPA specification.

For individual retrieval and deletion, repository methods explicitly include the tenant in their lookup conditions.

This prevents one tenant from accessing or modifying another tenant's notification records.

## 7. Notification Creation and Delivery Flow

The current notification creation flow is:

```text
Tenant Application
       |
       v
POST /notifications/create
       |
       v
Tenant Authentication
       |
       v
NotificationController
       |
       v
NotificationServiceImpl
       |
       v
Determine Request Mode
       |
       +----------------------+
       |                      |
       v                      v
 Direct Content          Template Mode
       |                      |
       |                Retrieve Template
       |                      |
       |                Render Variables
       |                      |
       +----------+-----------+
                  |
                  v
          Create Notification
                  |
                  v
          Save Notification
                  |
                  v
          NotificationSender
                  |
             +----+----+
             |         |
             v         v
          Success   Exception
             |         |
             v         v
            SENT     FAILED
             |         |
             +----+----+
                  |
                  v
        Save Updated Notification
                  |
                  v
           Return Response
```

## 8. Persistence and Querying

`NotificationRepository` is responsible for persistence and database queries.

The service uses repository operations for:

- Saving newly created notifications, including notifications suppressed by user preferences.
- Saving the notification after an allowed sending attempt.
- Retrieving tenant-scoped notification pages using JPA Specifications.
- Retrieving a notification by ID and tenant.
- Deleting a notification conditionally by ID, tenant, and status.

The `NotificationSpecification` class keeps dynamic query construction separate from the service implementation.

This makes it possible to combine optional filters while preserving the mandatory tenant restriction.

## 9. Testing

The Notifications module is tested using:

- JUnit 5
- Mockito
- MockMvc

The project also includes a `MailpitIntegrationTest` to verify email sending against the local Mailpit SMTP environment.
We'll implement real SMTP-based email delivery as part of the production-oriented evolution of this project.


The notification tests cover the implemented creation, validation, retrieval, deletion, pagination, and filtering behavior. Creation tests also verify preference-based suppression, delivery when a channel is enabled, and bypassing preference checks when `userId` is absent.

## 10. Current Implementation and Future Evolution

The current implementation performs notification sending synchronously within the notification creation service.

This means the API request remains open while the sending operation is performed.

As part of Phase 5, notification delivery is planned to evolve into an asynchronous workflow using:

- RabbitMQ for message publishing and consumption.
- A separate Spring Boot Delivery Worker.
- Asynchronous delivery processing.
- Notification status updates after delivery attempts.

The existing `NotificationSender` abstraction provides a boundary for the delivery implementation to evolve without placing channel-specific sending logic directly in the controller.

## 11. Implementation Notes

The current DTO applies `@Email` validation to the `recipient` field for every notification channel.

Although the channel model includes EMAIL, SMS, and PUSH, the current recipient validation is email-specific. Channel-specific recipient validation can be introduced as the SMS and PUSH delivery implementations are developed.

The creation endpoint explicitly maps `htmlMessage` into its response. The current service-level `toResponseDTO()` mapper used by the listing and retrieval endpoints does not populate `htmlMessage`. If HTML content should be returned consistently across all notification endpoints, that mapper should also be updated.
