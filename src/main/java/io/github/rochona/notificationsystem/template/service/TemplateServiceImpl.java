package io.github.rochona.notificationsystem.template.service;

import io.github.rochona.notificationsystem.notification.model.Notification;
import io.github.rochona.notificationsystem.notification.specification.NotificationSpecification;
import io.github.rochona.notificationsystem.template.dto.TemplateRequestDto;
import io.github.rochona.notificationsystem.template.dto.TemplateResponseDto;
import io.github.rochona.notificationsystem.template.exception.TemplateAlreadyActiveException;
import io.github.rochona.notificationsystem.template.exception.TemplateAlreadyInactiveException;
import io.github.rochona.notificationsystem.template.exception.TemplateNameAlreadyExistsException;
import io.github.rochona.notificationsystem.template.exception.TemplateNotFoundException;
import io.github.rochona.notificationsystem.template.model.Template;
import io.github.rochona.notificationsystem.template.repository.TemplateRepository;
import io.github.rochona.notificationsystem.template.specification.TemplateSpecification;
import io.github.rochona.notificationsystem.tenant.model.Tenant;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TemplateServiceImpl implements TemplateService {

    private final TemplateRepository templateRepository;
    @Override
    public TemplateResponseDto createTemplate(TemplateRequestDto requestDto, Tenant tenant) {

        // Check if to be created Template already exists
        boolean isTemplatePresent = templateRepository.existsByTenantIdAndName(tenant.getId(), requestDto.getName());

        if(isTemplatePresent){
            throw new TemplateNameAlreadyExistsException("Template already exists with this name");
        }

        Template template = new Template();
        template.setName(requestDto.getName());
        template.setSubject(requestDto.getSubject());
        template.setTenant(tenant);
        template.setBodyText(requestDto.getBodyText());
        template.setBodyHtml(requestDto.getBodyHtml());

        Template savedTemplate = saveTemplate(template);

        return toResponseDto(savedTemplate);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TemplateResponseDto> getTemplates(Tenant tenant, String name, Boolean active, Pageable pageable) {

        Specification<Template> specification = TemplateSpecification.belongsToTenant(tenant.getId());

        if(name != null){
            specification = specification.and(TemplateSpecification.hasName(name));
        }

        if(active != null){
            specification = specification.and(TemplateSpecification.hasActiveStatus(active));
        }

        return templateRepository.findAll(specification, pageable).map(this::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public TemplateResponseDto getTemplatesById(Long id, Tenant tenant) {

        Template template = templateRepository.findByIdAndTenantId(id, tenant.getId()).orElseThrow(
                () -> new TemplateNotFoundException("Template not found")
        );

        return toResponseDto(template);
    }


    @Override
    @Transactional(readOnly = true)
    public Template getTemplateEntityById(Long id, Tenant tenant) {

        Template template = templateRepository
                .findByIdAndTenantId(id, tenant.getId())
                .orElseThrow(() ->
                        new TemplateNotFoundException("Template not found"));

        if (!template.isActive()) {
            throw new TemplateNotFoundException("Template is inactive");
        }

        return template;
    }



    @Override
    public TemplateResponseDto updateTemplateById(Long id, TemplateRequestDto requestDto, Tenant tenant) {

        // Check if to be updated Template already exists
        Template template = templateRepository.findByIdAndTenantId(id, tenant.getId()).orElseThrow(
                () -> new TemplateNotFoundException("Template not found")
        );

        boolean nameExists = templateRepository.existsByTenantIdAndNameAndIdNot(tenant.getId(), requestDto.getName(), id);

        if(nameExists){
            throw new TemplateNameAlreadyExistsException("Another template already exists with this name");
        }


        template.setName(requestDto.getName());
        template.setSubject(requestDto.getSubject());
        template.setBodyText(requestDto.getBodyText());
        template.setBodyHtml(requestDto.getBodyHtml());

        Template savedTemplate = saveTemplate(template);

        return toResponseDto(savedTemplate);
    }


    @Override
    public TemplateResponseDto updateTemplateStatus(Long id, boolean active, Tenant tenant) {

        Template template = templateRepository.findByIdAndTenantId(id, tenant.getId()).orElseThrow(
                () -> new TemplateNotFoundException("Template not found")
        );

        if(template.isActive() == active){
            if(active){
                throw new TemplateAlreadyActiveException("Template is already active");
            }else{
                throw new TemplateAlreadyInactiveException("Template is already inactive");

            }
        }

        template.setActive(active);

        return toResponseDto(templateRepository.save(template));
    }



    private TemplateResponseDto toResponseDto(Template template){

        TemplateResponseDto response = new TemplateResponseDto();
        response.setId(template.getId());
        response.setName(template.getName());
        response.setSubject(template.getSubject());
        response.setBodyText(template.getBodyText());
        response.setBodyHtml(template.getBodyHtml());
        response.setActive(template.isActive());
        response.setCreatedAt(template.getCreatedAt());
        response.setUpdatedAt(template.getUpdatedAt());

        return  response;
    }

    private List<TemplateResponseDto> toResponseDto(List<Template> templates){
        return templates.stream().map(this::toResponseDto).toList();
    }


    private Template saveTemplate(Template template) {

        try {
            return templateRepository.saveAndFlush(template);
        } catch (DataIntegrityViolationException exception) {

            Throwable cause = exception;

            while (cause != null) {
                if (cause instanceof ConstraintViolationException cve) {
                    if ("uk_template_tenant_name".equals(cve.getConstraintName())) {
                        throw new TemplateNameAlreadyExistsException("Template already exists with this name");
                    }
                }
                cause = cause.getCause();
            }
            throw exception;
        }
    }
}
