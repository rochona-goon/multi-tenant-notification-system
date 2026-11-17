package io.github.rochona.notificationsystem.template.specification;

import io.github.rochona.notificationsystem.notification.enums.NotificationStatus;
import io.github.rochona.notificationsystem.notification.model.Notification;
import io.github.rochona.notificationsystem.template.model.Template;
import org.springframework.data.jpa.domain.Specification;

public class TemplateSpecification {

    public static Specification<Template> belongsToTenant(Long tenantId){


        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("tenant").get("id"), tenantId);
        // Equivalent to 'WHERE tenant_id = ?'
    }


    public static Specification<Template> hasActiveStatus(Boolean active) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("isActive"), active);
        // Equivalent to 'WHERE active = true'
    }


    public static Specification<Template> hasName(String name) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(
                        criteriaBuilder.lower(
                                root.get("name")), "%" + name.toLowerCase() + "%");
        // Equivalent to 'WHERE name LIKE "%name%"'
    }

}
