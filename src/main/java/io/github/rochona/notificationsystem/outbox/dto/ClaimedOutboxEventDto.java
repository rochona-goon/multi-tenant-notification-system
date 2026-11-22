package io.github.rochona.notificationsystem.outbox.dto;

public record ClaimedOutboxEventDto(Long eventId,
                                    Long notificationId) {
}
