package io.github.rochona.notificationsystem.template.service;

import io.github.rochona.notificationsystem.template.dto.TemplateRequestDto;
import io.github.rochona.notificationsystem.template.dto.TemplateResponseDto;
import io.github.rochona.notificationsystem.template.model.Template;
import io.github.rochona.notificationsystem.tenant.model.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TemplateService {

    TemplateResponseDto createTemplate(TemplateRequestDto requestDto, Tenant tenant);

    Page<TemplateResponseDto> getTemplates(Tenant tenant, String name, Boolean active, Pageable pageable);

    TemplateResponseDto getTemplatesById(Long id, Tenant tenant);
    Template getTemplateEntityById(Long id, Tenant tenant);

    TemplateResponseDto updateTemplateById(Long id, TemplateRequestDto requestDto, Tenant tenant);

    TemplateResponseDto updateTemplateStatus(Long id, boolean status, Tenant  tenant);

}
