package com.bridge.secto.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Item do script contendo pergunta e resposta esperada")
public class ScriptItemInputDto {
    
    @Schema(description = "A pergunta feita no script", example = "Qual é o seu nome?")
    private String question;

    @Schema(description = "A resposta esperada para a pergunta", example = "João da Silva")
    private String answer;
}
