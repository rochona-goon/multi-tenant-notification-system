package io.github.rochona.notificationsystem;

import io.github.rochona.notificationsystem.notification.model.Notification;
import io.github.rochona.notificationsystem.outbox.dto.ClaimedOutboxEventDto;
import io.github.rochona.notificationsystem.outbox.service.OutboxEventClaimService;
import io.github.rochona.notificationsystem.outbox.enums.OutboxEventStatus;
import io.github.rochona.notificationsystem.outbox.model.OutboxEvent;
import io.github.rochona.notificationsystem.outbox.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
        import static org.mockito.ArgumentMatchers.*;
        import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxEventClaimServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private OutboxEventClaimService outboxEventClaimService;

    @BeforeEach
    void setUp() {
        // Use reflection because these fields are injected using @Value.
        org.springframework.test.util.ReflectionTestUtils.setField(
                outboxEventClaimService, "batchSize", 10
        );

        org.springframework.test.util.ReflectionTestUtils.setField(
                outboxEventClaimService, "lockDurationSeconds", 60L
        );
    }


    @Test
    void shouldClaimEligibleEvents() {

        // Arrange
        Notification notification = new Notification();
        notification.setId(101L);

        OutboxEvent event = new OutboxEvent();
        event.setId(1L);
        event.setNotification(notification);
        event.setStatus(OutboxEventStatus.PENDING);
        event.setPublishAttemptCount(0);

        when(outboxEventRepository.findEventsToClaim(
                any(Instant.class), eq(10)
        )).thenReturn(List.of(event));

        // Act
        List<ClaimedOutboxEventDto> result =
                outboxEventClaimService.claimEvents();

        // Assert
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).eventId());
        assertEquals(101L, result.get(0).notificationId());

        assertEquals(OutboxEventStatus.PROCESSING, event.getStatus());
        assertEquals(1, event.getPublishAttemptCount());
        assertNotNull(event.getLockedUntil());

        verify(outboxEventRepository).findEventsToClaim(
                any(Instant.class), eq(10)
        );
    }


    @Test
    void shouldReturnEmptyListWhenNoEventsAreEligible() {

        // Arrange
        when(outboxEventRepository.findEventsToClaim(
                any(Instant.class), eq(10)
        )).thenReturn(List.of());

        // Act
        List<ClaimedOutboxEventDto> result = outboxEventClaimService.claimEvents();

        // Assert
        assertTrue(result.isEmpty());

        verify(outboxEventRepository).findEventsToClaim(
                any(Instant.class), eq(10)
        );
    }
}
