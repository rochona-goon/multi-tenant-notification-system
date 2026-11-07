package io.github.rochona.notificationsystem.tenant.service;

import io.github.rochona.notificationsystem.tenant.ApiKeyGenerator;
import io.github.rochona.notificationsystem.tenant.ApiKeyHasher;
import io.github.rochona.notificationsystem.tenant.dto.TenantRegistrationResponse;
import io.github.rochona.notificationsystem.tenant.model.Tenant;
import io.github.rochona.notificationsystem.tenant.repository.TenantRepository;
import org.springframework.stereotype.Service;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final ApiKeyGenerator apiKeyGenerator;
    private final ApiKeyHasher apiKeyHasher;

    public TenantService(TenantRepository tenantRepository,
                         ApiKeyGenerator apiKeyGenerator,
                         ApiKeyHasher apiKeyHasher){
        this.tenantRepository = tenantRepository;
        this.apiKeyGenerator = apiKeyGenerator;
        this.apiKeyHasher = apiKeyHasher;
    }

    public TenantRegistrationResponse registerTenant(String name){
        String rawApiKey = apiKeyGenerator.generateApiKey();
        String apiKeyHash = apiKeyHasher.hashApiKey(rawApiKey);

        Tenant tenant = new Tenant();
        tenant.setName(name);
        tenant.setApiKeyHash(apiKeyHash);

        Tenant savedTenant = tenantRepository.save(tenant);

        TenantRegistrationResponse response = new TenantRegistrationResponse();
        response.setTenantId(savedTenant.getId());
        response.setTenantName(savedTenant.getName());
        response.setApiKey(rawApiKey);


        return response;
    }

}
