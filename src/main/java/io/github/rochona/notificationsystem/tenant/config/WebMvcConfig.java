package io.github.rochona.notificationsystem.tenant.config;

import io.github.rochona.notificationsystem.ratelimit.RateLimitInterceptor;
import io.github.rochona.notificationsystem.tenant.security.TenantAuthenticationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final TenantAuthenticationInterceptor tenantAuthenticationInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;


    @Override
    public void addInterceptors(InterceptorRegistry registry){
        registry.addInterceptor(tenantAuthenticationInterceptor)
                .addPathPatterns(
                        "/notifications",
                        "/notifications/**",
                        "/templates",
                        "/templates/**",
                        "/preferences",
                        "/preferences/**"
                        );


        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns(
                        "/notifications",
                        "/notifications/**",
                        "/templates",
                        "/templates/**",
                        "/preferences",
                        "/preferences/**"
                );
    }
}
