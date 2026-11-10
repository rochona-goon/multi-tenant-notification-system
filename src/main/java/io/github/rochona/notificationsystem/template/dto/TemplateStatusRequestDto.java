package io.github.rochona.notificationsystem.template.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TemplateStatusRequestDto {
    @NotNull
    private Boolean active;
}
