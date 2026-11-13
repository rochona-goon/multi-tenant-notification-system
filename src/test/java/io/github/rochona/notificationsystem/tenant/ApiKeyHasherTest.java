package io.github.rochona.notificationsystem.tenant;

import io.github.rochona.notificationsystem.tenant.security.ApiKeyHasher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class ApiKeyHasherTest {
    private  final ApiKeyHasher apiKeyHasher = new ApiKeyHasher();

    @Test
    void shouldGenerateSameHashForSameApiKeys(){
        String apiKey = "ntf_live_test-key-123";

        String firstHash = apiKeyHasher.hashApiKey(apiKey);
        String secondHash = apiKeyHasher.hashApiKey(apiKey);

        assertEquals(firstHash, secondHash);
    }

    @Test
    void shouldGenerateDifferentHashForDifferentApiKeys(){
        String apiKey1 = "ntf_live_test-key-123";
        String apiKey2 = "ntf_live_test-key-456";

        String firstHash = apiKeyHasher.hashApiKey(apiKey1);
        String secondHash = apiKeyHasher.hashApiKey(apiKey2);

        assertNotEquals(firstHash, secondHash);
    }

    @Test
    void shouldGenerate64CharacterHexadecimalHash(){
        String apiKey = "ntf_live_test-key-123";

        String hash = apiKeyHasher.hashApiKey(apiKey);

        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]{64}"));
    }
}
