package io.github.rochona.notificationsystem.integration;

import io.github.rochona.notificationsystem.notification.enums.NotificationChannel;
import io.github.rochona.notificationsystem.notification.model.Notification;
import io.github.rochona.notificationsystem.notification.repository.NotificationRepository;
import io.github.rochona.notificationsystem.outbox.repository.OutboxEventRepository;
import io.github.rochona.notificationsystem.outbox.dto.ClaimedOutboxEventDto;
import io.github.rochona.notificationsystem.outbox.enums.OutboxEventStatus;
import io.github.rochona.notificationsystem.outbox.enums.OutboxEventType;
import io.github.rochona.notificationsystem.outbox.model.OutboxEvent;
import io.github.rochona.notificationsystem.outbox.service.OutboxEventClaimService;
import io.github.rochona.notificationsystem.tenant.model.Tenant;
import io.github.rochona.notificationsystem.tenant.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(OutboxEventClaimService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OutboxEventClaimIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private OutboxEventClaimService outboxEventClaimService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;


    @Test
    void shouldClaimPendingOutboxEvent() {

        // Arrange: create and save a tenant
        Tenant tenant = new Tenant();
        tenant.setName("Claim Test Tenant");
        tenant.setApiKeyHash(UUID.randomUUID().toString());

        tenant = tenantRepository.saveAndFlush(tenant);

        // Arrange: create and save a notification
        Notification notification = new Notification();
        notification.setSubject("Claim test");
        notification.setMessage("Testing outbox event claiming");
        notification.setRecipient("test@example.com");
        notification.setChannel(NotificationChannel.EMAIL);
        notification.setTenant(tenant);

        notification = notificationRepository.saveAndFlush(notification);

        // Arrange: create a pending outbox event
        OutboxEvent event = new OutboxEvent();
        event.setNotification(notification);
        event.setEventType(OutboxEventType.NOTIFICATION_CREATED);
        event.setStatus(OutboxEventStatus.PENDING);
        event.setNextAttemptAt(Instant.now().minusSeconds(10));

        event = outboxEventRepository.saveAndFlush(event);

        Long eventId = event.getId();
        Long notificationId = notification.getId();

        // Act: claim eligible events
        List<ClaimedOutboxEventDto> claimedEvents = outboxEventClaimService.claimEvents();

        // Assert: the expected event was claimed
        assertEquals(1, claimedEvents.size());

        ClaimedOutboxEventDto claimedEvent = claimedEvents.getFirst();

        assertEquals(eventId, claimedEvent.eventId());
        assertEquals(notificationId, claimedEvent.notificationId());

        // Assert: the event's state was updated in PostgreSQL
        OutboxEvent claimedFromDatabase =
                outboxEventRepository.findById(eventId).orElseThrow();

        assertEquals(OutboxEventStatus.PROCESSING, claimedFromDatabase.getStatus());

        assertNotNull(claimedFromDatabase.getLockedUntil());
        assertTrue(claimedFromDatabase.getLockedUntil().isAfter(Instant.now()));

        assertEquals(1, claimedFromDatabase.getPublishAttemptCount());
    }



    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldSkipOutboxEventLockedByAnotherTransaction() throws Exception {

        // Arrange: create and save a tenant
        Tenant tenant = new Tenant();
        tenant.setName("Claim Test Tenant");
        tenant.setApiKeyHash(UUID.randomUUID().toString());

        tenant = tenantRepository.saveAndFlush(tenant);


        // Arrange: create and save a notification
        Notification notification = new Notification();
        notification.setSubject("Claim test");
        notification.setMessage("Testing outbox event claiming");
        notification.setRecipient("test@example.com");
        notification.setChannel(NotificationChannel.EMAIL);
        notification.setTenant(tenant);

        notification = notificationRepository.saveAndFlush(notification);


        // Arrange: create one eligible PENDING outbox event
        OutboxEvent event = new OutboxEvent();
        event.setNotification(notification);
        event.setEventType(OutboxEventType.NOTIFICATION_CREATED);
        event.setStatus(OutboxEventStatus.PENDING);
        event.setNextAttemptAt(Instant.now().minusSeconds(10));

        event = outboxEventRepository.saveAndFlush(event);

        Long eventId = event.getId();

        CountDownLatch lockAcquired = new CountDownLatch(1);
        CountDownLatch releaseLock = new CountDownLatch(1);

        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            // Transaction 1: lock the event and keep the
            // transaction open until we release it.
            Future<?> lockingTransaction = executor.submit(() ->
                    transactionTemplate.execute(status -> {
                        jdbcTemplate.queryForObject(
                                "SELECT id FROM outbox_events WHERE id = ? FOR UPDATE",
                                Long.class,
                                eventId
                        );

                        lockAcquired.countDown();

                        try {
                            if (!releaseLock.await(10, TimeUnit.SECONDS)) {
                                throw new RuntimeException(
                                        "Timed out waiting to release database lock"
                                );
                            }
                        } catch (InterruptedException exception) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException(exception);
                        }

                        return null;
                    })
            );

            // Ensure Transaction 1 has acquired the row lock.
            assertTrue(
                    lockAcquired.await(5, TimeUnit.SECONDS),
                    "The first transaction should acquire the row lock"
            );

            // Transaction 2: attempt to claim the same eligible event.
            List<ClaimedOutboxEventDto> claimedEvents =
                    outboxEventClaimService.claimEvents();

            // SKIP LOCKED means the locked row is not returned.
            assertTrue(
                    claimedEvents.isEmpty(),
                    "The publisher should skip an event locked by another transaction"
            );

            // Release Transaction 1 so it can commit.
            releaseLock.countDown();
            lockingTransaction.get(10, TimeUnit.SECONDS);

        } finally {
            // Always release the lock, even if an assertion fails.
            releaseLock.countDown();
            executor.shutdownNow();
        }
    }

}