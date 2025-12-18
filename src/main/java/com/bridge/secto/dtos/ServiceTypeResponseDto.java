package com.bridge.secto.dtos;

import java.util.UUID;

import lombok.Data;

@Data
public class ServiceTypeResponseDto {
    private UUID id;
    private String name;
    private String description;
}
