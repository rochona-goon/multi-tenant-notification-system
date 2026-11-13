package io.github.rochona.notificationsystem.tenant.config;

import io.github.rochona.notificationsystem.tenant.security.TenantAuthenticationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final TenantAuthenticationInterceptor tenantAuthenticationInterceptor;

    public WebMvcConfig(TenantAuthenticationInterceptor tenantAuthenticationInterceptor){
        this.tenantAuthenticationInterceptor = tenantAuthenticationInterceptor;
    }

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
    }
}
