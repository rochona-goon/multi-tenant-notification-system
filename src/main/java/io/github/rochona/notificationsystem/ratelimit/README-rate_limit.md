# Tenant Rate Limiting

## Overview

The Rate Limiting module protects the Notification API against excessive request traffic.

It implements per-tenant rate limiting using Bucket4j's Token Bucket algorithm.

Each authenticated tenant receives an independent rate-limit bucket.

## Technology

- Java 21
- Spring Boot
- Bucket4j 8.22.0
- ConcurrentHashMap
- Spring MVC interceptors

## Rate Limit Configuration

The initial configuration is:

| Property | Value |
|---|---:|
| Bucket capacity | 5 tokens |
| Refill tokens | 5 |
| Refill duration | 1 minute |
| Refill strategy | Greedy |

Configuration is externalized in `application.properties`:

```properties
rate-limit.capacity=5
rate-limit.refill-tokens=5
rate-limit.refill-duration=1m
```

The configuration is bound through `RateLimitProperties` using Spring Boot's configuration-properties support.

## Token Bucket Behavior

Each tenant starts with a bucket containing five tokens.

Each accepted request consumes one token.

Tokens are replenished continuously using Bucket4j's `refillGreedy()` strategy.

When a tenant exhausts its available tokens, subsequent requests are rejected until sufficient tokens have been replenished.

Different tenants have independent buckets, so one tenant exhausting its limit does not affect another tenant.

## Request Processing

Rate limiting is applied through a Spring MVC interceptor.

The request processing order is:

1. Tenant API-key authentication.
2. Tenant identification through the authenticated tenant request attribute.
3. Rate-limit verification.
4. Controller execution if a token is available.

The rate-limit interceptor applies to these API paths:

- `/notifications`
- `/notifications/**`
- `/templates`
- `/templates/**`
- `/preferences`
- `/preferences/**`

Requests without an authenticated tenant are rejected. Requests that exceed the configured limit receive HTTP `429 Too Many Requests`.

## Architecture and Extensibility

The implementation separates bucket management from rate-limit enforcement through the `RateLimitBucketProvider` interface.

Current implementation:

- `RateLimitBucketProvider` – Defines the bucket-provider abstraction.
- `InMemoryBucketProvider` – Creates and manages tenant buckets using `ConcurrentHashMap`.
- `RateLimiterService` – Uses the provider to check whether a tenant can consume a token.
- `RateLimitInterceptor` – Enforces the rate limit during request processing.

This separation allows the bucket storage implementation to be replaced without changing the rate-limit service or interceptor.

## Current Limitations and Future Improvements

The current implementation uses in-memory storage.

Consequently:
- Rate-limit state is local to one application instance.
- Restarting the application resets bucket state.
- Multiple application instances do not share rate-limit state.
- Bucket entries currently have no eviction or expiry mechanism.

For a multi-instance deployment, a Redis-backed provider can be introduced to share rate-limit state across instances.

Additional production-readiness improvements include response headers such as `Retry-After` and remaining-token information, and lifecycle management for inactive tenant buckets.

## Testing

Tests cover:
- Allowing requests while tokens are available.
- Rejecting requests when a bucket is exhausted.
- Maintaining independent buckets for different tenants.
- Returning the same bucket for repeated requests from the same tenant.
- Ensuring one tenant's exhausted bucket does not affect another tenant.

The implementation is tested using JUnit 5 and Mockito.