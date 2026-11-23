# Tenant & Security Module

## 1. Overview

The Tenant module provides tenant registration, API-key authentication, and tenant isolation for the Notification API.

The application follows a multi-tenant model: each registered tenant represents an organization using the notification platform. API requests that require tenant authentication are associated with the tenant identified by the supplied API key.

The module is responsible for:

- Registering tenants.
- Generating tenant API keys.
- Hashing API keys before persistence.
- Authenticating incoming API requests.
- Resolving the authenticated tenant for downstream application services.
- Supporting tenant-scoped access to notification-related resources.

Tenant identity is established at the API boundary and is used by other modules to restrict access to tenant-owned data.

## 2. Package Structure

```text
tenant/
├── config/
│   └── WebMvcConfig
├── controller/
│   └── TenantController
├── dto/
│   ├── TenantRegistrationRequest
│   └── TenantRegistrationResponse
├── exception/
│   └── InvalidApiKeyException
├── model/
│   └── Tenant
├── repository/
│   └── TenantRepository
├── security/
│   ├── ApiKeyGenerator
│   ├── ApiKeyHasher
│   └── TenantAuthenticationInterceptor
├── service/
│   ├── TenantAuthenticationService
│   └── TenantService
└── README-tenants.md
```

### Component Responsibilities

| Package / Component | Responsibility |
| --- | --- |
| `config/WebMvcConfig` | Configures MVC integration for the tenant authentication interceptor. |
| `controller/TenantController` | Exposes tenant registration endpoints. |
| `dto/TenantRegistrationRequest` | Defines the data required to register a tenant. |
| `dto/TenantRegistrationResponse` | Defines the registration response, including the generated API key as appropriate. |
| `exception/InvalidApiKeyException` | Represents an API-key authentication failure. |
| `model/Tenant` | Stores tenant information and the persisted API-key hash. |
| `repository/TenantRepository` | Performs tenant persistence and lookup operations. |
| `security/ApiKeyGenerator` | Generates a new API key for a tenant. |
| `security/ApiKeyHasher` | Hashes API keys before they are stored or compared. |
| `security/TenantAuthenticationInterceptor` | Intercepts protected requests and initiates tenant authentication. |
| `service/TenantAuthenticationService` | Validates an API key and resolves the corresponding tenant. |
| `service/TenantService` | Implements tenant registration and related business logic. |

## 3. Tenant Registration

Tenant registration is handled by `TenantController` and `TenantService`.

The registration process:

1. Accepts tenant registration information through `TenantRegistrationRequest`.
2. Creates a new API key using `ApiKeyGenerator`.
3. Hashes the generated API key using `ApiKeyHasher`.
4. Persists the tenant and API-key hash through `TenantRepository`.
5. Returns the registration result using `TenantRegistrationResponse`.

The raw API key is returned to the tenant during registration. The database stores the hash rather than the raw API key.

**Important:** The raw API key should be treated as a secret and securely retained by the tenant. It should not be logged or stored in source control.

> Refer to `TenantController` for the exact registration endpoint, HTTP method, request fields, and response status.

## 4. API-Key Authentication

The Tenant module uses API-key authentication for tenant-facing API requests.

### Authentication Flow

1. A client sends an API request with its tenant API key in the configured request header.
2. `TenantAuthenticationInterceptor` intercepts the request.
3. The interceptor delegates authentication to `TenantAuthenticationService`.
4. The authentication service hashes the supplied key using `ApiKeyHasher`.
5. The service looks up the tenant associated with the hash.
6. If the key is valid, the authenticated tenant is made available to downstream request handling.
7. If the key is missing or invalid for a protected endpoint, authentication fails with `InvalidApiKeyException`.

The exact header name and endpoint exclusions are configured in the authentication implementation and MVC configuration.

## 5. Tenant Context and Isolation

After successful authentication, the request is associated with the authenticated tenant.

Other modules use this tenant context to scope operations, including:

- Notification creation and retrieval.
- Template management and template lookup.
- Preference management.
- Outbox-related notification processing where tenant ownership is relevant.

Tenant-scoped service and repository operations must use the authenticated tenant rather than trusting a tenant identifier supplied by the client in a request body or query parameter.

This prevents one tenant from accessing or modifying another tenant's resources through tenant-owned API operations.

## 6. Tenant-Based Rate Limiting

The Notification API applies rate limiting to the notification creation endpoint using the tenant API key as the tenant identifier.

This makes the rate limit tenant-specific rather than relying only on a shared global request limit. Requests are associated with the authenticated tenant's API key, allowing the application to control notification creation usage per tenant.

The rate limiter protects the notification creation endpoint from excessive request volume and helps prevent one tenant from consuming disproportionate API capacity.

The exact request limit, time window, and rate-limiting implementation are defined in the notification module's configuration and code.

## 7. API-Key Storage and Security

### Hashing

The application persists a hash of the API key in the `Tenant` entity rather than storing the raw key.

`ApiKeyHasher` is responsible for producing the value used for persistence and authentication lookup.

The exact hashing algorithm and encoding should be documented from the current `ApiKeyHasher` implementation.

### Secret Handling

- Do not log raw API keys.
- Do not commit API keys to source control.
- Store tenant API keys securely on the client side.
- Avoid returning the raw API key after the registration response unless the API explicitly supports a secure key-rotation workflow.
- Restrict database access to protect persisted API-key hashes.

### Authentication Failure

`InvalidApiKeyException` represents an invalid API key. The application's global exception handler defines the resulting HTTP status and error response format.

## 8. Configuration

Tenant authentication is integrated with Spring MVC through `WebMvcConfig`.

Review the configuration and interceptor implementation for:

- The API-key request header name.
- Which paths require authentication.
- Which paths are excluded, such as tenant registration.
- The request attribute used to expose the authenticated tenant.
- The handling of missing or invalid API keys.

These values should match the current application configuration.

## 9. Integration with Other Modules

The Tenant module is a foundational module used by the rest of the Notification API.

| Module | Tenant integration |
| --- | --- |
| Notifications | Associates notifications with the authenticated tenant and enforces tenant-scoped access. |
| Templates | Restricts template creation, retrieval, updates, and rendering lookup to the owning tenant. |
| Preferences | Stores and manages preferences within the authenticated tenant's scope. |
| Outbox | Publishes events for notification records created within the tenant context; delivery processing uses notification identity and persisted ownership. |

Tenant authentication and tenant authorization are complementary: authentication establishes which tenant is making the request, while tenant-scoped service operations ensure that the tenant can access only its own resources.

## 10. Testing

Tenant-related tests should verify:

- Successful tenant registration.
- API-key generation and hashing.
- Raw API key is not persisted.
- Successful authentication with a valid API key.
- Rejection of invalid or missing API keys on protected routes.
- Correct interceptor registration and endpoint exclusions.
- Tenant context is available to downstream handlers.
- Tenant-scoped operations cannot access another tenant's resources.

The application uses JUnit 5, Mockito, and MockMvc for automated testing.

## 11. Future Security Considerations

Potential enhancements include:

- API-key rotation and revocation.
- Key creation and last-used auditing.
- Additional tenant-level quotas.
- More granular tenant roles and permissions if the product requires user-level access within an organization.
