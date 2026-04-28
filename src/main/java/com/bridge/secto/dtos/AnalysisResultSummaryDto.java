package com.bridge.secto.dtos;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AnalysisResultSummaryDto {
    private UUID id;
    private Instant createdAt;
    private UUID clientId;
    private String clientName;
    private String clientCpf;
    private String audioFilename;
    private String audioUrl;
    private Boolean approved;
    private Double creditsUsed;
    private String executedBy;
    private String scriptName;
    private String serviceTypeName;
    private String serviceSubTypeName;
}
