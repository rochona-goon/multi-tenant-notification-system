package io.github.rochona.notificationsystem.preference.service;

import io.github.rochona.notificationsystem.preference.dto.PreferenceRequestDto;
import io.github.rochona.notificationsystem.preference.dto.PreferenceResponseDto;
import io.github.rochona.notificationsystem.tenant.model.Tenant;

import java.util.List;

public interface PreferenceService {

    PreferenceResponseDto upsertPreference(Tenant tenant, String userId, PreferenceRequestDto request);

    PreferenceResponseDto getPreference(Long tenantId, String userId);

    List<PreferenceResponseDto> getAllPreferences(Long tenantId);

    void deletePreference(Long tenantId, String userId);
}
