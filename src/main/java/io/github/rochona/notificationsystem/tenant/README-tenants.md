# Phase 3 – Tenant Management, API-Key Authentication & Tenant Isolation

## Overview

This phase extends the notification system to support multiple tenants with API-key-based authentication and tenant-level data isolation.

Each tenant receives a unique API key during registration. Authenticated tenants can create, retrieve and delete only their own notifications.

## Features Implemented

### 1. Tenant Registration

- Implemented `Tenant` entity and `TenantRepository`.
- Implemented tenant registration through `TenantService`.
- Added `TenantController` to expose tenant registration through a REST API.
- Added request validation for tenant names.

**Endpoint:**

`POST /tenants/signUp`

The registration response contains:
- Tenant ID
- Tenant name
- Generated API key

The raw API key is returned only during registration.

### 2. Secure API-Key Generation and Storage

- Implemented API-key generation using Java's `SecureRandom`.
- Generated 256-bit random API keys.
- Added the `ntf_live_` prefix.
- Implemented SHA-256 hashing using `ApiKeyHasher`.
- Only hashed API keys are stored in PostgreSQL.
- Raw API keys are returned to the client during registration and are not persisted.

### 3. Tenant Authentication

Implemented `TenantAuthenticationService` to authenticate tenants using their API keys.

Authentication flow:

1. Extract the API key from the `X-API-Key` request header.
2. Validate that the API key is present.
3. Hash the supplied API key.
4. Find the tenant using the stored API-key hash.
5. Return the authenticated tenant or throw `InvalidApiKeyException`.

### 4. Tenant Authentication Interceptor

Implemented `TenantAuthenticationInterceptor` using Spring MVC's `HandlerInterceptor`.

Responsibilities:

- Extract the API key from incoming requests.
- Authenticate the tenant.
- Store the authenticated tenant in the request attribute `authenticatedTenant`.

Registered the interceptor through `WebMvcConfig` to protect `/notifications` endpoints.

### 5. Notification-Tenant Association

Updated the `Notification` entity to establish a many-to-one relationship with `Tenant`.

- Added the `tenant_id` foreign key.
- Associated notifications with their owning tenant.
- Updated notification creation to use the authenticated tenant.

### 6. Tenant Data Isolation

Implemented tenant-scoped notification operations.

- Fetch all notifications belonging to the authenticated tenant.
- Fetch a notification using both its ID and tenant.
- Delete notifications using notification ID, tenant and notification status.
- Prevent access to notifications belonging to other tenants.

A tenant attempting to access another tenant's notification receives a notification-not-found response.

### 7. Exception Handling

Added exception handling for:

- Missing or invalid API keys – `401 Unauthorized`.
- Notifications not found for the authenticated tenant – `404 Not Found`.
- Notifications that cannot be deleted – `409 Conflict`.

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | `/tenants/signUp` | Register a tenant |
| POST | `/notifications/create` | Create a notification |
| GET | `/notifications` | Fetch all notifications belonging to the authenticated tenant |
| GET | `/notifications/{id}` | Fetch a notification by ID |
| DELETE | `/notifications/{id}` | Delete a pending notification |

### Authentication

All notification endpoints require the following request header:

```text
X-API-Key: <your-tenant-api-key>
```

Tenant registration does not require API-key authentication.

## Testing

Automated tests were implemented using:

- JUnit 5
- Mockito
- AssertJ
- Spring Boot Test
- MockMvc

Test coverage includes:

- Tenant registration and request validation.
- API-key generation and hashing.
- Valid, invalid and missing API-key authentication.
- Notification creation and retrieval.
- Tenant-scoped notification retrieval.
- Cross-tenant access rejection.
- Successful deletion of pending notifications.
- Rejection of deletion for non-pending notifications.
- Exception handling.
- Mailpit email integration.

**Test execution result: 36 tests passed.**

## Deferred Integration Testing

Comprehensive integration testing will be performed during a dedicated testing phase after core feature development.

Planned coverage:

- PostgreSQL integration testing using Testcontainers.
- RabbitMQ integration testing using Testcontainers.
- Repository-level tenant-isolation verification.
- Integration testing across application components.

## Phase 3 Completion Status

- [x] Tenant registration
- [x] Secure API-key generation
- [x] API-key hashing and storage
- [x] Tenant authentication service
- [x] Authentication interceptor
- [x] Notification-tenant association
- [x] Tenant-scoped notification retrieval
- [x] Tenant-scoped notification deletion
- [x] Exception handling
- [x] Controller and service unit tests
- [x] Full test suite – 36 tests passed
- [ ] Comprehensive Testcontainers-based integration testing (deferred)

**Phase 3: Core implementation completed.**