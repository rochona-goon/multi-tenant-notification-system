package io.github.rochona.notificationsystem.outbox.service;

import io.github.rochona.notificationsystem.outbox.model.OutboxEvent;
import io.github.rochona.notificationsystem.outbox.repository.OutboxEventRepository;
import io.github.rochona.notificationsystem.outbox.dto.ClaimedOutboxEventDto;
import io.github.rochona.notificationsystem.outbox.enums.OutboxEventStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxEventClaimService {

    private final OutboxEventRepository outboxEventRepository;

    @Value("${notification.outbox.batch-size:10}")
    private int batchSize;

    @Value("${notification.outbox.lock-duration-seconds:60}")
    private long lockDurationSeconds;

    @Transactional
    public List<ClaimedOutboxEventDto> claimEvents() {

        Instant now = Instant.now();

        List<OutboxEvent> events =
                outboxEventRepository.findEventsToClaim(now, batchSize);

        Instant lockedUntil = now.plusSeconds(lockDurationSeconds);

        for (OutboxEvent event : events) {
            event.setStatus(OutboxEventStatus.PROCESSING);
            event.setLockedUntil(lockedUntil);
            event.setPublishAttemptCount(event.getPublishAttemptCount() + 1);
        }

        return events.stream()
                .map(event -> new ClaimedOutboxEventDto(
                        event.getId(),
                        event.getNotification().getId()
                ))
                .toList();
    }
}
