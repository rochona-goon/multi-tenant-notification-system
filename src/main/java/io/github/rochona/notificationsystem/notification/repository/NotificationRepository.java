package io.github.rochona.notificationsystem.notification.repository;

import io.github.rochona.notificationsystem.notification.enums.NotificationStatus;
import io.github.rochona.notificationsystem.notification.model.Notification;
import io.github.rochona.notificationsystem.tenant.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findAllByTenant(Tenant tenant);

    Optional<Notification> findByIdAndTenant(Long id, Tenant tenant);

    boolean deleteByIdAndTenantAndStatus(Long id, Tenant tenant, NotificationStatus status);
}
