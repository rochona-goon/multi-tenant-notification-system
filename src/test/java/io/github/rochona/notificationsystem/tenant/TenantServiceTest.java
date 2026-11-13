package io.github.rochona.notificationsystem.tenant;

import io.github.rochona.notificationsystem.tenant.dto.TenantRegistrationResponse;
import io.github.rochona.notificationsystem.tenant.model.Tenant;
import io.github.rochona.notificationsystem.tenant.repository.TenantRepository;
import io.github.rochona.notificationsystem.tenant.security.ApiKeyGenerator;
import io.github.rochona.notificationsystem.tenant.security.ApiKeyHasher;
import io.github.rochona.notificationsystem.tenant.service.TenantService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private ApiKeyGenerator apiKeyGenerator;

    @Mock
    private ApiKeyHasher apiKeyHasher;

    @InjectMocks
    private TenantService tenantService;

    @Test
    void shouldSaveTenantWithHashedApiKeyAndReturnRawKey(){
        // Arrange

        String rawApiKey = "ntf_live_example";
        String apiKeyHash = "hashed-api-key";

        when(apiKeyGenerator.generateApiKey())
                .thenReturn(rawApiKey);

        when(apiKeyHasher.hashApiKey(rawApiKey))
                .thenReturn(apiKeyHash);


        when(tenantRepository.save(any(Tenant.class)))
                .thenAnswer(invocation -> {
                    Tenant tenant = invocation.getArgument(0);
                    tenant.setId(1L);

                    return tenant;
                });

        // Act

        TenantRegistrationResponse response = tenantService.registerTenant("Test Tenant");

        // Captures the tenant object that is being passed to the tenant repo in the service layer
        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);

        verify(tenantRepository, times(1)).save(tenantCaptor.capture());

        Tenant savedTenant = tenantCaptor.getValue();

        // Assert

        // Assert with the Captor result
        assertEquals("Test Tenant", savedTenant.getName());
        assertEquals(apiKeyHash, savedTenant.getApiKeyHash());
        assertNotEquals(rawApiKey, savedTenant.getApiKeyHash());


        // Assert with the Response
        assertEquals(1L, response.getTenantId());
        assertEquals("Test Tenant", response.getTenantName());
        assertEquals(rawApiKey, response.getApiKey());

        verify(apiKeyGenerator).generateApiKey();
        verify(apiKeyHasher).hashApiKey(rawApiKey);


    }

}
