package com.bridge.secto.dtos;

import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class ScriptResponseDto {
    private UUID id;
    private String name;
    private Boolean status;
    private List<ScriptItemResponseDto> scriptItems;
}
