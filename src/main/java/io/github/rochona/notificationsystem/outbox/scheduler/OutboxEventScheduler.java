package io.github.rochona.notificationsystem.outbox.scheduler;

import io.github.rochona.notificationsystem.outbox.service.OutboxEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxEventScheduler {

    private final OutboxEventPublisher outboxEventPublisher;

    @Scheduled(
            fixedDelayString = "${notification.outbox.publisher-interval-ms:5000}"
    )
    public void publishPendingEvents() {
        outboxEventPublisher.publishPendingEvents();
    }
}
