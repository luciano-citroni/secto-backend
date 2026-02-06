package com.bridge.secto.dtos;

import lombok.Data;

@Data
public class CreditEstimationResponseDto {
    private Double durationInSeconds;
    private Double estimatedCredits;
}