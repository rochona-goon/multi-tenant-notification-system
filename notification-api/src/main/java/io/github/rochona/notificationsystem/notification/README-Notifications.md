# Notifications Module

## 1. Overview

The Notifications module is responsible for creating, retrieving, filtering, and managing notifications within the Multi-tenant Notification System. Notification delivery is processed asynchronously by the separate Delivery Worker application.

Tenants can create notifications using either:

- Direct notification content supplied in the API request.
- Reusable notification templates with dynamic variable substitution.

Every notification is associated with a tenant. Tenant identification is handled through API-key authentication, and the authenticated tenant is made available to the controller through the `authenticatedTenant` request attribute.

For notifications that are eligible for delivery, the API persists the notification and its OutboxEvent in the same database transaction. A scheduled Outbox Publisher later publishes the notification ID to RabbitMQ. The Delivery Worker consumes the message and performs the delivery attempt.

The module also supports:

- Notification status tracking.
- Tenant-scoped notification retrieval.
- Pagination and filtering.
- Retrieval of individual notifications.
- Conditional notification deletion.
- Preference-based suppression before an OutboxEvent is created.

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
| `sender`        | Contains notification-sending abstractions/implementations present in the API codebase; notification creation now queues eligible work rather than invoking a sender synchronously. |
| `service`       | Implements notification creation, retrieval, filtering, and deletion.         |
| `specification` | Builds dynamic database queries for notification filtering.                   |

## 3. Notification Channels

The system defines the following notification channels through `NotificationChannel`:

- `EMAIL`
- `SMS`
- `PUSH`

The requested channel is stored with the notification.

**Current delivery capability:** the Delivery Worker currently implements email delivery. Although `SMS` and `PUSH` are represented in the channel model, their delivery implementations are not part of the current Phase 5 workflow. A notification using an unsupported channel will fail during Worker processing rather than being delivered by the current email implementation.

The Notification API accepts and persists eligible notification requests; the Worker owns the actual delivery operation.

## 4. Notification Status

The notification lifecycle is represented by `NotificationStatus`.

| Status | Description |
| --- | --- |
| `PENDING` | The notification has been persisted and is waiting for asynchronous processing, or remains pending for another delivery attempt. |
| `SUPPRESSED` | The requested channel is disabled in the user's tenant-scoped preferences; no OutboxEvent is created and delivery is skipped. |
| `SENT` | The Worker completed the email send operation successfully. This reflects successful sending by the configured mail transport, not an end-recipient read confirmation. |
| `FAILED` | The notification reached a terminal delivery failure, such as retry exhaustion or a non-retryable failure routed to the dead-letter queue. Individual failed attempts are recorded in `NotificationAttempt`. |
| `DELIVERED` | Present in the status model; the current Worker flow does not set this status as a separate post-send confirmation. |

### Asynchronous Status Flow

```text
              Notification Request
                      |
                      v
            Check User Preference
                /           \
          Disabled          Enabled / no preference
              |                    |
              v                    v
         SUPPRESSED          Save Notification
                                   |
                                   v
                            Create OutboxEvent
                                   |
                                   v
                                PENDING
                                   |
                       Outbox Publisher / RabbitMQ
                                   |
                                   v
                         Delivery Worker attempt
                            /              \
                       Success            Failure
                          |                  |
                          v                  v
                        SENT       Attempt marked FAILED
                                     | retry queue (5s, then 10s)
                                     | retry exhausted / non-retryable
                                     v
                                   FAILED (DLQ)
```

A notification is initially persisted as `PENDING`. The API does not synchronously send the email or wait for the Worker to finish before returning the creation response.

The Worker records each delivery attempt in the `NotificationAttempt` table. A failed attempt is marked `FAILED` with an error message. Retryable failures are routed through the configured retry queues (5-second and 10-second delays); terminal or non-retryable failures are routed to the dead-letter queue and the notification is marked `FAILED`. A later successful attempt updates the notification to `SENT`.

`DELIVERED` is part of the status model, but the current workflow does not set it as a separate delivery-receipt status.

## 5. API Endpoints

All notification endpoints use the following base path:

`/notifications`

All endpoints require tenant authentication. The authenticated tenant is obtained from the `authenticatedTenant` request attribute.

### 5.1 Create Notification

**Endpoint:** `POST /notifications/create`

**Description:**

Creates a notification for the authenticated tenant and, when it is eligible for delivery, queues it for asynchronous processing. The API does not send the email during this request.

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
7. If the requested channel is disabled, saves the notification as `SUPPRESSED` and does not create an OutboxEvent.
8. If delivery is allowed, saves the notification and creates a `NOTIFICATION_CREATED` OutboxEvent in the same database transaction.
9. Returns the saved notification, normally with status `PENDING`; the Outbox Publisher and Delivery Worker process it asynchronously.

The template lookup is tenant-scoped, so a tenant cannot use another tenant's template.

#### Preference Check

After the notification content and tenant association are populated, `NotificationServiceImpl` checks the user's preference before creating an OutboxEvent:

- If `userId` is absent, preference checking is skipped.
- If `userId` is supplied, the preference is looked up using both the authenticated tenant ID and the supplied `userId`.
- If no preference record exists, the channel is treated as enabled by default and normal delivery continues.
- If the requested channel is enabled, normal delivery continues.
- If the requested channel is disabled, the notification is saved with status `SUPPRESSED`, no OutboxEvent is created, and no delivery message is published.

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

For an eligible notification, the returned status is typically `PENDING` because delivery is asynchronous. The response confirms that the notification was accepted and persisted; it does not confirm that the email has already been sent. A preference-suppressed notification is returned with status `SUPPRESSED`.

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

## 7. Notification Creation and Asynchronous Delivery Flow

The Phase 5 workflow separates accepting a notification request from performing the delivery attempt.

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
Resolve Direct Content or Render Template
       |
       v
Check Tenant-scoped User Preference
       |
       +--------------------------+
       |                          |
       v                          v
 Channel Disabled            Delivery Allowed
       |                          |
       v                          v
 Save SUPPRESSED          Begin Database Transaction
 No OutboxEvent                  |
                                 v
                         Save Notification
                                 |
                                 v
                         Save OutboxEvent
                    (same transaction)
                                 |
                                 v
                         Commit Transaction
                                 |
                                 v
                       Return API Response
                                 |
                                 v
                    Scheduled Outbox Publisher
                                 |
                                 v
                         RabbitMQ Exchange
                                 |
                                 v
                         Delivery Queue
                                 |
                                 v
                        Delivery Worker
                                 |
                                 v
                       Load Notification
                                 |
                                 v
                       Create Attempt Record
                                 |
                                 v
                         Send Email
                         /         \
                    Success       Failure
                       |             |
                       v             v
                 Notification   Attempt FAILED
                    SENT        Message requeued
```

The API stores the notification and corresponding OutboxEvent atomically. If the transaction fails, neither record is committed. The Outbox Publisher reads eligible outbox records and publishes the notification ID to RabbitMQ. After a positive publisher confirmation and routability check, the outbox event is marked `PUBLISHED`.

RabbitMQ carries the notification ID as a string message body. The Worker loads the notification from the shared PostgreSQL database, performs the delivery operation, records the attempt result, and acknowledges the message after successful processing. The Worker uses configured retry queues for retryable failures and a dead-letter queue for terminal or non-retryable failures. It acknowledges the original message only after the retry/DLQ routing and any required notification status update succeed; if routing fails, the original message is requeued.

Publisher confirmation and message acknowledgement represent different stages:

- **Publisher confirmation:** RabbitMQ confirms the broker accepted the published message.
- **Consumer acknowledgement:** the Worker confirms that it successfully processed the message.
- Neither event is an end-recipient read receipt.

## 8. Persistence and Querying

`NotificationRepository` is responsible for notification persistence and database queries.

The Notification API uses repository operations for:

- Saving newly created notifications, including notifications suppressed by user preferences.
- Saving the eligible notification as `PENDING` before asynchronous processing.
- Retrieving tenant-scoped notification pages using JPA Specifications.
- Retrieving a notification by ID and tenant.
- Deleting a notification conditionally by ID, tenant, and status.

For an eligible notification, `NotificationServiceImpl` saves the Notification and the related OutboxEvent within one transaction. The OutboxEvent records that a notification was created and provides the publisher with the notification ID to send to RabbitMQ.

The Delivery Worker shares the PostgreSQL database and updates the notification status after delivery processing. It also persists individual delivery attempts in `NotificationAttempt`, allowing attempt history and failure information to be recorded separately from the notification's current status.

The `NotificationSpecification` class keeps dynamic query construction separate from the service implementation. It allows optional filters to be combined while preserving the mandatory tenant restriction.

## 9. Testing

The Notification API project uses:

- JUnit 5
- Mockito
- AssertJ
- MockMvc

The Notification module tests cover the implemented creation, validation, retrieval, deletion, pagination, and filtering behavior. Creation tests also verify preference-based suppression, delivery eligibility when a channel is enabled, and bypassing preference checks when `userId` is absent.

Phase 5 tests additionally cover the API's transactional outbox behavior and the publisher's handling of RabbitMQ confirmations, unroutable messages, and retry scheduling. The separate Delivery Worker project has its own tests for message consumption, delivery processing, attempt tracking, and notification status updates.

## 10. Current Implementation and Future Evolution

The Phase 5 implementation uses asynchronous notification processing:

- The Notification API accepts and persists notification requests.
- The Notification and OutboxEvent are saved in the same transaction for eligible notifications.
- A scheduled publisher publishes pending outbox events to RabbitMQ.
- The separate Delivery Worker consumes notification-ID messages using manual acknowledgement.
- The Worker currently delivers email notifications through the configured JavaMailSender.
- Delivery attempts are stored separately; successful sending updates the notification status to `SENT`, while terminal delivery failure updates it to `FAILED`.

The channel model includes `EMAIL`, `SMS`, and `PUSH`; however, SMS and push delivery are not implemented in the current Worker workflow.

The OutboxEvent publisher, event claiming, publisher confirmation, and retry strategy are documented in the dedicated OutboxEvent README. Worker-specific configuration and message-processing details will be covered in the Delivery Worker README.

## 11. Implementation Notes

The current DTO applies `@Email` validation to the `recipient` field for every notification channel.

Although the channel model includes EMAIL, SMS, and PUSH, the current recipient validation is email-specific. Channel-specific recipient validation can be introduced as the SMS and PUSH delivery implementations are developed.

The creation endpoint explicitly maps `htmlMessage` into its response. The current service-level `toResponseDTO()` mapper used by the listing and retrieval endpoints does not populate `htmlMessage`. If HTML content should be returned consistently across all notification endpoints, that mapper should also be updated.
