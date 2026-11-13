package io.github.rochona.notificationsystem.tenant;

import io.github.rochona.notificationsystem.tenant.security.ApiKeyGenerator;
import io.github.rochona.notificationsystem.tenant.security.ApiKeyHasher;
import org.junit.jupiter.api.Test;

public class TenantKeyMigrationTest {

    @Test
    void generateReplacementKeyForExistingTenant(){
        ApiKeyGenerator apiKeyGenerator = new ApiKeyGenerator();
        ApiKeyHasher apiKeyHasher = new ApiKeyHasher();

        String rawApiKey = apiKeyGenerator.generateApiKey();
        String apiKeyHash = apiKeyHasher.hashApiKey(rawApiKey);

        System.out.println("new api key: "+rawApiKey);
        System.out.println("hashed api key: "+apiKeyHash);
    }
}
