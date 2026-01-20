package com.bridge.secto.dtos;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.Data;

@Data
public class AnalysisResultResponseDto {
    private UUID id;
    private String clientName;
    private String audioFilename;
    private String transcription;
    private JsonNode script;
    private JsonNode aiOutput;
    private Boolean approved;
    private Instant createdAt;
    private UUID serviceTypeId;
    private String serviceTypeName;
    private UUID serviceSubTypeId;
    private String serviceSubTypeName;
    private String scriptName;
}
