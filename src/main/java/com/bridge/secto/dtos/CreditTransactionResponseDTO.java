package com.bridge.secto.dtos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(
    description = "Representa uma transação individual de crédito da empresa. "
        + "Pode ser uma compra (crédito adicionado), consumo por análise (débito), "
        + "recorrência de assinatura ou ajuste manual administrativo."
)
public class CreditTransactionResponseDTO {

    @Schema(
        description = "Identificador único da transação (UUID gerado automaticamente).",
        example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
    )
    private UUID id;

    @Schema(
        description = "Valor da transação em créditos. Valores positivos representam créditos adicionados à conta "
            + "(compras, assinaturas, ajustes manuais). Valores negativos representam créditos consumidos "
            + "(débitos por análise de áudio). Exemplo: 50.00 = compra de 50 créditos; -2.50 = consumo de 2.5 créditos por análise.",
        example = "50.00"
    )
    private BigDecimal amount;

    @Schema(
        description = "ID da sessão de checkout do Stripe associada a esta transação. "
            + "Presente apenas em transações originadas por compra via Stripe. Nulo para débitos de uso e ajustes manuais.",
        example = "cs_test_a1b2c3d4e5f6g7h8i9j0",
        nullable = true
    )
    private String stripeSessionId;

    @Schema(
        description = "ID do usuário no Keycloak que realizou a operação (compra ou análise que gerou o débito). "
            + "Pode ser nulo em transações automáticas do sistema.",
        example = "f47ac10b-58cc-4372-a567-0e02b2c3d479",
        nullable = true
    )
    private String purchasedBy;

    @Schema(
        description = "Nome completo do usuário que realizou a operação. "
            + "Exibido para facilitar a identificação sem necessidade de consultar o Keycloak.",
        example = "João da Silva",
        nullable = true
    )
    private String purchasedByName;

    @Schema(
        description = "Data e hora em que a transação foi registrada no sistema (formato ISO 8601 em UTC).",
        example = "2026-03-05T14:30:00Z"
    )
    private Instant createdAt;

    @Schema(
        description = "Data e hora de expiração dos créditos deste lote (formato ISO 8601 em UTC). "
            + "Após esta data, os créditos restantes deste lote não podem mais ser utilizados. "
            + "Presente apenas em transações de compra (valores positivos). Nulo para débitos de uso.",
        example = "2027-03-05T14:30:00Z",
        nullable = true
    )
    private Instant expiresAt;

    @Schema(
        description = "Quantidade de créditos ainda disponíveis neste lote específico. "
            + "Aplicável apenas a transações de compra. Diminui à medida que análises consomem créditos (FIFO). "
            + "Valor zero em lotes totalmente consumidos ou em transações de débito.",
        example = "47.50",
        nullable = true
    )
    private BigDecimal remainingAmount;

    @Schema(
        description = "Tipo de origem da transação. Valores possíveis:\n"
            + "- **RECURRING**: créditos adicionados automaticamente por assinatura recorrente (Stripe).\n"
            + "- **ONE_TIME**: compra avulsa de créditos (Stripe).\n"
            + "- **MANUAL**: ajuste manual realizado por administrador.\n"
            + "- **USAGE**: débito automático por consumo em análise de áudio.",
        example = "USAGE",
        allowableValues = {"RECURRING", "ONE_TIME", "MANUAL", "USAGE"}
    )
    private String sourceType;

    @Schema(
        description = "Intervalo de recorrência da assinatura que gerou esta transação. "
            + "Presente apenas quando sourceType = RECURRING. Valores possíveis: day, week, month, year.",
        example = "month",
        nullable = true,
        allowableValues = {"day", "week", "month", "year"}
    )
    private String intervalType;

    @Schema(
        description = "ID do resultado de análise que gerou esta transação de débito. "
            + "Presente apenas em transações de uso (sourceType = USAGE).",
        example = "b2c3d4e5-f6a7-8901-bcde-f23456789012",
        nullable = true
    )
    private UUID analysisResultId;
}
