package io.github.rochona.notificationsystem.preference.model;

import io.github.rochona.notificationsystem.tenant.model.Tenant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
        name = "notification_preferences",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_preference_tenant_user",
                        columnNames = {"tenant_id", "user_id"}
                )
        }
)
public class Preference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false, length = 100)
    private String userId;

    @Column(name = "is_email_enabled", nullable = false)
    private boolean emailEnabled = true;

    @Column(name = "is_sms_enabled", nullable = false)
    private boolean smsEnabled = true;

    @Column(name = "is_push_enabled", nullable = false)
    private boolean pushEnabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
