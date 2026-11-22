package io.github.rochona.notificationsystem.ratelimit;

import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

@Component
public interface RateLimitBucketProvider {
    Bucket resolveBucket(Long tenantId);
}
