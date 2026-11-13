package io.github.rochona.notificationsystem.preference;

import io.github.rochona.notificationsystem.tenant.model.Tenant;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class PreferenceServiceImpl implements PreferenceService{

    private final PreferenceRepository preferenceRepository;

    @Override
    public PreferenceResponseDto upsertPreference(Tenant tenant, String userId, PreferenceRequestDto request) {
        // check if preference exists for Update
        Optional<Preference> preferenceOptional = preferenceRepository.findByTenant_IdAndUserId(tenant.getId(), userId);

        Preference preference;

//      preference = preferenceOptional.orElseGet(Preference::new);
        if(preferenceOptional.isPresent()) {
            preference = preferenceOptional.get();
        }
        else {
            preference = new Preference();
            preference.setTenant(tenant);
            preference.setUserId(userId);
        }


        preference.setEmailEnabled(request.getEmailEnabled());
        preference.setSmsEnabled(request.getSmsEnabled());
        preference.setPushEnabled(request.getPushEnabled());

        return toResponseDto(savePreference(preference));
    }


    @Override
    @Transactional(readOnly = true)
    public PreferenceResponseDto getPreference(Long tenantId, String userId) {
        // check if preference is present or not
        Preference preference = preferenceRepository.findByTenant_IdAndUserId(tenantId, userId).orElseThrow(
                () -> new PreferenceDoesNotExistException("Preference does not exist for this user id")
        );

        return toResponseDto(preference);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PreferenceResponseDto> getAllPreferences(Long tenantId) {
        return toResponseDto(preferenceRepository.findAllByTenant_Id(tenantId));
    }

    @Override
    public void deletePreference(Long tenantId, String userId) {
        preferenceRepository.deleteByTenant_IdAndUserId(tenantId, userId);
    }


    private PreferenceResponseDto toResponseDto(Preference preference){

        PreferenceResponseDto responseDto = new PreferenceResponseDto();
        responseDto.setUserId(preference.getUserId());
        responseDto.setEmailEnabled(preference.isEmailEnabled());
        responseDto.setPushEnabled(preference.isPushEnabled());
        responseDto.setSmsEnabled(preference.isSmsEnabled());
        responseDto.setCreatedAt(preference.getCreatedAt());
        responseDto.setUpdatedAt(preference.getUpdatedAt());

        return responseDto;
    }


    private List<PreferenceResponseDto> toResponseDto(List<Preference> preferences){

        return preferences.stream().map(this::toResponseDto).toList();
    }




    // TO BE REVIEWED LATER
    private Preference savePreference(Preference preference){

        try {
            return preferenceRepository.saveAndFlush(preference);
        } catch (DataIntegrityViolationException exception) {

            Throwable cause = exception;

            while (cause != null) {
                if (cause instanceof ConstraintViolationException cve) {
                    if ("uk_preference_tenant_user".equals(cve.getConstraintName())) {
                        throw new PreferenceAlreadyExistsException("Preference already exists");
                    }
                }
                cause = cause.getCause();
            }
            throw exception;
        }
    }
}
