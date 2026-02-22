package com.bridge.secto.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Informações da assinatura ativa do Stripe")
public class SubscriptionInfoDto {
    @Schema(description = "ID da subscription no Stripe")
    private String subscriptionId;
    @Schema(description = "Status da assinatura (active, canceled, past_due, etc)")
    private String status;
    @Schema(description = "Nome do plano")
    private String planName;
    @Schema(description = "Créditos por ciclo de cobrança")
    private Integer credits;
    @Schema(description = "Intervalo de cobrança (month, year, week, day)")
    private String interval;
    @Schema(description = "Data de fim do período atual (timestamp Unix)")
    private Long currentPeriodEnd;
    @Schema(description = "Se a assinatura será cancelada ao fim do período atual")
    private Boolean cancelAtPeriodEnd;
    @Schema(description = "Preço em centavos")
    private Long unitAmount;
    @Schema(description = "Moeda")
    private String currency;
}
