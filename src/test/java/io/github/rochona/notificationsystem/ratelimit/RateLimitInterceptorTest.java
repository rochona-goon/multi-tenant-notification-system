package io.github.rochona.notificationsystem.ratelimit;

import io.github.rochona.notificationsystem.tenant.model.Tenant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RateLimitInterceptorTest {

    private RateLimiterService rateLimiterService;
    private RateLimitInterceptor rateLimitInterceptor;

    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        rateLimiterService = mock(RateLimiterService.class);
        rateLimitInterceptor = new RateLimitInterceptor(rateLimiterService);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
    }

    @Test
    void shouldAllowRequestWhenTenantIsWithinRateLimit() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setId(1L);

        when(request.getAttribute("authenticatedTenant"))
                .thenReturn(tenant);

        when(rateLimiterService.isAllowed(1L))
                .thenReturn(true);

        boolean result = rateLimitInterceptor.preHandle(
                request, response, new Object()
        );

        assertThat(result).isTrue();

        verify(rateLimiterService).isAllowed(1L);
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    void shouldRejectRequestWhenTenantExceedsRateLimit() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setId(1L);

        when(request.getAttribute("authenticatedTenant"))
                .thenReturn(tenant);

        when(rateLimiterService.isAllowed(1L))
                .thenReturn(false);

        boolean result = rateLimitInterceptor.preHandle(
                request, response, new Object()
        );

        assertThat(result).isFalse();

        verify(rateLimiterService).isAllowed(1L);
        verify(response).sendError(
                429,
                "Rate limit exceeded. Please try again later."
        );
    }

    @Test
    void shouldRejectRequestWhenTenantIsNotAuthenticated() throws Exception {
        when(request.getAttribute("authenticatedTenant"))
                .thenReturn(null);

        boolean result = rateLimitInterceptor.preHandle(
                request, response, new Object()
        );

        assertThat(result).isFalse();

        verifyNoInteractions(rateLimiterService);

        verify(response).sendError(
                401,
                "Tenant authentication required"
        );
    }
}
