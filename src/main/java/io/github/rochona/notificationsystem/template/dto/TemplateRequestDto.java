package io.github.rochona.notificationsystem.template.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TemplateRequestDto {

    @NotBlank @Size(max = 150)
    private String name;

    @NotBlank @Size(max = 255)
    private String subject;

    @NotBlank
    private String bodyText;

    @NotBlank
    private String bodyHtml;
}
