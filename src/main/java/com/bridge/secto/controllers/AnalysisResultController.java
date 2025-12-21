package com.bridge.secto.controllers;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bridge.secto.dtos.AnalysisResultResponseDto;
import com.bridge.secto.entities.AnalysisResult;
import com.bridge.secto.repositories.AnalysisResultRepository;
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
    private final ObjectMapper objectMapper;

    @GetMapping
    @Operation(summary = "List all analysis results")
    public ResponseEntity<List<AnalysisResultResponseDto>> getAll() {
        List<AnalysisResultResponseDto> list = repository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get analysis result by ID")
    public ResponseEntity<AnalysisResultResponseDto> getById(@PathVariable UUID id) {
        return repository.findById(id)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private AnalysisResultResponseDto toDto(AnalysisResult entity) {
        AnalysisResultResponseDto dto = new AnalysisResultResponseDto();
        dto.setId(entity.getId());
        dto.setClientName(entity.getClientName());
        dto.setAudioFilename(entity.getAudioFilename());
        dto.setTranscription(entity.getTranscription());
        dto.setApproved(entity.getApproved());
        dto.setCreatedAt(entity.getCreatedAt());

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
