# Multi-tenant Notification System

## Overview

A backend notification platform built with Java and Spring Boot. The system is being developed to support multiple organizations, with tenant-specific notification ownership, API-key-based authentication, and email delivery.

## Technology Stack

- **Language:** Java 21
- **Framework:** Spring Boot
- **Database:** PostgreSQL
- **Persistence:** Spring Data JPA / Hibernate
- **Messaging and Delivery:** Mailpit for local email testing
- **Build Tool:** Maven
- **Testing:** JUnit 5, Mockito, Spring Boot Test, MockMvc

## Project Setup

### Prerequisites

Ensure the following are installed:

- Java 21
- PostgreSQL
- Maven (or use the included Maven Wrapper)
- Docker, if using Mailpit through Docker

### Database Configuration

1. Create a PostgreSQL database named `notification_system`.
2. Configure the database connection in `src/main/resources/application.properties`.

Example configuration:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/notification_system
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

spring.jpa.hibernate.ddl-auto=update
```

Replace the environment variables with your local database credentials. The Hibernate `update` setting is intended for local development; a versioned migration strategy is preferable for production.

### Run the Application

On Windows:

```bash
mvnw.cmd spring-boot:run
```

On macOS/Linux:

```bash
./mvnw spring-boot:run
```

The application starts on port `8080` by default.

## Mailpit Integration

Mailpit is used to test email delivery locally without sending real emails.

- Configure the application's SMTP settings to point to the local Mailpit SMTP server.
- Start Mailpit using your local setup or Docker.
- Trigger a notification through the API and inspect the captured email in the Mailpit web interface.

Mailpit's default ports are `1025` for SMTP and `8025` for its web interface, unless configured otherwise.

## API Endpoints

### 1. Register Tenant

`POST /signUp`

Registers a new tenant and generates a unique API key.

Example request:

```json
{
  "tenantName": "Example Organization"
}
```

The response contains the tenant ID, tenant name, and generated API key.

**Security considerations:**
- API keys are generated using `SecureRandom`.
- Generated API keys contain 256 bits of randomness.
- Only the SHA-256 hash of the API key is stored in the database.
- The raw API key is returned during registration and should be stored securely by the client.

### 2. Create Notification

`POST /notify`

This endpoint requires tenant authentication through the `X-API-Key` request header.

Example request header:

```text
X-API-Key: <your-api-key>
```

Example request body:

```json
{
  "subject": "Welcome",
  "message": "Welcome to our platform!",
  "recipient": "user@example.com"
}
```

The authenticated tenant is resolved from the API key by the tenant authentication interceptor. The client does not supply tenant information in the notification request.

Requests with missing, blank, or invalid API keys are rejected with `401 Unauthorized`.

## Multi-tenancy and Security

### Tenant Model

Introduced the `Tenant` entity to represent organizations using the notification platform.

### Notification Ownership

- Added a many-to-one relationship between `Notification` and `Tenant`.
- Introduced the `tenant_id` foreign key in the `notifications` table.
- Associated existing notification records with a tenant while preserving existing data.
- Updated notification creation to associate notifications with the authenticated tenant.

### API Key Generation and Storage

- Implemented secure API-key generation using Java's `SecureRandom`.
- Added a unique API-key prefix: `ntf_live_`.
- Implemented SHA-256 hashing for API keys.
- Only hashed API keys are stored in the database.
- Raw API keys are returned only during tenant registration.

### Tenant Authentication

- Implemented `TenantAuthenticationService` to authenticate tenants using API keys.
- Added `TenantAuthenticationInterceptor` to validate incoming requests.
- Configured the interceptor to protect the notification endpoint.
- Authenticated tenant information is stored as a request attribute and used during notification creation.
- Added exception handling for invalid or missing API keys.

### Testing

Added automated tests covering:

- API-key generation.
- API-key hashing.
- Tenant registration.
- Tenant authentication.
- Notification controller request validation and authentication.
- Existing notification functionality and Mailpit integration.

**Current limitation:** Tenant authentication and notification ownership are implemented, but tenant data isolation is still pending. Repository-level and service-level access restrictions will be added to ensure that tenants can access only their own notifications.

## Running Tests

On Windows:

```bash
mvnw.cmd test
```

On macOS/Linux:

```bash
./mvnw test
```

## Development Status

- [x] Basic notification API
- [x] Local email testing with Mailpit
- [x] Initial tenant model and notification ownership relationship
- [x] Tenant registration API
- [x] Secure API-key generation
- [x] API-key hashing and secure storage
- [x] Tenant API-key authentication
- [x] Interceptor-based request authentication
- [x] Associate notifications with authenticated tenants
- [x] Automated tests for tenant registration and authentication
- [ ] Tenant data isolation
- [ ] Further integration and security testing