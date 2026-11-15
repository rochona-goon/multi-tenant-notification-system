package io.github.rochona.notificationsystem.notification.repository;

import io.github.rochona.notificationsystem.notification.enums.NotificationStatus;
import io.github.rochona.notificationsystem.notification.model.Notification;
import io.github.rochona.notificationsystem.tenant.model.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends
        JpaRepository<Notification, Long>, JpaSpecificationExecutor<Notification> {

//    Page<Notification> findAllByTenant(Tenant tenant, Pageable pageable);

    Optional<Notification> findByIdAndTenant(Long id, Tenant tenant);

    boolean deleteByIdAndTenantAndStatus(Long id, Tenant tenant, NotificationStatus status);
}
