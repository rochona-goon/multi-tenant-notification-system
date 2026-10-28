package io.github.rochona.notificationsystem.notification.model;

import io.github.rochona.notificationsystem.notification.enums.NotificationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String subject;
    private String recipient;
    private String message;

    @Enumerated(value = EnumType.STRING)
    private NotificationStatus status = NotificationStatus.PENDING;

    private Instant createdAt;

    @PrePersist
    protected void onCreate(){
        createdAt = Instant.now();
    }
}
