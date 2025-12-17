package com.bridge.secto.services;

import java.util.UUID;

import lombok.Data;

@Data
public class ServiceTypeResponseDto {
    private UUID id;
    private String name;
    private String description;
}
