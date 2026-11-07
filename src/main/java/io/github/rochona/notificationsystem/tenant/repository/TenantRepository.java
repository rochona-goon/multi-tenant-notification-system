package io.github.rochona.notificationsystem.tenant.repository;

import io.github.rochona.notificationsystem.tenant.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findByApiKeyHash(String apiKey);
}
