package com.bridge.secto.controllers;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bridge.secto.dtos.ScriptResponseDto;
import com.bridge.secto.entities.Script;
import com.bridge.secto.entities.ServiceSubType;
import com.bridge.secto.repositories.ScriptRepository;
import com.bridge.secto.repositories.ServiceSubTypeRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/scripts")
@RequiredArgsConstructor
public class ScriptController {

    private final ScriptRepository scriptRepository;
    private final ServiceSubTypeRepository serviceSubTypeRepository;

    @Operation(summary = "Get Scripts by Service Sub Type")
    @ApiResponse(responseCode = "200", description = "Successful operation")
    @GetMapping("/byServiceSubType/{serviceSubTypeId}")
    public ResponseEntity<List<ScriptResponseDto>> getScriptsByServiceSubType(@PathVariable UUID serviceSubTypeId) {
        List<ScriptResponseDto> dtos = scriptRepository.findByServiceSubTypeId(serviceSubTypeId).stream()
            .map(script -> {
                ScriptResponseDto dto = new ScriptResponseDto();
                dto.setId(script.getId());
                dto.setName(script.getName());
                dto.setStatus(script.getStatus());
                return dto;
            })
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Create Script")
    @ApiResponse(responseCode = "200", description = "Successful operation")
    @PostMapping("/byServiceSubType/{serviceSubTypeId}")
    public ResponseEntity<ScriptResponseDto> createScript(@PathVariable UUID serviceSubTypeId, @RequestBody Script request) {
        ServiceSubType serviceSubType = serviceSubTypeRepository.findById(serviceSubTypeId)
            .orElseThrow(() -> new RuntimeException("ServiceSubType not found with id: " + serviceSubTypeId));
        
        Script script = new Script();
        script.setName(request.getName());
        script.setStatus(request.getStatus());
        script.setServiceSubType(serviceSubType);
        script.setCompany(serviceSubType.getCompany()); // Inherit company from ServiceSubType
        
        scriptRepository.save(script);
        
        ScriptResponseDto dto = new ScriptResponseDto();
        dto.setId(script.getId());
        dto.setName(script.getName());
        dto.setStatus(script.getStatus());
        
        return ResponseEntity.ok(dto);
    }
}
