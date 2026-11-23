package io.github.rochona.deliveryworker.notification.model;


import io.github.rochona.deliveryworker.notification.enums.NotificationChannel;
import io.github.rochona.deliveryworker.notification.enums.NotificationStatus;
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

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(length = 100)
    private String userId;

    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    private Instant createdAt;

    @Version
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;
}
