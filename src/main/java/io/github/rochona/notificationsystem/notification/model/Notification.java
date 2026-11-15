package io.github.rochona.notificationsystem.notification.model;

import io.github.rochona.notificationsystem.notification.enums.NotificationChannel;
import io.github.rochona.notificationsystem.notification.enums.NotificationStatus;
import io.github.rochona.notificationsystem.tenant.model.Tenant;
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

    @Column(name = "html_message", columnDefinition = "TEXT")
    private String htmlMessage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(length = 100)
    private String userId;

    @Enumerated(value = EnumType.STRING)
    private NotificationStatus status = NotificationStatus.PENDING;

    private Instant createdAt;

    @Version
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;

    @PrePersist
    protected void onCreate(){
        createdAt = Instant.now();
    }
}
