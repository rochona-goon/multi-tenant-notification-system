package io.github.rochona.notificationsystem.notification.specification;

import io.github.rochona.notificationsystem.notification.enums.NotificationChannel;
import io.github.rochona.notificationsystem.notification.enums.NotificationStatus;
import io.github.rochona.notificationsystem.notification.model.Notification;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

public class NotificationSpecification {

    public static Specification<Notification> belongsToTenant(Long tenantId){


        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("tenant").get("id"), tenantId);
        // Equivalent to 'WHERE tenant_id = ?'
    }

    public static Specification<Notification> hasStatus(NotificationStatus status) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("status"), status);
        // Equivalent to 'WHERE status = "FAILED"'
    }

    public static Specification<Notification> hasChannel(NotificationChannel channel) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("channel"), channel);
        // Equivalent to 'WHERE channel = "EMAIL"'
    }

    public static Specification<Notification> hasUserId(String userId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("userId"), userId);
        // Equivalent to 'WHERE user_id = ? '
    }

    public static Specification<Notification> createdBetween(LocalDate fromDate, LocalDate toDate) {

        Instant start = fromDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = toDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.and(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), start),
                        criteriaBuilder.lessThan(root.get("createdAt"), end));
        /*
        GET /notifications?fromDate=2026-11-01&toDate=2026-11-13

        ... created_at >= '2026-11-01T00:00:00Z' AND created_at < '2026-11-14T00:00:00Z'
        * **/
    }

    public static Specification<Notification> createdFrom(LocalDate fromDate) {
        Instant start = fromDate.atStartOfDay(ZoneOffset.UTC).toInstant();

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), start);
    }

    public static Specification<Notification> createdUntil(LocalDate toDate) {
        Instant end = toDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThan(root.get("createdAt"), end);
    }
}
