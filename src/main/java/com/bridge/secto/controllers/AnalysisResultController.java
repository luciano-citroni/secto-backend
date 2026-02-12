package com.bridge.secto.controllers;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bridge.secto.dtos.AnalysisResultResponseDto;
import com.bridge.secto.entities.AnalysisResult;
import com.bridge.secto.exceptions.ResourceNotFoundException;
import com.bridge.secto.exceptions.UnauthorizedActionException;
import com.bridge.secto.repositories.AnalysisResultRepository;
import com.bridge.secto.services.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/analysis-results")
@RequiredArgsConstructor
@Tag(name = "Analysis Results", description = "Endpoints for retrieving analysis history")
public class AnalysisResultController {

    private final AnalysisResultRepository repository;
    private final AuthService authService;
    private final ObjectMapper objectMapper;

    @GetMapping
    @Operation(summary = "List analysis results by company, optionally filtered by client")
    public ResponseEntity<List<AnalysisResultResponseDto>> getAll(
            @RequestParam(required = false) UUID clientId) {
        UUID companyId = authService.getCurrentUser()
            .map(AuthService.UserInfo::getCompanyId)
            .orElseThrow(() -> new UnauthorizedActionException("User not associated with any company"));

        List<AnalysisResult> results = clientId != null
                ? repository.findByCompanyIdAndClientId(companyId, clientId)
                : repository.findByCompanyId(companyId);

        List<AnalysisResultResponseDto> list = results.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get analysis result by ID")
    public ResponseEntity<AnalysisResultResponseDto> getById(@PathVariable UUID id) {
        UUID companyId = authService.getCurrentUser()
            .map(AuthService.UserInfo::getCompanyId)
            .orElseThrow(() -> new UnauthorizedActionException("User not associated with any company"));

        AnalysisResult result = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis result not found"));
        
        if (!result.getCompany().getId().equals(companyId)) {
             throw new ResourceNotFoundException("Analysis result not found"); // Hide existence
        }
        
        return ResponseEntity.ok(toDto(result));
    }

    private AnalysisResultResponseDto toDto(AnalysisResult entity) {
        AnalysisResultResponseDto dto = new AnalysisResultResponseDto();
        dto.setId(entity.getId());
        dto.setAudioFilename(entity.getAudioFilename());
        dto.setAudioUrl(entity.getAudioUrl());
        dto.setTranscription(entity.getTranscription());
        dto.setApproved(entity.getApproved());
        dto.setCreditsUsed(entity.getCreditsUsed());
        dto.setExecutedBy(entity.getExecutedBy());
        dto.setCreatedAt(entity.getCreatedAt());

        // Informações do cliente se estiver associado
        if (entity.getClient() != null) {
            dto.setClientId(entity.getClient().getId());
            dto.setClientName(entity.getClient().getName());
            dto.setClientSurname(entity.getClient().getSurname());
            dto.setClientCpf(entity.getClient().getCpf());
        }

        if (entity.getScript() != null) {
            dto.setScriptName(entity.getScript().getName());
            if (entity.getScript().getServiceType() != null) {
                dto.setServiceTypeId(entity.getScript().getServiceType().getId());
                dto.setServiceTypeName(entity.getScript().getServiceType().getName());
                if (entity.getScript().getServiceType().getServiceSubType() != null) {
                    dto.setServiceSubTypeId(entity.getScript().getServiceType().getServiceSubType().getId());
                    dto.setServiceSubTypeName(entity.getScript().getServiceType().getServiceSubType().getName());
                }
            }
        }

        try {
            if (entity.getScriptJson() != null) {
                dto.setScript(objectMapper.readTree(entity.getScriptJson()));
            }
            if (entity.getAiOutputJson() != null) {
                dto.setAiOutput(objectMapper.readTree(entity.getAiOutputJson()));
            }
        } catch (Exception e) {

        }

        return dto;
    }
}
