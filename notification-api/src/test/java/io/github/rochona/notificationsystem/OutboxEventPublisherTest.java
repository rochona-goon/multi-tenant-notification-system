package io.github.rochona.notificationsystem;

import io.github.rochona.notificationsystem.outbox.dto.ClaimedOutboxEventDto;
import io.github.rochona.notificationsystem.outbox.service.OutboxEventClaimService;
import io.github.rochona.notificationsystem.outbox.service.OutboxEventPublisher;
import io.github.rochona.notificationsystem.outbox.service.OutboxEventStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
        import static org.mockito.ArgumentMatchers.*;
        import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherTest {

    @Mock
    private OutboxEventClaimService outboxEventClaimService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private OutboxEventStatusService outboxEventStatusService;

    @InjectMocks
    private OutboxEventPublisher outboxEventPublisher;


    @BeforeEach
    void setUp() {
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

    @Test
    void shouldPublishNotificationAndMarkEventAsPublished() throws Exception {

        // Arrange
        ClaimedOutboxEventDto event =
                new ClaimedOutboxEventDto(1L, 101L);

        when(outboxEventClaimService.claimEvents())
                .thenReturn(List.of(event));

        doAnswer(invocation -> {
            CorrelationData correlationData = invocation.getArgument(4);

            correlationData.getFuture().complete(
                    new CorrelationData.Confirm(true, null)
            );

            return null;
        }).when(rabbitTemplate).convertAndSend(
                anyString(),
                anyString(),
                any(),
                any(),
                any(CorrelationData.class)
        );

        // Act
        outboxEventPublisher.publishPendingEvents();

        // Assert
        verify(rabbitTemplate).convertAndSend(
                anyString(),
                anyString(),
                eq("101"),
                any(),
                any(CorrelationData.class)
        );

        verify(outboxEventStatusService).markAsPublished(1L);
        verify(outboxEventStatusService, never())
                .scheduleRetry(anyLong(), anyString());
    }

    @Test
    void shouldScheduleRetryWhenRabbitMqNegativelyAcknowledges()
            throws Exception {

        // Arrange
        ClaimedOutboxEventDto event =
                new ClaimedOutboxEventDto(1L, 101L);

        when(outboxEventClaimService.claimEvents())
                .thenReturn(List.of(event));

        doAnswer(invocation -> {
            CorrelationData correlationData = invocation.getArgument(4);

            correlationData.getFuture().complete(
                    new CorrelationData.Confirm(false, "Broker rejected message")
            );

            return null;
        }).when(rabbitTemplate).convertAndSend(
                anyString(),
                anyString(),
                any(),
                any(),
                any(CorrelationData.class)
        );

        // Act
        outboxEventPublisher.publishPendingEvents();

        // Assert
        verify(outboxEventStatusService, never()).markAsPublished(1L);

        verify(outboxEventStatusService).scheduleRetry(
                eq(1L),
                contains("Broker rejected message")
        );
    }
}

