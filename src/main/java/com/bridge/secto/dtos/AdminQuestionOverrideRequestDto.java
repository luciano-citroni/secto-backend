package com.bridge.secto.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Requisição de override administrativo em uma questão da análise")
public class AdminQuestionOverrideRequestDto {

    @NotNull(message = "O índice da questão é obrigatório")
    @Min(value = 0, message = "O índice da questão deve ser >= 0")
    @Schema(description = "Índice da questão no array de output (0-based)")
    private Integer questionIndex;

    @Schema(description = "Override do campo 'correct' (true/false). Null para não alterar.")
    private Boolean correct;

    @Schema(description = "Override do campo 'questionAsked' (true/false). Null para não alterar.")
    private Boolean questionAsked;
}
