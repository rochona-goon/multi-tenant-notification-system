package io.github.rochona.notificationsystem.preference.controller;

import io.github.rochona.notificationsystem.preference.dto.PreferenceRequestDto;
import io.github.rochona.notificationsystem.preference.dto.PreferenceResponseDto;
import io.github.rochona.notificationsystem.preference.service.PreferenceService;
import io.github.rochona.notificationsystem.tenant.model.Tenant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/preferences")
@RequiredArgsConstructor
public class PreferenceController {

    private final PreferenceService preferenceService;


    @PutMapping("/{userId}")
    public ResponseEntity<PreferenceResponseDto> upsertPreference(@PathVariable @NotBlank @Size(max = 100) String userId, @Valid @RequestBody PreferenceRequestDto request, @RequestAttribute("authenticatedTenant") Tenant tenant){
        return ResponseEntity.ok(preferenceService.upsertPreference(tenant, userId, request));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<PreferenceResponseDto> getPreference(@PathVariable @NotBlank @Size(max = 100) String userId, @RequestAttribute("authenticatedTenant") Tenant tenant){
        return ResponseEntity.ok(preferenceService.getPreference(tenant.getId(), userId));
    }

    @GetMapping
    public ResponseEntity<List<PreferenceResponseDto>> getAllPreferences(@RequestAttribute("authenticatedTenant") Tenant tenant){
        return ResponseEntity.ok(preferenceService.getAllPreferences(tenant.getId()));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deletePreference(@PathVariable @NotBlank @Size(max = 100) String userId, @RequestAttribute("authenticatedTenant") Tenant tenant){
        preferenceService.deletePreference(tenant.getId(), userId);
        return ResponseEntity.noContent().build();
    }

}
