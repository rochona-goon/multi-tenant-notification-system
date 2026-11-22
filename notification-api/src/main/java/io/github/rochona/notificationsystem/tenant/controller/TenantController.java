package io.github.rochona.notificationsystem.tenant.controller;

import io.github.rochona.notificationsystem.tenant.dto.TenantRegistrationRequest;
import io.github.rochona.notificationsystem.tenant.dto.TenantRegistrationResponse;
import io.github.rochona.notificationsystem.tenant.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenants")
public class TenantController {
    private final TenantService tenantService;

    public TenantController(TenantService tenantService){
        this.tenantService = tenantService;
    }

    @PostMapping("/signUp")
    public ResponseEntity<TenantRegistrationResponse> tenantRegistration(@Valid @RequestBody TenantRegistrationRequest request){
        TenantRegistrationResponse response = tenantService.registerTenant(request.getTenantName());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
