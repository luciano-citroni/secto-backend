package com.bridge.secto.dtos;

import java.math.BigDecimal;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Dados da conta de crédito da empresa")
public class CompanyCreditResponseDto {
    @Schema(description = "ID da conta de crédito")
    private UUID id;
    @Schema(description = "Saldo atual de créditos", example = "150.00")
    private BigDecimal creditAmount;
}
