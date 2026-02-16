package com.bridge.secto.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Dados para iniciar uma sessão de checkout no Stripe")
public class CheckoutRequestDto {
    @Schema(description = "ID do preço do produto no Stripe (ex: price_xxx)", example = "price_1ABC123")
    private String priceId;
}
