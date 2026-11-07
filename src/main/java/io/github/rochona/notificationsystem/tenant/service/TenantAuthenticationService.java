package io.github.rochona.notificationsystem.tenant.service;

import io.github.rochona.notificationsystem.tenant.ApiKeyHasher;
import io.github.rochona.notificationsystem.tenant.exception.InvalidApiKeyException;
import io.github.rochona.notificationsystem.tenant.model.Tenant;
import io.github.rochona.notificationsystem.tenant.repository.TenantRepository;
import org.springframework.stereotype.Service;

@Service
public class TenantAuthenticationService {
    private final TenantRepository tenantRepository;
    private final ApiKeyHasher apiKeyHasher;

    public TenantAuthenticationService(TenantRepository tenantRepository,
                                       ApiKeyHasher apiKeyHasher){
        this.tenantRepository = tenantRepository;
        this.apiKeyHasher = apiKeyHasher;
    }

    public Tenant authenticate(String rawApiKey){

        if(rawApiKey == null || rawApiKey.isBlank()){
            throw new InvalidApiKeyException("API key is missing");
        }

        String apiKeyHash = apiKeyHasher.hashApiKey(rawApiKey);

        return tenantRepository.findByApiKeyHash(apiKeyHash)
                .orElseThrow(() ->new InvalidApiKeyException("Invalid API key"));

    }
}
