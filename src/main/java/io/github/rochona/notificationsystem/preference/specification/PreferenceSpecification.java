package io.github.rochona.notificationsystem.preference.specification;

import io.github.rochona.notificationsystem.notification.model.Notification;
import io.github.rochona.notificationsystem.preference.model.Preference;
import io.github.rochona.notificationsystem.template.model.Template;
import org.springframework.data.jpa.domain.Specification;

public class PreferenceSpecification {

    public static Specification<Preference> belongsToTenant(Long tenantId){

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("tenant").get("id"), tenantId);
        // Equivalent to 'WHERE tenant_id = ?'
    }

    public static Specification<Preference> hasUserId(String userId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("userId"), userId);
        // Equivalent to 'WHERE user_id = ? '
    }
}
