package io.github.rochona.notificationsystem.template.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class TemplateResponseDto {

    private Long id;

    private String name;

    private String subject;

    private String bodyText;

    private String bodyHtml;

    private boolean isActive;

    private Instant createdAt;

    private Instant updatedAt;
}
