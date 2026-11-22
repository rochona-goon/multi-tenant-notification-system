package io.github.rochona.notificationsystem.outbox.model;

import io.github.rochona.notificationsystem.notification.model.Notification;
import io.github.rochona.notificationsystem.outbox.enums.OutboxEventStatus;
import io.github.rochona.notificationsystem.outbox.enums.OutboxEventType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "outbox_events",
        indexes = {
                @Index(
                        name = "idx_outbox_status_next_attempt",
                        columnList = "status, next_attempt_at"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_outbox_notification",
                        columnNames = "notification_id"
                )
        }
)
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "notification_id",
            nullable = false
    )
    private Notification notification;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private OutboxEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxEventStatus status = OutboxEventStatus.PENDING;

    @Column(name = "publish_attempt_count", nullable = false)
    private int publishAttemptCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;

        if (nextAttemptAt == null) {
            nextAttemptAt = now;
        }
    }
}
