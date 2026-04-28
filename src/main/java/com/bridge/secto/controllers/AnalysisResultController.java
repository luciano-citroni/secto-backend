package com.bridge.secto.controllers;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bridge.secto.dtos.AdminQuestionOverrideRequestDto;
import com.bridge.secto.dtos.AnalysisResultResponseDto;
import com.bridge.secto.dtos.AnalysisResultSummaryDto;
import com.bridge.secto.dtos.ScriptItemInputDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.validation.Valid;
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

    @GetMapping("/summary")
    @Operation(summary = "List analysis results summary (without transcription, script and AI output)")
    public ResponseEntity<List<AnalysisResultSummaryDto>> getSummary(
            @RequestParam(required = false) UUID clientId) {
        UUID companyId = authService.getCurrentUser()
            .map(AuthService.UserInfo::getCompanyId)
            .orElseThrow(() -> new UnauthorizedActionException("User not associated with any company"));

        List<AnalysisResultSummaryDto> summaries = clientId != null
                ? repository.findSummaryByCompanyIdAndClientId(companyId, clientId)
                : repository.findSummaryByCompanyId(companyId);

        return ResponseEntity.ok(summaries);
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

        String transcription;
        String audioFilename = original.getAudioFilename();
        if (audioFilename != null && !audioFilename.isBlank()) {
            // Re-transcribe from the original audio stored in S3
            java.nio.file.Path tempAudioFile = s3StorageService.downloadToTempFile(audioFilename);
            try {
                transcription = openAIService.transcribeAudioFromPath(tempAudioFile);
            } finally {
                try { java.nio.file.Files.deleteIfExists(tempAudioFile); } catch (Exception ignored) {}
            }
        } else {
            transcription = original.getTranscription();
            if (transcription == null || transcription.isBlank()) {
                throw new BusinessRuleException("A análise original não possui áudio nem transcrição para re-gerar.");
            }
        }

        UUID clientId = original.getClient() != null ? original.getClient().getId() : null;
        UUID scriptId = original.getScript() != null ? original.getScript().getId() : null;
        String executedBy = authService.getCurrentUser().map(AuthService.UserInfo::getName).orElse(null);

        // Re-run the AI analysis with the new transcription
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
            if (processingResult.analysisResultId() == null) {
                throw new BusinessRuleException("Não foi possível registrar a análise re-gerada para vincular a cobrança de créditos.");
            }

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

    @PatchMapping("/{id}/questions/override")
    @Operation(summary = "Admin override of a question's correct/questionAsked result")
    public ResponseEntity<AnalysisResultResponseDto> overrideQuestion(
            @PathVariable UUID id,
            @Valid @RequestBody AdminQuestionOverrideRequestDto request) {

        if (!authService.isCompanyAdmin()) {
            throw new UnauthorizedActionException("Apenas company-admin pode corrigir questões da análise.");
        }

        UUID companyId = authService.getCurrentUser()
            .map(AuthService.UserInfo::getCompanyId)
            .orElseThrow(() -> new UnauthorizedActionException("User not associated with any company"));

        AnalysisResult result = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis result not found"));

        if (!result.getCompany().getId().equals(companyId)) {
            throw new ResourceNotFoundException("Analysis result not found");
        }

        if (result.getAiOutputJson() == null || result.getAiOutputJson().isBlank()) {
            throw new BusinessRuleException("Esta análise não possui resultado de IA para corrigir.");
        }

        try {
            JsonNode aiOutputNode = objectMapper.readTree(result.getAiOutputJson());
            JsonNode outputArray = aiOutputNode.get("output");

            if (outputArray == null || !outputArray.isArray()) {
                throw new BusinessRuleException("Formato do resultado de IA inválido.");
            }

            int idx = request.getQuestionIndex();
            if (idx < 0 || idx >= outputArray.size()) {
                throw new BusinessRuleException(
                        "Índice de questão inválido. Deve ser entre 0 e " + (outputArray.size() - 1) + ".");
            }

            ObjectNode questionNode = (ObjectNode) outputArray.get(idx);

            String adminName = authService.getCurrentUser()
                    .map(AuthService.UserInfo::getName)
                    .orElse("Admin");

            if (request.getCorrect() == null && request.getQuestionAsked() == null) {
                // Remove override
                questionNode.remove("adminOverride");
            } else {
                ObjectNode overrideNode;
                if (questionNode.has("adminOverride") && questionNode.get("adminOverride").isObject()) {
                    overrideNode = (ObjectNode) questionNode.get("adminOverride");
                } else {
                    overrideNode = objectMapper.createObjectNode();
                }

                if (request.getCorrect() != null) {
                    overrideNode.put("correct", request.getCorrect());
                }
                if (request.getQuestionAsked() != null) {
                    overrideNode.put("questionAsked", request.getQuestionAsked());
                }
                overrideNode.put("overriddenBy", adminName);
                overrideNode.put("overriddenAt", Instant.now().toString());

                questionNode.set("adminOverride", overrideNode);
            }

            // Recalculate approved: all questions must be correct (using override if present)
            boolean allCorrect = true;
            for (JsonNode q : outputArray) {
                boolean effectiveCorrect;
                if (q.has("adminOverride") && q.get("adminOverride").has("correct")) {
                    effectiveCorrect = q.get("adminOverride").get("correct").asBoolean();
                } else {
                    effectiveCorrect = q.has("correct") && q.get("correct").asBoolean();
                }
                if (!effectiveCorrect) {
                    allCorrect = false;
                    break;
                }
            }

            result.setAiOutputJson(objectMapper.writeValueAsString(aiOutputNode));
            result.setApproved(allCorrect);
            repository.save(result);

            return ResponseEntity.ok(toDto(result));

        } catch (BusinessRuleException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro ao aplicar override na análise {}: {}", id, e.getMessage());
            throw new BusinessRuleException("Erro ao aplicar correção na questão.");
        }
    }

    @GetMapping("/{id}/download-url")
    @Operation(summary = "Generate a temporary presigned URL for downloading the analysis audio")
    public ResponseEntity<Map<String, String>> getAudioDownloadUrl(@PathVariable UUID id) {
        if (!authService.isCompanyAdmin()) {
            throw new UnauthorizedActionException("Apenas company-admin pode baixar o áudio da análise.");
        }

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
