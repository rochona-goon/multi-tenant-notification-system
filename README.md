# multi-tenant-notification-system
A multi-tenant notification platform built with Java 21 and Spring Boot, featuring asynchronous delivery, RabbitMQ, tenant isolation, and reliable message processing.


## Mailpit Email Integration

The application uses [Mailpit](https://mailpit.axllent.org/) as a local SMTP server for testing email delivery without sending emails to real recipients.

### Prerequisites

- Docker Desktop
- Java 21
- PostgreSQL

### Start Mailpit

```bash
docker run -d --name mailpit -p 8025:8025 -p 1025:1025 axllent/mailpit
```

- **SMTP:** `localhost:1025`
- **Web UI:** http://localhost:8025

If the container already exists, start it with `docker start mailpit`.

### Run Tests

Run the Mailpit integration test:

```powershell
./mvnw.cmd test "-Dtest=MailpitIntegrationTest"
```

Run notification service unit tests:

```powershell
./mvnw.cmd test "-Dtest=NotificationServiceImplTest"
```

The integration test requires PostgreSQL to be running and the `DB_PASSWORD` environment variable to be configured.

### Verify Email Delivery

After a successful integration test, open the Mailpit web UI and verify that the test email appears in the inbox.

The notification service also tracks delivery attempts using `PENDING`, `SENT`, and `FAILED` statuses.

**Note:** `SENT` indicates that the SMTP server accepted the email; it does not guarantee delivery to the recipient's actual inbox.

