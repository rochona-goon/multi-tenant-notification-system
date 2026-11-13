package io.github.rochona.notificationsystem.tenant;

import io.github.rochona.notificationsystem.tenant.security.ApiKeyGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ApiKeyGeneratorTest {

    private final ApiKeyGenerator apiKeyGenerator = new ApiKeyGenerator();

    @Test
    void shouldGenerateApiKeyWithExpectedFormat(){
        String apiKey = apiKeyGenerator.generateApiKey();

        assertTrue(apiKey.startsWith("ntf_live_"));

        String encodedKey = apiKey.substring("ntf_live_".length());

        assertEquals(43, encodedKey.length());
    }

    @Test
    void shouldGenerateDifferentApiKeysOnSuccessiveCalls(){
        String apiKey1 = apiKeyGenerator.generateApiKey();
        String apiKey2 = apiKeyGenerator.generateApiKey();

        assertNotEquals(apiKey1,apiKey2);

    }
}
