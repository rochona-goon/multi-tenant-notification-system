package io.github.rochona.notificationsystem.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RateLimiterServiceTest {

    private RateLimitBucketProvider bucketProvider;
    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        bucketProvider = mock(RateLimitBucketProvider.class);
        rateLimiterService = new RateLimiterService(bucketProvider);
    }

    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(5)
                .refillGreedy(5, java.time.Duration.ofMinutes(1))
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    @Test
    void shouldAllowRequestWhenTokensAreAvailable() {
        Bucket bucket = createBucket();

        when(bucketProvider.resolveBucket(1L)).thenReturn(bucket);

        assertThat(rateLimiterService.isAllowed(1L)).isTrue();
        verify(bucketProvider).resolveBucket(1L);
    }

    @Test
    void shouldRejectRequestWhenNoTokensAreAvailable() {
        Bucket bucket = createBucket();

        when(bucketProvider.resolveBucket(1L)).thenReturn(bucket);

        // Consume all five available tokens.
        for (int i = 0; i < 5; i++) {
            bucket.tryConsume(1);
        }

        assertThat(rateLimiterService.isAllowed(1L)).isFalse();
    }

    @Test
    void shouldResolveBucketForEachTenant() {
        Bucket tenantOneBucket = createBucket();
        Bucket tenantTwoBucket = createBucket();

        when(bucketProvider.resolveBucket(1L)).thenReturn(tenantOneBucket);
        when(bucketProvider.resolveBucket(2L)).thenReturn(tenantTwoBucket);

        assertThat(rateLimiterService.isAllowed(1L)).isTrue();
        assertThat(rateLimiterService.isAllowed(2L)).isTrue();

        verify(bucketProvider).resolveBucket(1L);
        verify(bucketProvider).resolveBucket(2L);
    }

    @Test
    void shouldRejectExhaustedTenantWithoutAffectingOtherTenants() {
        Bucket tenantOneBucket = createBucket();
        Bucket tenantTwoBucket = createBucket();

        when(bucketProvider.resolveBucket(1L)).thenReturn(tenantOneBucket);
        when(bucketProvider.resolveBucket(2L)).thenReturn(tenantTwoBucket);

        // Exhaust tenant 1's bucket.
        for (int i = 0; i < 5; i++) {
            tenantOneBucket.tryConsume(1);
        }

        // Tenant 1 should be rejected.
        assertThat(rateLimiterService.isAllowed(1L)).isFalse();

        // Tenant 2 should still be allowed.
        assertThat(rateLimiterService.isAllowed(2L)).isTrue();

        verify(bucketProvider).resolveBucket(1L);
        verify(bucketProvider).resolveBucket(2L);
    }
}