package io.github.rochona.notificationsystem.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.rochona.notificationsystem.config.RateLimitProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class InMemoryBucketProvider implements RateLimitBucketProvider {

    private final RateLimitProperties rateLimitProperties;

    private final Map<Long, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public Bucket resolveBucket(Long tenantId) {
        return buckets.computeIfAbsent(
                tenantId,
                key -> createBucket()
        );
    }

    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(rateLimitProperties.getCapacity())
                .refillGreedy(
                        rateLimitProperties.getRefillTokens(),
                        rateLimitProperties.getRefillDuration()
                )
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}