# Multi-tenant Notification System — Notification API

## 1. Overview

The Notification API is the main application of the Multi-tenant Notification System. It is built as a Spring Boot modular monolith and provides tenant registration and authentication, notification management, reusable templates, tenant-scoped user preferences, rate limiting, and asynchronous delivery coordination.

The API accepts notification requests, validates and associates them with the authenticated tenant, applies the tenant's notification preferences, and persists notification data. For notifications eligible for delivery, it writes an outbox event in the same database transaction. The outbox publisher then publishes a notification-ID message to RabbitMQ for the separate Delivery Worker.

The Notification API and Delivery Worker are separate Spring Boot applications. They share PostgreSQL and communicate through RabbitMQ.

## 2. Architecture

```text
Tenant Client
    |
    | HTTP request + tenant API key
    v
Notification API (Spring Boot modular monolith)
    |
    ├── Tenant & Security
    ├── Notification
    ├── Template
    ├── Preference
    ├── Rate Limiting
    └── Transactional Outbox
             |
             | notification ID
             v
          RabbitMQ
             |
             v
      Delivery Worker
             |
             ├── Load notification from PostgreSQL
             ├── Track delivery attempts
             └── Send email through SMTP
```

### Main Design Decisions

- The Notification API is a modular monolith, not a collection of API microservices.
- Delivery execution is separated into its own Spring Boot Worker application.
- PostgreSQL is shared by the API and Worker.
- RabbitMQ decouples API request processing from delivery execution.
- The Transactional Outbox Pattern keeps notification persistence and event persistence atomic.
- Tenant isolation is enforced using API-key authentication and tenant-scoped operations.

## 3. Technology Stack

| Technology | Purpose |
| --- | --- |
| Java 21 | Application runtime |
| Spring Boot | API and Worker application framework |
| Spring Web / Spring MVC | REST API and request handling |
| Spring Data JPA / Hibernate | Persistence and ORM |
| PostgreSQL | Shared application database |
| RabbitMQ | Asynchronous notification delivery messaging |
| Mailpit | Local SMTP email capture and testing |
| Bucket4j | In-memory tenant-based rate limiting |
| JUnit 5 | Automated testing |
| Mockito | Unit-test mocking |
| MockMvc | Controller/API testing |
| Testcontainers | Integration testing with containerized dependencies |
| Maven | Build and dependency management |
| Docker Compose | Local supporting services |

## 4. Modules

The API is organized into modules with distinct responsibilities.

| Module | Responsibility |
| --- | --- |
| Tenant & Security | Tenant registration, API-key generation and hashing, authentication, and tenant context. |
| Notification | Notification creation, retrieval, filtering, deletion, status, and asynchronous delivery handoff. |
| Template | Tenant-scoped reusable templates and dynamic variable rendering. |
| Preference | Tenant-scoped notification channel preferences for external user IDs. |
| OutboxEvent | Transactional outbox persistence, event claiming, RabbitMQ publishing, and publisher retry handling. |
| Rate Limiting | Limits notification creation requests by tenant API key. |
| Shared / Configuration | Common response/error handling and application configuration. |

Module-level documentation is maintained alongside the relevant packages. See the READMEs in the corresponding module directories for implementation details.

## 5. Core Features

### Multi-Tenant API-Key Authentication

- Tenants register with the API and receive an API key.
- The raw API key is returned to the tenant; a hash is persisted by the application.
- Protected requests are authenticated using the tenant API key.
- The authenticated tenant is made available to downstream request handling.
- Tenant-owned resources are accessed using tenant-scoped service and repository operations.

### Notification Management

The API supports notification creation using:

- Direct notification content.
- A reusable template ID and a variable map.

Notification management includes tenant-scoped retrieval, pagination, filtering, individual record lookup, and conditional deletion.

### Templates and HTML Rendering

Templates support subject, plain-text body, and HTML body content with `{{variableName}}` placeholders.

The Template module uses separate text and HTML rendering methods. Dynamic values inserted into HTML content are escaped using OWASP Java Encoder's `Encode.forHtmlContent()`.

### User Preferences

Preferences are scoped by tenant and external `userId`. A tenant can manage whether a user should receive a notification through a particular channel.

When a notification includes a `userId`:

- The API checks the preference for the authenticated tenant and user.
- If no preference record exists, delivery is allowed by default.
- If the requested channel is disabled, the notification is saved as `SUPPRESSED` and no delivery event is created.

If `userId` is absent, preference checking is skipped.

### Tenant-Based Rate Limiting

Notification creation is rate-limited using the tenant API key as the tenant identifier. The current implementation uses in-memory Bucket4j buckets.

The rate limit is tenant-specific and protects the notification creation endpoint from excessive request volume.

### Asynchronous Delivery

For eligible notifications, the API:

1. Saves the notification.
2. Saves its outbox event in the same transaction.
3. Allows the scheduled outbox publisher to claim the event.
4. Publishes the notification ID to RabbitMQ.
5. Returns the created notification without waiting for email delivery to complete.

The Delivery Worker performs the actual delivery and records delivery attempt outcomes.

## 6. Notification Processing Flow

```text
Client Request
    |
    v
API-Key Authentication
    |
    v
Tenant Rate Limit
    |
    v
Validate Notification Request
    |
    v
Resolve Direct Content or Render Template
    |
    v
Check Tenant User Preference (if userId supplied)
    |
    +---- Channel disabled ----> Save as SUPPRESSED
    |
    v
Transactional Save
  - Notification
  - OutboxEvent
    |
    v
Commit
    |
    v
Outbox Publisher
    |
    v
RabbitMQ
    |
    v
Delivery Worker
```

The notification and outbox event are persisted together. If the transaction fails, both records are rolled back.

## 7. API Endpoints

The API exposes REST endpoints for tenant registration, notification management, templates, and preferences.

Some currently documented notification endpoints include:

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/notifications/create` | Create a notification. |
| `GET` | `/notifications` | Retrieve a tenant-scoped, paginated notification list with optional filters. |
| `GET` | `/notifications/{id}` | Retrieve a notification by ID within the tenant scope. |
| `DELETE` | `/notifications/{id}` | Delete a notification when its status permits deletion. |

All protected endpoints require tenant authentication. Refer to each module's README and controller mappings for the complete endpoint list, request/response fields, validation rules, and status codes.

## 8. Persistence

The Notification API uses PostgreSQL for tenant, notification, template, preference, and outbox data.

The Delivery Worker uses the same database to load notifications and persist delivery attempt records.

The application uses JPA and repository-level tenant-scoped queries to enforce data ownership. The OutboxEvent module includes database-backed event claiming and indexes for eligible event lookup.

## 9. RabbitMQ Integration

The API publishes notification-ID messages to RabbitMQ using the configured notification exchange, delivery queue, and routing key.

The configured topology includes:

- Exchange: `notification.exchange`
- Delivery queue: `notification.delivery.queue`
- Routing key: `notification.delivery`

The outbox publisher uses publisher confirms and retry scheduling. RabbitMQ publishing acceptance is separate from recipient delivery; delivery is completed asynchronously by the Worker.

## 10. Local Development Setup

### Prerequisites

- Java 21
- Docker Desktop
- PostgreSQL
- RabbitMQ
- Mailpit
- Maven (or use the included Maven Wrapper)

The repository includes Docker Compose configuration for local supporting services. Start the required infrastructure and ensure the ports and credentials match the application configuration.

### Configuration

Application configuration is maintained in `application.properties`. Use environment variables for sensitive values rather than committing passwords or tokens.

The API and Worker must be configured to connect to:

- The same PostgreSQL database.
- The same RabbitMQ broker.
- The appropriate local SMTP/Mailpit endpoint for email testing.

Check the current `application.properties` files for the exact property names and environment-variable placeholders.

### Run the Notification API

From the `notification-api` directory on Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

The API runs on the configured application port (currently `8080` in local development).

### Run the Delivery Worker

From the `delivery-worker` directory:

```powershell
.\mvnw.cmd spring-boot:run
```

The Worker runs separately (currently `8081` in local development) and consumes delivery messages from RabbitMQ.

## 11. Testing

The project uses:

- JUnit 5
- Mockito
- MockMvc
- Testcontainers

Tests cover module-level business logic, API behavior, tenant isolation, notification preferences, outbox atomicity and claiming, RabbitMQ publishing, and Worker delivery/attempt handling.

Run the API tests from the `notification-api` directory:

```powershell
.\mvnw.cmd test
```

Run the Worker tests from the `delivery-worker` directory:

```powershell
.\mvnw.cmd test
```

## 12. Repository Layout

```text
notification-system-v2/
├── notification-api/
│   ├── pom.xml
│   ├── mvnw
│   ├── src/
│   │   ├── main/
│   │   └── test/
│   └── README.md
├── delivery-worker/
│   ├── pom.xml
│   ├── mvnw
│   ├── src/
│   │   ├── main/
│   │   └── test/
│   └── README.md
├── docker-compose.yml
└── README.md
```

The repository root contains shared local infrastructure configuration. The Notification API and Delivery Worker have separate Maven projects and application entry points.

## 13. Current Scope and Future Extensions

The current implementation focuses on tenant-aware notification creation and asynchronous email delivery with reliable messaging and delivery-attempt tracking.

The channel model includes `EMAIL`, `SMS`, and `PUSH`, but email is the implemented delivery channel. SMS and push delivery can be added behind channel-specific sender implementations as the project evolves.

Potential future enhancements include:

- API-key rotation and revocation.
- More advanced tenant quotas and rate-limit configuration.
- Additional notification channel providers.
- Improved delivery observability and operational dashboards.
- Production deployment and external infrastructure configuration.
