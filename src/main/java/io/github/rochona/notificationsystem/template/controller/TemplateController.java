package io.github.rochona.notificationsystem.template.controller;

import io.github.rochona.notificationsystem.notification.enums.NotificationChannel;
import io.github.rochona.notificationsystem.notification.enums.NotificationStatus;
import io.github.rochona.notificationsystem.template.dto.TemplateRequestDto;
import io.github.rochona.notificationsystem.template.dto.TemplateResponseDto;
import io.github.rochona.notificationsystem.template.service.TemplateService;
import io.github.rochona.notificationsystem.template.dto.TemplateStatusRequestDto;
import io.github.rochona.notificationsystem.tenant.model.Tenant;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    // create template
    @PostMapping("/create")
    public ResponseEntity<TemplateResponseDto> createTemplate(@Valid @RequestBody TemplateRequestDto request, @RequestAttribute("authenticatedTenant") Tenant tenant){
        TemplateResponseDto response = templateService.createTemplate(request, tenant);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    //  get templates
    @GetMapping
    public ResponseEntity<Page<TemplateResponseDto>> getAllTemplates(
                     @RequestAttribute("authenticatedTenant") Tenant tenant,
                     @RequestParam(defaultValue = "0") int page,
                     @RequestParam(defaultValue = "20") int size,
                     @RequestParam(required = false) String name,
                     @RequestParam(required = false) Boolean active
     ){

        if(page < 0){
            throw new IllegalArgumentException("Page number cannot be negative");
        }

        if(size < 1 || size > 100){
            throw new IllegalArgumentException("Page size must be between 1 to 100");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());



        return ResponseEntity.ok(templateService.getTemplates(tenant, name, active, pageable));
    }

    // get template by id and tenant
    @GetMapping("/{id}")
    public ResponseEntity<TemplateResponseDto> getTemplateById(@PathVariable Long id, @RequestAttribute("authenticatedTenant") Tenant tenant){
        TemplateResponseDto response = templateService.getTemplatesById(id, tenant);
        return ResponseEntity.ok(response);
    }

    // Update template by id
    @PutMapping("/{id}")
    public ResponseEntity<TemplateResponseDto> updateTemplateById(@PathVariable Long id, @Valid @RequestBody TemplateRequestDto request, @RequestAttribute("authenticatedTenant")Tenant tenant){
        TemplateResponseDto response = templateService.updateTemplateById(id, request, tenant);
        return ResponseEntity.ok(response);
    }

    // update Template status
    @PatchMapping("/{id}/status")
    public ResponseEntity<TemplateResponseDto> updateTemplateStatus(@PathVariable Long id, @Valid @RequestBody TemplateStatusRequestDto request, @RequestAttribute("authenticatedTenant") Tenant tenant){
        TemplateResponseDto response = templateService.updateTemplateStatus(id, request.getActive(), tenant);
        return ResponseEntity.ok(response);
    }


}
