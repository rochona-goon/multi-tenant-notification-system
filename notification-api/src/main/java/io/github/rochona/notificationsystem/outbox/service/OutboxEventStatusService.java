package io.github.rochona.notificationsystem.outbox.service;

import io.github.rochona.notificationsystem.outbox.enums.OutboxEventStatus;
import io.github.rochona.notificationsystem.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OutboxEventStatusService {

    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public void markAsPublished(Long eventId) {
        var event = outboxEventRepository.findById(eventId)
                .orElseThrow(() ->
                        new IllegalStateException("Outbox event not found: " + eventId));

        event.setStatus(OutboxEventStatus.PUBLISHED);
        event.setPublishedAt(Instant.now());
        event.setLockedUntil(null);
        event.setLastError(null);
    }

    @Transactional
    public void scheduleRetry(Long eventId, String errorMessage) {
        var event = outboxEventRepository.findById(eventId)
                .orElseThrow(() ->
                        new IllegalStateException("Outbox event not found: " + eventId));

        // Exponential backoff: 5, 10, 20, 40... seconds, capped at 15 minutes.
        long exponent = Math.min(Math.max(event.getPublishAttemptCount() - 1, 0), 8);
        long delaySeconds = Math.min(5L * (1L << exponent), 900L);

        event.setStatus(OutboxEventStatus.PENDING);
        event.setNextAttemptAt(Instant.now().plusSeconds(delaySeconds));
        event.setLockedUntil(null);
        event.setLastError(errorMessage);
    }
}
