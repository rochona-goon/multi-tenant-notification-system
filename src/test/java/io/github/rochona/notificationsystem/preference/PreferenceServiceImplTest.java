package io.github.rochona.notificationsystem.preference;

import io.github.rochona.notificationsystem.preference.dto.PreferenceRequestDto;
import io.github.rochona.notificationsystem.preference.dto.PreferenceResponseDto;
import io.github.rochona.notificationsystem.preference.exception.PreferenceDoesNotExistException;
import io.github.rochona.notificationsystem.preference.model.Preference;
import io.github.rochona.notificationsystem.preference.repository.PreferenceRepository;
import io.github.rochona.notificationsystem.preference.service.PreferenceServiceImpl;
import io.github.rochona.notificationsystem.tenant.model.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PreferenceServiceImplTest {

    @Mock
    private PreferenceRepository preferenceRepository;

    @InjectMocks
    private PreferenceServiceImpl preferenceService;

    private Tenant tenant;

    @BeforeEach
    void setUp(){
        tenant = new Tenant();
        tenant.setId(1L);
        tenant.setName("Test Tenant");
    }


    // ------------- upsertPreference ------------>

    @Test
    void shouldCreatePreferenceSuccessfully(){

        // Arrange

        Preference response = new Preference();
        response.setId(1L);
        response.setEmailEnabled(true);
        response.setSmsEnabled(true);
        response.setPushEnabled(true);
        response.setTenant(tenant);
        response.setUserId("testuser101");
        response.setCreatedAt(Instant.now());
        response.setUpdatedAt(Instant.now());
        response.setVersion(0L);

        Preference preference = new Preference();
        preference.setEmailEnabled(true);
        preference.setSmsEnabled(true);
        preference.setPushEnabled(true);
        preference.setTenant(tenant);
        preference.setUserId("testuser101");

        PreferenceRequestDto request = new PreferenceRequestDto();
        request.setEmailEnabled(true);
        request.setSmsEnabled(true);
        request.setPushEnabled(true);

        when(preferenceRepository.findByTenant_IdAndUserId(eq(tenant.getId()), eq("testuser101")))
                .thenReturn(Optional.empty());

        when(preferenceRepository.saveAndFlush(any(Preference.class)))
                .thenReturn(response);

        // Act

        PreferenceResponseDto result = preferenceService.upsertPreference(tenant, "testuser101", request);

        // Assert

        assertNotNull(result);
        assertEquals("testuser101", result.getUserId());
        assertTrue(result.isEmailEnabled());
        assertTrue(result.isPushEnabled());
        assertTrue(result.isSmsEnabled());


        // Verify the call

        verify(preferenceRepository).findByTenant_IdAndUserId(eq(1L), eq("testuser101"));

        ArgumentCaptor<Preference> preferenceCaptor = ArgumentCaptor.forClass(Preference.class);
        verify(preferenceRepository).saveAndFlush(preferenceCaptor.capture());

        Preference savedPreference = preferenceCaptor.getValue();

        // Assert

        assertEquals(tenant, savedPreference.getTenant());
        assertEquals("testuser101", savedPreference.getUserId());
        assertTrue(savedPreference.isEmailEnabled());
        assertTrue(savedPreference.isPushEnabled());
        assertTrue(savedPreference.isSmsEnabled());

    }


    @Test
    void shouldUpdateExistingPreferenceSuccessfully(){

        // Arrange

        Preference response = new Preference();
        response.setId(1L);
        response.setEmailEnabled(true);
        response.setSmsEnabled(false);
        response.setPushEnabled(false);
        response.setTenant(tenant);
        response.setUserId("testuser101");
        response.setCreatedAt(Instant.now());
        response.setUpdatedAt(Instant.now());
        response.setVersion(1L);

        Preference preference = new Preference();
        preference.setEmailEnabled(true);
        preference.setSmsEnabled(true);
        preference.setPushEnabled(true);
        preference.setTenant(tenant);
        preference.setUserId("testuser101");

        PreferenceRequestDto request = new PreferenceRequestDto();
        request.setEmailEnabled(true);
        request.setSmsEnabled(false);
        request.setPushEnabled(false);

        when(preferenceRepository.findByTenant_IdAndUserId(eq(tenant.getId()), eq("testuser101")))
                .thenReturn(Optional.of(preference));

        when(preferenceRepository.saveAndFlush(any(Preference.class)))
                .thenReturn(response);

        // Act

        PreferenceResponseDto result = preferenceService.upsertPreference(tenant, "testuser101", request);

        // Assert

        assertNotNull(result);
        assertEquals("testuser101", result.getUserId());
        assertTrue(result.isEmailEnabled());
        assertFalse(result.isPushEnabled());
        assertFalse(result.isSmsEnabled());


        // Verify the call

        verify(preferenceRepository).findByTenant_IdAndUserId(eq(tenant.getId()), eq("testuser101"));


        ArgumentCaptor<Preference> preferenceCaptor = ArgumentCaptor.forClass(Preference.class);

        verify(preferenceRepository).saveAndFlush(preferenceCaptor.capture());

        Preference savedPreference = preferenceCaptor.getValue();

        // Assert

        assertSame(preference, savedPreference);
        assertTrue(savedPreference.isEmailEnabled());
        assertFalse(savedPreference.isPushEnabled());
        assertFalse(savedPreference.isSmsEnabled());

    }


    @Test
    void shouldCreatePreferenceWithAllChannelsDisabled(){

        // Arrange

        Preference response = new Preference();
        response.setId(1L);
        response.setEmailEnabled(false);
        response.setSmsEnabled(false);
        response.setPushEnabled(false);
        response.setTenant(tenant);
        response.setUserId("testuser101");
        response.setCreatedAt(Instant.now());
        response.setUpdatedAt(Instant.now());
        response.setVersion(0L);

        Preference preference = new Preference();
        preference.setEmailEnabled(false);
        preference.setSmsEnabled(false);
        preference.setPushEnabled(false);
        preference.setTenant(tenant);
        preference.setUserId("testuser101");

        PreferenceRequestDto request = new PreferenceRequestDto();
        request.setEmailEnabled(false);
        request.setSmsEnabled(false);
        request.setPushEnabled(false);

        when(preferenceRepository.findByTenant_IdAndUserId(eq(tenant.getId()), eq("testuser101")))
                .thenReturn(Optional.empty());

        when(preferenceRepository.saveAndFlush(any(Preference.class)))
                .thenReturn(response);

        // Act

        PreferenceResponseDto result = preferenceService.upsertPreference(tenant, "testuser101", request);

        // Assert

        assertNotNull(result);
        assertEquals("testuser101", result.getUserId());
        assertFalse(result.isEmailEnabled());
        assertFalse(result.isPushEnabled());
        assertFalse(result.isSmsEnabled());


        // Verify the call

        verify(preferenceRepository).findByTenant_IdAndUserId(eq(1L), eq("testuser101"));

        ArgumentCaptor<Preference> preferenceCaptor = ArgumentCaptor.forClass(Preference.class);
        verify(preferenceRepository).saveAndFlush(preferenceCaptor.capture());

        Preference savedPreference = preferenceCaptor.getValue();

        // Assert

        assertEquals(tenant, savedPreference.getTenant());
        assertEquals("testuser101", savedPreference.getUserId());
        assertFalse(savedPreference.isEmailEnabled());
        assertFalse(savedPreference.isPushEnabled());
        assertFalse(savedPreference.isSmsEnabled());

    }



    // ------------- getPreference ------------->

    @Test
    void shouldReturnPreferenceWhenItExists(){

        // Arrange

        Preference response = new Preference();
        response.setId(1L);
        response.setEmailEnabled(true);
        response.setSmsEnabled(true);
        response.setPushEnabled(false);
        response.setTenant(tenant);
        response.setUserId("testuser101");
        response.setCreatedAt(Instant.now());
        response.setUpdatedAt(Instant.now());
        response.setVersion(1L);

        when(preferenceRepository.findByTenant_IdAndUserId(eq(tenant.getId()), eq("testuser101")))
                .thenReturn(Optional.of(response));


        // Act

        PreferenceResponseDto result = preferenceService.getPreference(tenant.getId(), "testuser101");


        // Assert

        assertNotNull(result);
        assertEquals("testuser101", result.getUserId());
        assertTrue(result.isEmailEnabled());
        assertFalse(result.isPushEnabled());
        assertTrue(result.isSmsEnabled());


        // Verify the call

        verify(preferenceRepository).findByTenant_IdAndUserId(eq(tenant.getId()), eq("testuser101"));

    }


    @Test
    void shouldThrowExceptionWhenPreferenceDoesNotExist(){

        // Arrange

        when(preferenceRepository.findByTenant_IdAndUserId(eq(tenant.getId()), eq("testuser101")))
                .thenReturn(Optional.empty());


        // Act & Assert

        assertThrows(
                PreferenceDoesNotExistException.class,
                () -> preferenceService.getPreference(tenant.getId(), "testuser101")
        );


        // Verify the call

        verify(preferenceRepository).findByTenant_IdAndUserId(eq(tenant.getId()), eq("testuser101"));


    }



    // ------------ getAllPreferences ----------->


    @Test
    void shouldReturnAllPreferencesForTenant(){

        // Arrange

        Preference response = new Preference();
        response.setId(1L);
        response.setEmailEnabled(true);
        response.setSmsEnabled(true);
        response.setPushEnabled(false);
        response.setTenant(tenant);
        response.setUserId("testuser101");
        response.setCreatedAt(Instant.now());
        response.setUpdatedAt(Instant.now());
        response.setVersion(1L);

        Preference response1 = new Preference();
        response1.setId(2L);
        response1.setEmailEnabled(true);
        response1.setSmsEnabled(false);
        response1.setPushEnabled(false);
        response1.setTenant(tenant);
        response1.setUserId("testuser102");
        response1.setCreatedAt(Instant.now());
        response1.setUpdatedAt(Instant.now());
        response1.setVersion(2L);

        when(preferenceRepository.findAllByTenant_Id(eq(tenant.getId())))
                .thenReturn(List.of(response, response1));


        // Act

        List<PreferenceResponseDto> result = preferenceService.getAllPreferences(tenant.getId());

        // Assert

        assertNotNull(result);
        assertEquals(2, result.size());

        assertEquals("testuser101", result.get(0).getUserId());
        assertTrue(result.get(0).isEmailEnabled());
        assertTrue(result.get(0).isSmsEnabled());
        assertFalse(result.get(0).isPushEnabled());


        assertEquals("testuser102", result.get(1).getUserId());
        assertTrue(result.get(1).isEmailEnabled());
        assertFalse(result.get(1).isSmsEnabled());
        assertFalse(result.get(1).isPushEnabled());


        // Verify the call
        verify(preferenceRepository).findAllByTenant_Id(eq(tenant.getId()));
    }


    @Test
    void shouldReturnEmptyListWhenTenantHasNoPreferences(){

        // Arrange

        when(preferenceRepository.findAllByTenant_Id(eq(tenant.getId())))
                .thenReturn(List.of());


        // Act

        List<PreferenceResponseDto> result = preferenceService.getAllPreferences(tenant.getId());

        // Assert

        assertTrue(result.isEmpty());


        // Verify the call
        verify(preferenceRepository).findAllByTenant_Id(eq(tenant.getId()));


    }



    // ------------- deletePreference ------------>


    @Test
    void shouldDeletePreferenceWithoutThrowingException(){

        // Act & Assert

        assertDoesNotThrow(() ->
                preferenceService.deletePreference(tenant.getId(), "testuser101"));

        // Verify
        verify(preferenceRepository).deleteByTenant_IdAndUserId(eq(tenant.getId()), eq("testuser101"));


    }
}
