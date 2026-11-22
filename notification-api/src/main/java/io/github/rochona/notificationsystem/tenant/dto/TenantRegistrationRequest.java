package io.github.rochona.notificationsystem.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TenantRegistrationRequest {

    @NotBlank(message = "Tenant name should not be blank")
    private String tenantName;
}
