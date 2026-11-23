package io.github.rochona.deliveryworker.notification_attempt.model;

import io.github.rochona.deliveryworker.notification_attempt.enums.AttemptStatus;
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
        name = "notification_attempts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_notification_attempt_number",
                        columnNames = {"notification_id", "attempt_number"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_notification_attempts_notification",
                        columnList = "notification_id"
                )
        }
)
public class NotificationAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notification_id", nullable = false)
    private Long notificationId;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttemptStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "attempted_at", nullable = false, updatable = false)
    private Instant attemptedAt;

    @PrePersist
    protected void onCreate() {
        attemptedAt = Instant.now();
    }
}

