package com.bridge.secto.controllers;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bridge.secto.dtos.AnalysisResultResponseDto;
import com.bridge.secto.dtos.ScriptItemInputDto;
import com.bridge.secto.entities.AnalysisResult;
import com.bridge.secto.exceptions.BusinessRuleException;
import com.bridge.secto.exceptions.ResourceNotFoundException;
import com.bridge.secto.exceptions.UnauthorizedActionException;
import com.bridge.secto.repositories.AnalysisResultRepository;
import com.bridge.secto.services.AuthService;
import com.bridge.secto.services.CreditService;
import com.bridge.secto.services.OpenAIService;
import com.bridge.secto.services.S3StorageService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/analysis-results")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Analysis Results", description = "Endpoints for retrieving analysis history")
public class AnalysisResultController {

    private final AnalysisResultRepository repository;
    private final AuthService authService;
    private final ObjectMapper objectMapper;
    private final OpenAIService openAIService;
    private final CreditService creditService;
    private final S3StorageService s3StorageService;

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

    @PostMapping("/{id}/regenerate")
    @Operation(summary = "Re-generate an analysis using the same data and charging the same credits")
    public ResponseEntity<AnalysisResultResponseDto> regenerateAnalysis(@PathVariable UUID id) {
        UUID companyId = authService.getCurrentUser()
            .map(AuthService.UserInfo::getCompanyId)
            .orElseThrow(() -> new UnauthorizedActionException("User not associated with any company"));

        AnalysisResult original = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis result not found"));

        if (!original.getCompany().getId().equals(companyId)) {
            throw new ResourceNotFoundException("Analysis result not found");
        }

        // Parse the original script items
        List<ScriptItemInputDto> scriptItems;
        try {
            scriptItems = objectMapper.readValue(
                    original.getScriptJson(),
                    new TypeReference<List<ScriptItemInputDto>>() {}
            );
        } catch (Exception e) {
            log.error("Erro ao deserializar scriptJson da análise {}: {}", id, e.getMessage());
            throw new BusinessRuleException("Não foi possível recuperar os dados do script da análise original.");
        }

        Double creditsToCharge = original.getCreditsUsed();

        // Check credits if the original analysis had a cost
        if (creditsToCharge != null && creditsToCharge > 0) {
            if (!creditService.hasEnoughCredits(companyId, creditsToCharge)) {
                throw new BusinessRuleException(
                        "Créditos insuficientes para re-gerar esta análise. Necessário: " + creditsToCharge + " créditos");
            }
        }

        String transcription = original.getTranscription();
        if (transcription == null || transcription.isBlank()) {
            throw new BusinessRuleException("A análise original não possui transcrição para re-gerar.");
        }

        UUID clientId = original.getClient() != null ? original.getClient().getId() : null;
        UUID scriptId = original.getScript() != null ? original.getScript().getId() : null;
        String executedBy = authService.getCurrentUser().map(AuthService.UserInfo::getName).orElse(null);

        // Re-run the AI analysis with the same data
        OpenAIService.AnalysisProcessingResult processingResult = openAIService.compareTranscribedTextAndScript(
                transcription,
                scriptItems,
                clientId,
                original.getAudioFilename(),
                original.getAudioUrl(),
                scriptId,
                creditsToCharge,
                executedBy
        );

        // Debit credits after successful analysis
        if (creditsToCharge != null && creditsToCharge > 0) {
            String clientName = original.getClient() != null
                    ? original.getClient().getFullName()
                    : "Cliente ID: " + clientId;
            creditService.debitCredits(companyId, creditsToCharge,
                    String.format("Re-análise de áudio - Cliente: %s", clientName),
                    processingResult.analysisResultId());
        }

        // Find the newly created AnalysisResult (the last one created by OpenAIService)
        List<AnalysisResult> results = repository.findByCompanyId(companyId);
        AnalysisResult newResult = results.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Erro ao recuperar a análise re-gerada."));

        return ResponseEntity.ok(toDto(newResult));
    }

    @GetMapping("/{id}/download-url")
    @Operation(summary = "Generate a temporary presigned URL for downloading the analysis audio")
    public ResponseEntity<Map<String, String>> getAudioDownloadUrl(@PathVariable UUID id) {
        UUID companyId = authService.getCurrentUser()
            .map(AuthService.UserInfo::getCompanyId)
            .orElseThrow(() -> new UnauthorizedActionException("User not associated with any company"));

        AnalysisResult result = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis result not found"));

        if (!result.getCompany().getId().equals(companyId)) {
            throw new ResourceNotFoundException("Analysis result not found");
        }

        if (result.getAudioFilename() == null || result.getAudioFilename().isBlank()) {
            throw new BusinessRuleException("Esta análise não possui áudio associado.");
        }

        String presignedUrl = s3StorageService.generatePresignedUrl(
                result.getAudioFilename(),
                java.time.Duration.ofMinutes(15)
        );

        return ResponseEntity.ok(Map.of("url", presignedUrl));
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
            dto.setClientName(entity.getClient().getFullName());
            dto.setClientCpf(entity.getClient().getCpf());
        }

        if (entity.getScript() != null) {
            dto.setScriptId(entity.getScript().getId());
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
