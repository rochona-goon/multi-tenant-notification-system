package io.github.rochona.notificationsystem.tenant;


import io.github.rochona.notificationsystem.tenant.exception.InvalidApiKeyException;
import io.github.rochona.notificationsystem.tenant.model.Tenant;
import io.github.rochona.notificationsystem.tenant.repository.TenantRepository;
import io.github.rochona.notificationsystem.tenant.security.ApiKeyHasher;
import io.github.rochona.notificationsystem.tenant.service.TenantAuthenticationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TenantAuthenticationServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private ApiKeyHasher apiKeyHasher;

    @InjectMocks
    private TenantAuthenticationService tenantAuthenticationService;

    @Test
    public void shouldAuthenticateTenantForValidApiKey(){
        // Arrange

        String rawApiKey = "ntf_live_valid";
        String apiKeyHash = "valid_hash";

        Tenant tenant = new Tenant();
        tenant.setId(1L);
        tenant.setName("Test Tenant");
        tenant.setApiKeyHash(apiKeyHash);

        when(apiKeyHasher.hashApiKey(rawApiKey))
                .thenReturn(apiKeyHash);

        when(tenantRepository.findByApiKeyHash(apiKeyHash))
                .thenReturn(Optional.of(tenant));

        // Act & Assert

        Tenant result = tenantAuthenticationService.authenticate(rawApiKey);

        assertSame(tenant, result);
        verify(apiKeyHasher).hashApiKey(rawApiKey);
        verify(tenantRepository).findByApiKeyHash(apiKeyHash);

    }

    @Test
    public void shouldThrowExceptionForInvalidApiKey(){

        // Arrange

        String rawApiKey = "ntf_live_invalid";
        String apiKeyHash = "unknown_hash";

        when(apiKeyHasher.hashApiKey(rawApiKey))
                .thenReturn(apiKeyHash);

        when(tenantRepository.findByApiKeyHash(apiKeyHash))
                .thenReturn(Optional.empty());

        // Assert
        assertThrows(
                InvalidApiKeyException.class,
                () -> tenantAuthenticationService.authenticate(rawApiKey)
        );

    }

    @Test
    public void shouldThrowExceptionForMissingApiKey(){

        assertThrows(
                InvalidApiKeyException.class,
                () -> tenantAuthenticationService.authenticate(null)
        );

        verifyNoInteractions(apiKeyHasher, tenantRepository);

    }

    @Test
   public void shouldThrowExceptionForBlankApiKey(){

        assertThrows(
                InvalidApiKeyException.class,
                () -> tenantAuthenticationService.authenticate(" ")
        );

        verifyNoInteractions(apiKeyHasher, tenantRepository);
    }

}
