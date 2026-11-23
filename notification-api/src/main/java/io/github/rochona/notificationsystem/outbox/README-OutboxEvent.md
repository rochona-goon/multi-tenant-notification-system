# OutboxEvent Module

## 1. Overview

The OutboxEvent module implements the Transactional Outbox Pattern for asynchronous notification processing.

When a tenant's notification is accepted for asynchronous delivery, the Notification API persists the notification and its outbox event in the same database transaction. A scheduled publisher claims eligible outbox events and publishes notification-ID-based messages to RabbitMQ. The separate Delivery Worker consumes the messages and processes the notification.

The outbox prevents a notification from being committed to the database without a corresponding event record for asynchronous delivery.

### Key Features

- Transactional persistence of notification and outbox event.
- Scheduled outbox polling and batch publishing.
- Safe event claiming using PostgreSQL row locking.
- Concurrent publisher support using `FOR UPDATE SKIP LOCKED`.
- Claim lease to recover events left in processing after a publisher interruption.
- RabbitMQ publisher confirms and correlated message publishing.
- Handling of negative acknowledgements, confirm timeouts, exceptions, and unroutable messages.
- Retry scheduling with capped exponential backoff.
- Durable RabbitMQ exchange and queue configuration.
- Persistent RabbitMQ messages.
- Outbox event status tracking and optimistic locking.

## 2. Package Structure

```text
outbox/
├── dto/
│   └── ClaimedOutboxEventDto
├── enums/
│   ├── OutboxEventStatus
│   └── OutboxEventType
├── model/
│   └── OutboxEvent
├── repository/
│   └── OutboxEventRepository
├── scheduler/
│   └── OutboxEventScheduler
└── service/
    ├── OutboxEventClaimService
    ├── OutboxEventPublisher
    └── OutboxEventStatusService
```

### Component Responsibilities

| Package / Component | Responsibility |
| --- | --- |
| `dto/ClaimedOutboxEventDto` | Carries the claimed outbox event information used by the publishing flow. |
| `enums/OutboxEventStatus` | Defines the lifecycle statuses of an outbox event. |
| `enums/OutboxEventType` | Defines the type of event represented by an outbox record. |
| `model/OutboxEvent` | Stores the event associated with a notification and its publishing/retry metadata. |
| `repository/OutboxEventRepository` | Provides persistence and eligible-event selection/claiming operations. |
| `scheduler/OutboxEventScheduler` | Triggers the scheduled outbox publishing process. |
| `service/OutboxEventClaimService` | Claims eligible events for publishing while preventing concurrent publishers from claiming the same event. |
| `service/OutboxEventPublisher` | Publishes claimed events to RabbitMQ and handles broker confirms and publish failures. |
| `service/OutboxEventStatusService` | Updates event status and schedules retries after publish failures. |

## 3. Outbox Event Data Structure

Outbox events are persisted in the `outbox_events` table.

| Field | Description |
| --- | --- |
| `id` | Unique outbox event identifier. |
| `notification` | Lazy one-to-one association with the notification. |
| `notification_id` | Non-null, unique reference to the notification. |
| `eventType` | Event type, currently `NOTIFICATION_CREATED`. |
| `status` | Current outbox publishing status. |
| `publishAttemptCount` | Number of publishing attempts recorded. |
| `createdAt` | Event creation timestamp. |
| `publishedAt` | Timestamp when publishing was confirmed. |
| `nextAttemptAt` | Earliest time the event can be claimed for another attempt. |
| `lockedUntil` | Claim lease expiry timestamp. |
| `lastError` | Last recorded publishing error. |
| `version` | Optimistic-locking version. |

The entity initializes relevant timestamps during persistence using `@PrePersist`.

### Event Statuses

| Status | Meaning |
| --- | --- |
| `PENDING` | Event is waiting to be claimed and published. |
| `PROCESSING` | Event has been claimed by a publisher. |
| `PUBLISHED` | RabbitMQ publishing was confirmed successfully. |

An event in `PROCESSING` can become eligible for claiming again after its claim lease expires.

## 4. Transactional Outbox Flow

`NotificationServiceImpl.createNotification(...)` performs notification creation within a transaction.

For a notification eligible for delivery:

1. The notification is created and saved.
2. An outbox event is created for the notification.
3. The event is saved with event type `NOTIFICATION_CREATED` and status `PENDING`.
4. The transaction commits both records together.

If the transaction rolls back, neither the notification nor its outbox event is committed.

Notifications suppressed by tenant user preferences are saved with status `SUPPRESSED` and do not create an outbox event, because they should not be delivered.

## 5. Event Claiming

The publisher does not simply select and publish all pending rows. It first claims eligible events in the database.

The claim query considers:

- Events with status `PENDING` whose `nextAttemptAt` is due.
- Events with status `PROCESSING` whose claim lease has expired.

Eligible events are ordered oldest first and selected in a limited batch.

The query uses PostgreSQL:

```sql
FOR UPDATE SKIP LOCKED
```

This allows multiple publisher executions to claim separate rows without waiting on rows already locked by another publisher. It helps prevent the same event from being claimed concurrently by multiple publisher instances.

### Claim Configuration

| Setting | Value |
| --- | --- |
| Publisher fixed delay | 5 seconds |
| Claim batch size | 10 events |
| Claim lease | 60 seconds |

The claim lease allows an event to be recovered if a publisher stops after claiming it but before completing publishing.

## 6. RabbitMQ Publishing

The outbox publisher sends notification-ID-based messages to RabbitMQ. The message contains the notification identifier so that the Delivery Worker can retrieve the notification from the shared database.

### RabbitMQ Topology

| Component | Name |
| --- | --- |
| Exchange | `notification.exchange` |
| Exchange type | Direct |
| Queue | `notification.delivery.queue` |
| Routing key | `notification.delivery` |

The exchange and queue are durable, and published messages are persistent.

Publishing uses mandatory/returned-message handling so an unroutable message can be recognized rather than silently treated as successfully delivered to a queue.

### Publisher Confirms

The publisher uses `CorrelationData` with the outbox event ID to associate the broker confirm with the event being published.

The publisher waits up to 5 seconds for the confirm:

- **ACK:** The event is marked `PUBLISHED` and its `publishedAt` timestamp is recorded.
- **NACK:** The event is scheduled for retry.
- **Confirm timeout:** The event is scheduled for retry.
- **Publishing exception:** The event is scheduled for retry.
- **Returned/unroutable message:** The event is scheduled for retry.

A successful broker confirm means RabbitMQ accepted the publish according to the configured broker-confirm flow. It does not mean the notification has already been delivered to the recipient; delivery is performed by the Worker.

## 7. Retry Handling

When publishing does not succeed, the outbox event remains eligible for another attempt after a delay.

The publisher retry delay uses exponential backoff:

| Attempt delay sequence | Delay |
| --- | --- |
| First retry | 5 seconds |
| Second retry | 10 seconds |
| Subsequent retries | Doubles each time |
| Maximum delay | 900 seconds (15 minutes) |

The retry delay is capped at 900 seconds. No explicit maximum publishing-attempt count is configured in the current implementation.

Retry scheduling records the next eligible attempt time and the last publishing error.

The RabbitMQ topology also includes retry queues with 5-second and 10-second delays and a dead-letter queue (DLQ) for the delivery-processing flow. These are distinct from the database-backed outbox publisher retry schedule.

## 8. Persistence and Concurrency

The `OutboxEvent` entity uses optimistic locking through `@Version`.

The database includes:

- A unique constraint on `notification_id`, ensuring a notification has at most one associated outbox event.
- An index on `(status, next_attempt_at)` to support eligible-event lookup.
- Row locking with `FOR UPDATE SKIP LOCKED` for concurrent claiming.

These mechanisms support safe event claiming and reduce the chance of duplicate concurrent publishing attempts.

## 9. Integration with the Delivery Worker

The OutboxEvent module is part of the Notification API. It publishes notification identifiers to RabbitMQ; it does not send the email itself.

The Delivery Worker:

1. Consumes the RabbitMQ message.
2. Uses the notification ID to load the notification from the shared PostgreSQL database.
3. Processes delivery.
4. Acknowledges the message according to the Worker’s manual-acknowledgement flow.

The outbox publisher and Worker therefore have separate responsibilities: the publisher reliably transfers the event to the broker, while the Worker performs notification delivery.

## 10. Failure and Recovery Behavior

| Failure scenario | Outbox behavior |
| --- | --- |
| Notification transaction rolls back | Notification and outbox event are both rolled back. |
| Publisher stops after claiming an event | Event can be reclaimed after the 60-second lease expires. |
| RabbitMQ rejects the publish | Event is scheduled for retry. |
| Confirm is not received within the timeout | Event is scheduled for retry. |
| Message is returned as unroutable | Event is scheduled for retry. |
| Publisher receives a confirm ACK | Event is marked `PUBLISHED`. |

Because a publisher may not know whether a timed-out publish reached RabbitMQ, duplicate messages are possible in failure/retry scenarios. The consumer must therefore tolerate duplicate delivery through the system's idempotent processing behavior.

## 11. Testing

Outbox-related tests cover the transactional and concurrent publishing behavior, including:

- Notification and outbox event persistence within the same transaction.
- Eligible event claiming.
- Concurrent claiming behavior.
- Successful publishing and broker ACK handling.
- Retry scheduling for unroutable messages.
- Optimistic-locking behavior for claim/status updates.

The project uses JUnit 5, Mockito, and integration testing with PostgreSQL/RabbitMQ test infrastructure where configured.

## 12. Operational Notes

- Monitor the number and age of `PENDING` events.
- Monitor events that remain in `PROCESSING` beyond the claim lease.
- Track publish attempts and `lastError` for repeated failures.
- Ensure RabbitMQ exchange, queue, routing key, and retry/DLQ bindings match the application configuration.
- The outbox is shared with the Notification API's PostgreSQL database; the Delivery Worker uses the same database to load notification records.
