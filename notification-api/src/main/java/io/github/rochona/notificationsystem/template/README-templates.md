# Templates Module

## 1. Overview

The Templates module manages reusable, tenant-specific notification templates.

It allows tenants to create, retrieve, update, activate, deactivate, and list templates. These templates can later be used when creating notifications through the Notifications module.

A template contains:

- A name.
- An email subject.
- A plain-text body.
- An HTML body.

The module supports dynamic variable substitution using placeholders in the `{{variableName}}` format.

Templates are tenant-scoped. A tenant can access and manage only its own templates.

### Key Features

- Tenant-scoped template management.
- Template creation and retrieval.
- Template updates.
- Soft deactivation through an active/inactive status.
- Template activation and deactivation.
- Duplicate-name validation within a tenant.
- Pagination and filtering.
- Dynamic variable substitution.
- Validation of missing, null, and malformed template variables.
- Optimistic locking support, as established in the project's design.

## 2. Package Structure

```text
template/
├── controller/
│   └── TemplateController
│
├── dto/
│   ├── TemplateRequestDto
│   ├── TemplateResponseDto
│   └── TemplateStatusRequestDto
│
├── exception/
│   ├── TemplateAlreadyActiveException
│   ├── TemplateAlreadyInactiveException
│   ├── TemplateNameAlreadyExistsException
│   └── TemplateNotFoundException
│
├── model/
│   └── Template
│
├── repository/
│   └── TemplateRepository
│
├── service/
│   ├── TemplateRenderer
│   ├── TemplateService
│   └── TemplateServiceImpl
│
└── specification/
    └── TemplateSpecification
```

### Package Responsibilities

| Package         | Responsibility                                             |
| --------------- | ---------------------------------------------------------- |
| `controller`    | Exposes REST endpoints for template management.            |
| `dto`           | Defines template request and response structures.          |
| `exception`     | Contains template-specific business exceptions.            |
| `model`         | Contains the Template persistence entity.                  |
| `repository`    | Handles template database operations.                      |
| `service`       | Implements template business logic and variable rendering. |
| `specification` | Builds dynamic queries for template filtering.             |

## 3. Template Data Structure

A template contains the following information:

| Field       | Description                                     |
| ----------- | ----------------------------------------------- |
| `id`        | Unique template identifier.                     |
| `name`      | Tenant-defined template name.                   |
| `subject`   | Email subject containing optional placeholders. |
| `bodyText`  | Plain-text email content.                       |
| `bodyHtml`  | HTML email content.                             |
| `isActive`  | Indicates whether the template is active.       |
| `createdAt` | Template creation timestamp.                    |
| `updatedAt` | Timestamp of the latest update.                 |

The `TemplateResponseDto` exposes these template details.

### Request DTO

`TemplateRequestDto` accepts the following fields:

| Field      | Type   | Validation                        |
| ---------- | ------ | --------------------------------- |
| `name`     | String | Required, maximum 150 characters. |
| `subject`  | String | Required, maximum 255 characters. |
| `bodyText` | String | Required.                         |
| `bodyHtml` | String | Required.                         |

The same request structure is used for creating and updating templates.

## 4. API Endpoints

All template endpoints use the following base path:

`/templates`

The endpoints operate in the context of the authenticated tenant. The tenant is obtained from the `authenticatedTenant` request attribute.

### 4.1 Create Template

**Endpoint:** `POST /templates/create`

**Description:**

Creates a new template for the authenticated tenant.

The service performs the following operations:

1. Checks whether a template with the same name already exists for the tenant.
2. Throws `TemplateNameAlreadyExistsException` if a duplicate is found.
3. Creates and persists the new template.
4. Returns the created template as a `TemplateResponseDto`.

Template names are checked within the tenant's scope. Different tenants can therefore use the same template name.

**Request Body:**

```json
{
  "name": "Booking Confirmation",
  "subject": "Your booking is confirmed, {{customerName}}",
  "bodyText": "Hello {{customerName}}, your booking {{bookingId}} is confirmed.",
  "bodyHtml": "<h2>Hello {{customerName}}</h2><p>Your booking {{bookingId}} is confirmed.</p>"
}
```

**Response:** `201 Created`

The response contains the created template details, including its ID, active status, and timestamps.

### 4.2 Get All Templates

**Endpoint:** `GET /templates`

**Description:**

Retrieves templates belonging to the authenticated tenant.

The endpoint supports pagination and optional filtering.

**Supported filters:**

- Template name.
- Active/inactive status.

The service constructs a dynamic query using `TemplateSpecification`.

Pagination is handled using Spring Data's `Pageable` mechanism.

Only templates belonging to the authenticated tenant are considered.

> Refer to the controller's query parameter declarations for the exact parameter names and default pagination values.

### 4.3 Get Template by ID

**Endpoint:** `GET /templates/{id}`

**Description:**

Retrieves a template using its ID within the authenticated tenant's scope.

The service ensures that the requested template belongs to the authenticated tenant.

A template that does not exist within the tenant's scope results in `TemplateNotFoundException`.

Inactive templates are not available for notification rendering. The behavior of this endpoint for inactive templates should be distinguished from the rendering lookup if the controller uses a separate service method.

### 4.4 Update Template

**Endpoint:** `PUT /templates/{id}`

**Description:**

Updates an existing template belonging to the authenticated tenant.

The update operation checks whether the requested template name is already used by another template belonging to the same tenant.

The current template is excluded from the duplicate-name check, allowing a template to retain its existing name.

If another template already uses the requested name, `TemplateNameAlreadyExistsException` is thrown.

The request updates the template's supplied content fields.

### 4.5 Update Template Status

**Endpoint:** `PATCH /templates/{id}/status`

**Description:**

Changes the active status of an existing template.

The endpoint accepts `TemplateStatusRequestDto`.

The status operation supports:

- Activating an inactive template.
- Deactivating an active template.

The service prevents redundant status changes:

| Current state | Requested operation | Result                             |
| ------------- | ------------------- | ---------------------------------- |
| Active        | Activate            | `TemplateAlreadyActiveException`   |
| Inactive      | Deactivate          | `TemplateAlreadyInactiveException` |
| Inactive      | Activate            | Status changed to active           |
| Active        | Deactivate          | Status changed to inactive         |

Deactivation is a soft-delete approach. It preserves the template record rather than permanently removing it.

## 5. Template Rendering

`TemplateRenderer` is responsible for replacing dynamic placeholders in template content.

It supports variable substitution in:

- Subject.
- Plain-text body.
- HTML body.

### Placeholder Format

Variables use double curly braces:

`{{variableName}}`

For example:

```text
Hello {{customerName}},
Your booking {{bookingId}} has been confirmed.
```

Given the following variables:

```json
{
  "customerName": "John",
  "bookingId": "BK1001"
}
```

The rendered output becomes:

```text
Hello John,
Your booking BK1001 has been confirmed.
```

### Rendering Behavior

The renderer supports:

- Replacing placeholders using the supplied variable map.
- Replacing multiple occurrences of a variable.
- Rendering subject, text, and HTML content using the same variable map.
- Rejecting missing variables.
- Rejecting null variable values.
- Rejecting malformed or unresolved placeholders.

The renderer also uses safe replacement handling for Java regular-expression replacement strings, preventing replacement values containing special characters from being interpreted as regex replacement instructions.

### Rendering During Notification Creation

Template rendering is integrated with the Notifications module.

When a tenant creates a notification using a `templateId`:

1. The notification service retrieves the template using the template ID and authenticated tenant.
2. The service renders the subject.
3. It renders the plain-text body.
4. It renders the HTML body.
5. The rendered content is assigned to the notification.
6. The notification is saved and passed to the notification sender.

The notification stores the rendered content rather than depending on the template at the time of later retrieval.

Consequently, subsequent template edits or deactivation do not change the content already stored in an existing notification.

## 6. Tenant Isolation

Templates are tenant-scoped.

The authenticated tenant is obtained through the request attribute:

`authenticatedTenant`

Tenant restrictions apply to:

- Creating templates.
- Listing templates.
- Retrieving templates.
- Updating templates.
- Activating and deactivating templates.
- Retrieving templates for notification rendering.

Duplicate-name validation is also tenant-specific.

This ensures that tenants cannot access or modify templates owned by other tenants.

## 7. Exception Handling

The Templates module defines the following business exceptions:

| Exception                            | Purpose                                                                                                       |
| ------------------------------------ | ------------------------------------------------------------------------------------------------------------- |
| `TemplateAlreadyActiveException`     | Thrown when an already active template is activated again.                                                    |
| `TemplateAlreadyInactiveException`   | Thrown when an already inactive template is deactivated again.                                                |
| `TemplateNameAlreadyExistsException` | Thrown when a tenant attempts to use a template name already assigned to another template within that tenant. |
| `TemplateNotFoundException`          | Thrown when a requested template cannot be found within the tenant's scope.                                   |

These exceptions are handled by the application's global exception-handling layer.

The exact HTTP status codes and error response structure are defined in the global exception handler.

## 8. Persistence and Concurrency

The `TemplateRepository` is responsible for template persistence and retrieval.

The module uses tenant-scoped repository operations and JPA Specifications to support template management and filtering.

### Duplicate Name Protection

The service checks for duplicate template names within a tenant during creation and update.

The database design also uses a tenant-and-name uniqueness constraint to protect against duplicate records.

### Optimistic Locking

The Template entity uses JPA optimistic locking through a version field.

This helps prevent concurrent updates from silently overwriting one another.

If two requests attempt to update the same template version, a conflicting update can be detected rather than allowing the later write to silently replace the earlier one.

## 9. Integration with the Notifications Module

The Templates module is consumed by the Notifications module.

A tenant can create a notification using a previously created template rather than supplying the complete subject and message each time.

The integration uses:

- `TemplateService` to retrieve the tenant-owned template.
- `TemplateRenderer` to substitute dynamic variables.
- `NotificationServiceImpl` to construct and save the resulting notification.

This separation keeps template management and rendering logic independent of notification delivery.

## 10. Security Considerations

Template rendering supports HTML content, so HTML variable substitution requires special care.

### HTML-Context Escaping

`TemplateRenderer` provides separate rendering methods for plain-text and HTML content:

- `renderText()` renders text content, including the email subject and plain-text body.
- `renderHtml()` renders HTML content.

The HTML renderer uses OWASP Java Encoder's `Encode.forHtmlContent()` to escape dynamic variable values before inserting them into HTML content. This helps prevent a variable value containing HTML markup or script content from being interpreted as executable HTML.

For example, a value such as `<script>alert(1)</script>` is rendered as escaped text in an HTML content context rather than as a script element.

This escaping applies to **dynamic variable values**. Template HTML itself is tenant-managed content and is treated as trusted template markup. Escaping variables does not sanitize or make arbitrary template markup safe.

HTML content escaping is specific to the HTML content context. Values used in other contexts, such as HTML attributes, URLs, CSS, or JavaScript, require appropriate context-specific validation or encoding.

The separate renderer methods ensure that plain-text content is rendered without HTML encoding, while HTML content receives HTML-context escaping.

## 11. Testing

The Templates module has controller and service-level test coverage in the project.

The relevant test technologies include:

- JUnit 5
- Mockito
- MockMvc

## 12. Future Improvements

Potential production-oriented improvements include:

- More advanced template validation.
- Template preview functionality.
- Additional notification channel-specific template formats where required.
- Audit history for template modifications.
- Improved handling and observability of concurrent updates.

