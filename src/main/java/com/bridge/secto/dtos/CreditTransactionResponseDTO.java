package com.bridge.secto.dtos;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Data;

@Data
public class CreditTransactionResponseDTO {
    private UUID id;
    private BigDecimal amount;
}
