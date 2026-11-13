# Phase 4 - Notification Templates

## Overview

The Notification Templates feature allows tenants to create and manage reusable notification templates within the Multi-tenant Notification System.

Each tenant manages its own templates, ensuring that templates belonging to one tenant cannot be accessed or modified by another tenant.

The initial implementation supports **email templates**, with support for plain-text and HTML content.

## Features

- **Tenant-specific templates:** Each template belongs to a tenant.
- **Template creation:** Create reusable email templates with a name, subject, plain-text body, and HTML body.
- **Template retrieval:** Retrieve active templates by default or inactive templates when explicitly requested.
- **Template updates:** Update existing templates.
- **Template activation/deactivation:** Enable or disable templates without permanently deleting them.
- **Tenant isolation:** All operations are scoped to the authenticated tenant.
- **Duplicate prevention:** A tenant cannot create two templates with the same name.
- **Optimistic locking:** Prevent conflicting concurrent updates.

## Template Data Model

Each template contains the following fields:

| Field | Description |
|---|---|
| `id` | Unique template identifier |
| `tenant_id` | Tenant that owns the template |
| `name` | Template name |
| `subject` | Email subject |
| `body_text` | Plain-text email content |
| `body_html` | HTML email content |
| `is_active` | Indicates whether the template is active |
| `created_at` | Creation timestamp |
| `updated_at` | Last update timestamp |
| `version` | Optimistic locking version |

### Database Constraints

- Unique constraint on `(tenant_id, name)`.
- A template name must be unique within a tenant.
- Templates belonging to different tenants may have the same name.

## API Endpoints

All template endpoints require tenant authentication through the `X-API-KEY` request header.

| Method | Endpoint | Description |
|---|---|---|
| POST | `/templates` | Create a template |
| GET | `/templates` | Retrieve active templates |
| GET | `/templates?active=false` | Retrieve inactive templates |
| PUT | `/templates/{id}` | Update a template |
| PATCH | `/templates/{id}/status` | Activate or deactivate a template |

### Example: Create a Template

**Request**

`POST /templates`

```json
{
  "name": "Welcome Email",
  "subject": "Welcome to our platform!",
  "bodyText": "Hello, welcome to our platform.",
  "bodyHtml": "<h1>Welcome!</h1><p>We're glad to have you.</p>"
}
```

Include the tenant API key:

```http
X-API-KEY: <tenant-api-key>
```

### Example: Update Template Status

`PATCH /templates/{id}/status`

The request specifies the desired activation state.

```json
{
  "active": false
}
```

## Business Rules

1. Every template belongs to exactly one tenant.
2. The tenant is identified through API-key authentication. Clients cannot specify or override the tenant ID in the request.
3. A tenant cannot access or modify another tenant's templates.
4. Template names must be unique within the same tenant.
5. Inactive templates can still be updated.
6. Inactive templates cannot be used when creating notifications.
7. Templates are deactivated instead of permanently deleted.
8. Attempting to access a nonexistent template or a template belonging to another tenant returns `404 Not Found`.

## Error Handling

The implementation handles common scenarios, including:

- Missing or invalid request data.
- Duplicate template names within a tenant.
- Template not found.
- Attempts to access another tenant's template.
- Concurrent modification conflicts.

Errors are handled through the application's global exception handling mechanism.

## Testing

The template functionality includes unit tests covering the service and controller layers.

Tests cover:

- Template creation.
- Template retrieval and filtering by activation status.
- Template updates.
- Template activation and deactivation.
- Tenant isolation.
- Duplicate template-name handling.
- Invalid and missing template scenarios.

Run the test suite using:

```bash
./mvnw.cmd test
```

## Future Enhancements

The template architecture is designed to support additional notification channels in the future, including:

- SMS templates.
- Push notification templates.
- Dynamic template variable substitution.
- Additional template formats and rendering capabilities.

