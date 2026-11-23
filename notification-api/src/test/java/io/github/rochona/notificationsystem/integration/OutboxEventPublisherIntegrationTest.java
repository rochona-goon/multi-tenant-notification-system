package io.github.rochona.notificationsystem.integration;

import io.github.rochona.notificationsystem.notification.enums.NotificationChannel;
import io.github.rochona.notificationsystem.notification.model.Notification;
import io.github.rochona.notificationsystem.notification.repository.NotificationRepository;
import io.github.rochona.notificationsystem.outbox.repository.OutboxEventRepository;
import io.github.rochona.notificationsystem.outbox.enums.OutboxEventStatus;
import io.github.rochona.notificationsystem.outbox.enums.OutboxEventType;
import io.github.rochona.notificationsystem.outbox.model.OutboxEvent;
import io.github.rochona.notificationsystem.outbox.service.OutboxEventPublisher;
import io.github.rochona.notificationsystem.tenant.model.Tenant;
import io.github.rochona.notificationsystem.tenant.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
public class OutboxEventPublisherIntegrationTest {

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Container
    static final RabbitMQContainer rabbit  = new RabbitMQContainer("rabbitmq:4-management");

    static void configureProperties(DynamicPropertyRegistry registry){
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);
    }

    @Autowired
    private OutboxEventPublisher outboxEventPublisher;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private NotificationRepository notificationRepository;



    @Test
    void shouldPublishClaimedEventAndMarkItPublished(){

        OutboxEvent event = outboxEventSetUp();

        Long eventId = event.getId();
        Long notificationId = event.getNotification().getId();

        // Remove any previous messages from the queue
        rabbitTemplate.execute(channel -> {
            channel.queuePurge("notification.delivery.queue");
            return null;
        });

        // Act
        outboxEventPublisher.publishPendingEvents();

        // Assert:Outbox event is PUBLISHED
        OutboxEvent outboxEvent = outboxEventRepository.findById(eventId).orElseThrow();

        assertEquals(OutboxEventStatus.PUBLISHED, outboxEvent.getStatus());

        // Assert: RabbitMQ delivered the notification ID to the configured queue.
        Object receivedMessage = rabbitTemplate.receiveAndConvert(
                "notification.delivery.queue",
                5000
        );

        assertNotNull(receivedMessage);
        assertEquals(notificationId.toString(), receivedMessage.toString());

        // Assert: the queue has no second copy of this message.
        Object duplicateMessage =
                rabbitTemplate.receiveAndConvert(
                        "notification.delivery.queue",
                        1000
                );

        assertNull(duplicateMessage);

    }


    @Test
    void shouldRetryClaimedEventAndNotMarkItPublished(){

        OutboxEvent event = outboxEventSetUp();

        Long eventId = event.getId();

        // Remove any previous messages from the queue
        rabbitTemplate.execute(channel -> {
            channel.queuePurge("notification.delivery.queue");
            return null;
        });

        String routingKey = "notification.delivery";

        try{
            // Set invalid routing key for publisher
            ReflectionTestUtils.setField(
                    outboxEventPublisher,
                    "routingKey",
                    "invalid.routing.key"
            );

            // Act
            outboxEventPublisher.publishPendingEvents();

            // Assert: Outbox event reload & status not PUBLISHED
            OutboxEvent outboxEvent = outboxEventRepository.findById(eventId).orElseThrow();

            assertNotEquals(OutboxEventStatus.PUBLISHED, outboxEvent.getStatus());
            assertNotNull(outboxEvent.getNextAttemptAt());
            assertTrue(outboxEvent.getNextAttemptAt().isAfter(Instant.now()));
            assertNotNull(outboxEvent.getLastError());

            // Assert: RabbitMQ delivered the notification ID to the configured queue.
            Object receivedMessage = rabbitTemplate.receiveAndConvert(
                    "notification.delivery.queue",
                    1000
            );

            assertNull(receivedMessage);

        } finally {
            // Set the original routingKey
            ReflectionTestUtils.setField(
                    outboxEventPublisher,
                    "routingKey",
                    routingKey
            );
        }

    }

    @Test
    void NACKTest() throws Exception{

        OutboxEvent event = outboxEventSetUp();
        Long eventId = event.getId();

        // test nack setup
        String testExchange = "nack.test.exchange";
        String testQueue = "nack.test.queue";
        String testRoutingKey = "nack.test.routing";

        Map<String, Object> queueArgs = new HashMap<>();

        // set to reject
        queueArgs.put("x-max-length", 1);
        queueArgs.put("x-overflow", "reject-publish");

        try {
            rabbitTemplate.execute(channel -> {
                        //  1. Declare exchange, queue and binding
                        channel.exchangeDeclare(testExchange, "direct", false);
                        channel.queueDeclare(testQueue, true, false, false, queueArgs);
                        channel.queueBind(testQueue, testExchange, testRoutingKey);
                        return null;

                    });

            // 2. Publish filler message and wait for broker confirmation
                CorrelationData fillerConfirm = new CorrelationData(UUID.randomUUID().toString());

                rabbitTemplate.convertAndSend(testExchange, testRoutingKey, "filler", fillerConfirm);
                CorrelationData.Confirm confirm = fillerConfirm.getFuture().get(5, TimeUnit.SECONDS);

                assertTrue(confirm.ack(), "RabbitMQ should ACK the filler message");


            // 3. Verify queue is full
                rabbitTemplate.execute(channel -> {
                    int messageCount = channel.queueDeclarePassive(testQueue).getMessageCount();
                    assertEquals(1, messageCount);

                    return null;
                });


            // 4. Temporarily set the fields of Publisher to the test values
            ReflectionTestUtils.setField(
                    outboxEventPublisher,
                    "exchangeName",
                    testExchange
            );

            ReflectionTestUtils.setField(
                    outboxEventPublisher,
                    "routingKey",
                    testRoutingKey
            );


            // Act :

            outboxEventPublisher.publishPendingEvents();


            OutboxEvent outboxEvent = outboxEventRepository.findById(eventId).orElseThrow();

            // Assert

            assertNotEquals(
                    OutboxEventStatus.PUBLISHED,
                    outboxEvent.getStatus()
            );

            assertNotNull(outboxEvent.getNextAttemptAt());
            assertNotNull(outboxEvent.getLastError());
            assertTrue(
                    outboxEvent.getLastError().contains("negatively acknowledged"),
                    "Expected a RabbitMQ negative publisher confirmation"
            );

        }finally {
            rabbitTemplate.execute(channel -> {
                channel.queueDelete(testQueue);
                channel.exchangeDelete(testExchange);

                return null;
            });

            ReflectionTestUtils.setField(
                    outboxEventPublisher,
                    "exchangeName",
                    "notification.exchange"
            );

            ReflectionTestUtils.setField(
                    outboxEventPublisher,
                    "routingKey",
                    "notification.delivery"
            );
        }
    }


    private OutboxEvent outboxEventSetUp(){
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

        return event;
    }

}
