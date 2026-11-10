package io.github.rochona.notificationsystem.template.service;

import io.github.rochona.notificationsystem.template.dto.TemplateRequestDto;
import io.github.rochona.notificationsystem.template.dto.TemplateResponseDto;
import io.github.rochona.notificationsystem.tenant.model.Tenant;

import java.util.List;

public interface TemplateService {

    TemplateResponseDto createTemplate(TemplateRequestDto requestDto, Tenant tenant);

    List<TemplateResponseDto> getTemplates(Tenant tenant, boolean isActive);

    TemplateResponseDto getTemplatesById(Long id, Tenant tenant);

    TemplateResponseDto updateTemplateById(Long id, TemplateRequestDto requestDto, Tenant tenant);

    TemplateResponseDto updateTemplateStatus(Long id, boolean status, Tenant  tenant);

}
