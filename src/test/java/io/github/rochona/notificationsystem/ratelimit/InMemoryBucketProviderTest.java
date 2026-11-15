package io.github.rochona.notificationsystem.ratelimit;

import io.github.rochona.notificationsystem.config.RateLimitProperties;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryBucketProviderTest {

    private InMemoryBucketProvider bucketProvider;

    @BeforeEach
    void setUp() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setCapacity(5);
        properties.setRefillTokens(5);
        properties.setRefillDuration(Duration.ofMinutes(1));

        bucketProvider = new InMemoryBucketProvider(properties);
    }

    @Test
    void shouldCreateBucketForNewTenant() {
        Bucket bucket = bucketProvider.resolveBucket(1L);

        assertThat(bucket).isNotNull();
        assertThat(bucket.tryConsume(5)).isTrue();
        assertThat(bucket.tryConsume(1)).isFalse();
    }

    @Test
    void shouldReturnSameBucketForSameTenant() {
        Bucket firstBucket = bucketProvider.resolveBucket(1L);
        Bucket secondBucket = bucketProvider.resolveBucket(1L);

        assertThat(firstBucket).isSameAs(secondBucket);
    }

    @Test
    void shouldMaintainIndependentBucketsForDifferentTenants() {
        Bucket tenantOneBucket = bucketProvider.resolveBucket(1L);
        Bucket tenantTwoBucket = bucketProvider.resolveBucket(2L);

        tenantOneBucket.tryConsume(5);

        assertThat(tenantOneBucket.tryConsume(1)).isFalse();
        assertThat(tenantTwoBucket.tryConsume(1)).isTrue();
    }
}