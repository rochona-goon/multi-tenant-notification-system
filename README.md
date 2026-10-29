# Multi-tenant Notification System

## Overview

A backend notification platform built with Java and Spring Boot. The system is being developed to support multiple organizations, with tenant-specific notification ownership and asynchronous email delivery.

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

## API Endpoint

### Create Notification

`POST /notify`

Example request:

```json
{
  "subject": "Welcome",
  "message": "Welcome to our platform!",
  "recipient": "user@example.com"
}
```

The endpoint accepts notification details and returns a response containing the notification information and processing status.

## Multi-tenancy Foundation

### Tenant Model

Introduced the `Tenant` entity to represent organizations using the notification platform.

### Notification Ownership

- Added a many-to-one relationship between `Notification` and `Tenant`.
- Introduced the `tenant_id` foreign key in the `notifications` table.
- Associated existing notification records with a tenant while preserving existing data.

### Repository and Service Changes

- Added `TenantRepository` with API-key lookup support.
- Updated notification creation to associate notifications with a tenant.
- Updated unit and controller tests to reflect the new relationship.

**Current limitation:** API-key authentication, secure API-key storage, and tenant isolation are not yet implemented. Passing the tenant through the request DTO is temporary and will be replaced by authentication-based tenant resolution.

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
- [ ] Secure API-key authentication
- [ ] Tenant data isolation
- [ ] Further integration and security testing
