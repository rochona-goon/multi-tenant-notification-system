package io.github.rochona.notificationsystem.tenant.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class TenantRegistrationResponse {
    private Long tenantId;
    private String tenantName;
    private String apiKey;
}
