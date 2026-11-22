package io.github.rochona.notificationsystem.preference.service;

import io.github.rochona.notificationsystem.notification.enums.NotificationChannel;
import io.github.rochona.notificationsystem.preference.dto.PreferenceRequestDto;
import io.github.rochona.notificationsystem.preference.dto.PreferenceResponseDto;
import io.github.rochona.notificationsystem.tenant.model.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PreferenceService {

    PreferenceResponseDto upsertPreference(Tenant tenant, String userId, PreferenceRequestDto request);

    PreferenceResponseDto getPreference(Long tenantId, String userId);

    Page<PreferenceResponseDto> getAllPreferences(Long tenantId, String userId, Pageable pageable);

    boolean isChannelEnabled(Long tenantId, String userId, NotificationChannel channel);

    void deletePreference(Long tenantId, String userId);
}
