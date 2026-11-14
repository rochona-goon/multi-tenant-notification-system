package io.github.rochona.notificationsystem.preference.repository;

import io.github.rochona.notificationsystem.preference.model.Preference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PreferenceRepository extends JpaRepository<Preference, Long> {
    Optional<Preference> findByTenant_IdAndUserId(Long tenantId, String userId);

    List<Preference> findAllByTenant_Id(Long tenantId);

    boolean existsByTenant_IdAndUserId(Long tenantId, String userId);

    void deleteByTenant_IdAndUserId(Long tenantId, String userId);
}
