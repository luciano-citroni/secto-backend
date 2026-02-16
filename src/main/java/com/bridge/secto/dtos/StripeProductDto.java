package com.bridge.secto.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Produto do Stripe com informações de preço e créditos")
public class StripeProductDto {
    @Schema(description = "ID do produto no Stripe")
    private String productId;
    @Schema(description = "ID do preço no Stripe")
    private String priceId;
    @Schema(description = "Nome do produto")
    private String name;
    @Schema(description = "Descrição do produto")
    private String description;
    @Schema(description = "Valor em centavos", example = "9900")
    private Long unitAmount;
    @Schema(description = "Moeda (ex: brl)", example = "brl")
    private String currency;
    @Schema(description = "Tipo do preço: 'recurring' (recorrente) ou 'one_time' (avulso)", example = "one_time")
    private String type;
    @Schema(description = "Intervalo de cobrança para planos recorrentes (month, year, week, day)", example = "month")
    private String interval;
    @Schema(description = "Quantidade de créditos incluídos no produto", example = "100")
    private Integer credits;
}
