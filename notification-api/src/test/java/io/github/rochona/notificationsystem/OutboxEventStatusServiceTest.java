package io.github.rochona.notificationsystem;

import io.github.rochona.notificationsystem.outbox.model.OutboxEvent;
import io.github.rochona.notificationsystem.outbox.repository.OutboxEventRepository;
import io.github.rochona.notificationsystem.outbox.enums.OutboxEventStatus;
import io.github.rochona.notificationsystem.outbox.service.OutboxEventStatusService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
        import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxEventStatusServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private OutboxEventStatusService outboxEventStatusService;

    @Test
    void shouldMarkEventAsPublished() {

        // Arrange
        OutboxEvent event = new OutboxEvent();
        event.setStatus(OutboxEventStatus.PROCESSING);
        event.setLockedUntil(Instant.now().plusSeconds(60));
        event.setLastError("Previous error");

        when(outboxEventRepository.findById(1L))
                .thenReturn(Optional.of(event));

        // Act
        outboxEventStatusService.markAsPublished(1L);

        // Assert
        assertEquals(OutboxEventStatus.PUBLISHED, event.getStatus());
        assertNotNull(event.getPublishedAt());
        assertNull(event.getLockedUntil());
        assertNull(event.getLastError());

        verify(outboxEventRepository).findById(1L);
    }

    @Test
    void shouldScheduleRetryWithExponentialBackoff() {

        // Arrange
        OutboxEvent event = new OutboxEvent();
        event.setStatus(OutboxEventStatus.PROCESSING);
        event.setPublishAttemptCount(2);
        event.setLockedUntil(Instant.now().plusSeconds(60));

        when(outboxEventRepository.findById(1L))
                .thenReturn(Optional.of(event));

        Instant beforeExecution = Instant.now();

        // Act
        outboxEventStatusService.scheduleRetry(
                1L,
                "Broker rejected message"
        );

        Instant afterExecution = Instant.now();

        // Assert
        assertEquals(OutboxEventStatus.PENDING, event.getStatus());
        assertNull(event.getLockedUntil());
        assertEquals("Broker rejected message", event.getLastError());

        // Attempt 2 produces a 10-second retry delay.
        assertTrue(
                event.getNextAttemptAt().compareTo(
                        beforeExecution.plusSeconds(10)
                ) >= 0
        );

        assertTrue(
                event.getNextAttemptAt().compareTo(
                        afterExecution.plusSeconds(10)
                ) <= 0
        );

        verify(outboxEventRepository).findById(1L);
    }
}
