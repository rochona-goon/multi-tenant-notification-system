package io.github.rochona.notificationsystem.template.repository;

import io.github.rochona.notificationsystem.template.model.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TemplateRepository extends JpaRepository<Template, Long>, JpaSpecificationExecutor<Template> {

    Optional<Template> findByIdAndTenantId(Long id, Long tenantId);

//    List<Template> findAllTemplates(Long  tenantId);

    List<Template> findAllByTenantIdAndIsActive(Long tenantId, boolean isActive);

    boolean existsByTenantIdAndName(Long tenantId, String name);

    boolean existsByTenantIdAndNameAndIdNot(Long tenantId, String name, Long id);
}
