package io.github.rochona.notificationsystem.template.model;

import io.github.rochona.notificationsystem.tenant.model.Tenant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
        name = "notification_templates",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_template_tenant_name",
                        columnNames = {"tenant_id", "name"}
                )
        }
)
public class Template {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "tenant_id", nullable = false)
        private Tenant tenant;

        @Column(nullable = false, length = 150)
        private String name;

        @Column(nullable = false, length = 255)
        private String subject;

        @Column(name = "body_text", nullable = false, columnDefinition = "TEXT")
        private String bodyText;

        @Column(name = "body_html", nullable = false, columnDefinition = "TEXT")
        private String bodyHtml;

        @Column(name = "is_active", nullable = false)
        private boolean isActive = true;

        @Column(nullable = false, updatable = false)
        private Instant createdAt;

        @Column(nullable = false)
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
