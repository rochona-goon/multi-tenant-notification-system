package io.github.rochona.notificationsystem.ratelimit;

import io.github.rochona.notificationsystem.tenant.model.Tenant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiterService;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws IOException {

        Tenant tenant = (Tenant) request.getAttribute("authenticatedTenant");

        if (tenant == null) {
            response.sendError(
                    HttpStatus.UNAUTHORIZED.value(),
                    "Tenant authentication required"
            );
            return false;
        }

        boolean allowed = rateLimiterService.isAllowed(tenant.getId());

        if (!allowed) {
            response.sendError(
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                    "Rate limit exceeded. Please try again later."
            );
            return false;
        }

        return true;
    }
}
