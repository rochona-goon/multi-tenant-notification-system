package io.github.rochona.notificationsystem.ratelimit;


import io.github.rochona.notificationsystem.config.RateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final RateLimitBucketProvider rateLimitBucketProvider;

    public boolean isAllowed(Long tenantId) {
        Bucket bucket = rateLimitBucketProvider.resolveBucket(tenantId);
        return bucket.tryConsume(1);
    }
}
