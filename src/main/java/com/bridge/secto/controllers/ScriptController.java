package com.bridge.secto.controllers;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bridge.secto.dtos.ScriptItemResponseDto;
import com.bridge.secto.dtos.ScriptResponseDto;
import com.bridge.secto.entities.Script;
import com.bridge.secto.entities.ScriptItem;
import com.bridge.secto.entities.ServiceType;
import com.bridge.secto.repositories.ScriptRepository;
import com.bridge.secto.repositories.ServiceTypeRepository;
import com.bridge.secto.services.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/scripts")
@RequiredArgsConstructor
public class ScriptController {

    private final ScriptRepository scriptRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final AuthService authService;

    private ScriptItemResponseDto mapToScriptItemDto(ScriptItem item) {
        ScriptItemResponseDto dto = new ScriptItemResponseDto();
        dto.setId(item.getId());
        dto.setQuestion(item.getQuestion());
        dto.setAnswer(item.getAnswer());
        return dto;
    }

    private ScriptResponseDto mapToScriptDto(Script script) {
        ScriptResponseDto dto = new ScriptResponseDto();
        dto.setId(script.getId());
        dto.setName(script.getName());
        dto.setStatus(script.getStatus());
        if (script.getScriptItems() != null) {
            dto.setScriptItems(script.getScriptItems().stream()
                .map(this::mapToScriptItemDto)
                .collect(Collectors.toList()));
        }
        return dto;
    }

    @Operation(summary = "Get Scripts by Service Type")
    @ApiResponse(responseCode = "200", description = "Successful operation")
    @GetMapping("/byServiceType/{serviceTypeId}")
    public ResponseEntity<List<ScriptResponseDto>> getScriptsByServiceType(@PathVariable UUID serviceTypeId) {
        UUID userCompanyId = authService.getCurrentUser()
            .map(AuthService.UserInfo::getCompanyId)
            .orElseThrow(() -> new RuntimeException("User not associated with any company"));

        ServiceType serviceType = serviceTypeRepository.findById(serviceTypeId)
            .orElseThrow(() -> new RuntimeException("ServiceType not found"));

        if (!serviceType.getCompany().getId().equals(userCompanyId)) {
            throw new RuntimeException("Unauthorized: ServiceType does not belong to your company");
        }

        List<ScriptResponseDto> dtos = scriptRepository.findByServiceTypeId(serviceTypeId).stream()
            .map(this::mapToScriptDto)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    
    @Operation(summary = "Get Script by ID")
    @ApiResponse(responseCode = "200", description = "Successful operation")
    @GetMapping("/{id}")
    public ResponseEntity<ScriptResponseDto> getScriptById(@PathVariable UUID id) {
        UUID userCompanyId = authService.getCurrentUser()
            .map(AuthService.UserInfo::getCompanyId)
            .orElseThrow(() -> new RuntimeException("User not associated with any company"));

        Script script = scriptRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Script not found"));

        if (!script.getCompany().getId().equals(userCompanyId)) {
            throw new RuntimeException("Unauthorized: Script does not belong to your company");
        }

        return ResponseEntity.ok(mapToScriptDto(script));
    }

    @Operation(summary = "Update Script")
    @ApiResponse(responseCode = "200", description = "Successful operation")
    @PutMapping("/{id}")
    public ResponseEntity<ScriptResponseDto> updateScript(@PathVariable UUID id, @RequestBody Script request) {
        UUID userCompanyId = authService.getCurrentUser()
            .map(AuthService.UserInfo::getCompanyId)
            .orElseThrow(() -> new RuntimeException("User not associated with any company"));

        Script script = scriptRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Script not found"));

        if (!script.getCompany().getId().equals(userCompanyId)) {
            throw new RuntimeException("Unauthorized: Script does not belong to your company");
        }

        script.setName(request.getName());
        script.setStatus(request.getStatus());

        // Handle Script Items Update (Full Replace Strategy for simplicity, or Merge)
        // Ideally: clear existing items and add new ones if provided, or update in place.
        // For this implementation, we will replace the list if provided.
        if (request.getScriptItems() != null) {
            script.getScriptItems().clear(); // This might need orphanRemoval=true in Entity
             List<ScriptItem> items = request.getScriptItems().stream()
                .map(item -> {
                    item.setScript(script);
                    return item;
                })
                .collect(Collectors.toList());
            script.getScriptItems().addAll(items);
        }

        scriptRepository.save(script);
        return ResponseEntity.ok(mapToScriptDto(script));
    }

    @Operation(summary = "Create Script")
    @ApiResponse(responseCode = "200", description = "Successful operation")
    @PostMapping("/byServiceType/{serviceTypeId}")
    public ResponseEntity<ScriptResponseDto> createScript(@PathVariable UUID serviceTypeId, @RequestBody Script request) {
        UUID userCompanyId = authService.getCurrentUser()
            .map(AuthService.UserInfo::getCompanyId)
            .orElseThrow(() -> new RuntimeException("User not associated with any company"));

        ServiceType serviceType = serviceTypeRepository.findById(serviceTypeId)
            .orElseThrow(() -> new RuntimeException("ServiceType not found with id: " + serviceTypeId));
        
        if (!serviceType.getCompany().getId().equals(userCompanyId)) {
            throw new RuntimeException("Unauthorized: ServiceType does not belong to your company");
        }

        Script script = new Script();
        script.setName(request.getName());
        script.setStatus(request.getStatus());
        script.setServiceType(serviceType);
        script.setCompany(serviceType.getCompany()); // Inherit company from ServiceType
        
        if (request.getScriptItems() != null) {
            List<ScriptItem> items = request.getScriptItems().stream()
                .map(item -> {
                    item.setScript(script);
                    return item;
                })
                .collect(Collectors.toList());
            script.setScriptItems(items);
        }

        scriptRepository.save(script);
        
        return ResponseEntity.ok(mapToScriptDto(script));
    }
}
