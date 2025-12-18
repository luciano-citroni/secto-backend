package com.bridge.secto.dtos;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Data;

@Data
public class CompanyCreditResponseDto {
    private UUID id;
    private BigDecimal creditAmount;
}
