package com.bridge.secto.dtos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Dados de uma transação de crédito")
public class CreditTransactionResponseDTO {
    @Schema(description = "ID da transação")
    private UUID id;
    @Schema(description = "Valor da transação (positivo = compra/crédito, negativo = débito/uso)")
    private BigDecimal amount;
    @Schema(description = "ID da sessão Stripe associada (se aplicável)")
    private String stripeSessionId;
    @Schema(description = "ID do usuário que realizou a compra (Keycloak ID)")
    private String purchasedBy;
    @Schema(description = "Nome do usuário que realizou a compra")
    private String purchasedByName;
    @Schema(description = "Data/hora da transação")
    private Instant createdAt;
}
