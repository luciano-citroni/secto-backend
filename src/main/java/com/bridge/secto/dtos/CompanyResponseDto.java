package com.bridge.secto.dtos;

import java.util.UUID;

import lombok.Data;

@Data
public class CompanyResponseDto {
    private UUID id;
    private UUID ownerId;
    private String name;
}
