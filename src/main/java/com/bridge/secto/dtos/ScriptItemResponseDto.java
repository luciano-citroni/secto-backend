package com.bridge.secto.dtos;

import java.util.UUID;
import lombok.Data;

@Data
public class ScriptItemResponseDto {
    private UUID id;
    private String question;
    private String answer;
    private String linkedClientField;
}
