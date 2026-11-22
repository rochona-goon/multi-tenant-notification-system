package io.github.rochona.notificationsystem.tenant.security;

import io.github.rochona.notificationsystem.tenant.model.Tenant;
import io.github.rochona.notificationsystem.tenant.service.TenantAuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TenantAuthenticationInterceptor implements HandlerInterceptor {

    private final TenantAuthenticationService tenantAuthenticationService;

    public TenantAuthenticationInterceptor(TenantAuthenticationService tenantAuthenticationService){
        this.tenantAuthenticationService = tenantAuthenticationService;
    }

    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler){

        String rawApiKey = request.getHeader("X-API-KEY");

        Tenant tenant = tenantAuthenticationService.authenticate(rawApiKey);

        request.setAttribute("authenticatedTenant", tenant);

        return true;
    }


}
