package io.github.rochona.notificationsystem.tenant.security;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class ApiKeyGenerator {
    private  static final String KEY_PREFIX = "ntf_live_";
    private static final int KEY_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateApiKey(){
        byte[] randomBytes = new byte[KEY_BYTES];
        secureRandom.nextBytes(randomBytes); // fills the array with random bytes

        // Converts the bytes to printable string
        String encodedKey = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        return KEY_PREFIX + encodedKey;
    }
}
