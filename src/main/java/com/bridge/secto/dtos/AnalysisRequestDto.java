package com.bridge.secto.dtos;

import java.util.List;

import lombok.Data;

@Data
public class AnalysisRequestDto {
    private String transcription;
    private List<ScriptItemInputDto> scriptItems;
}
