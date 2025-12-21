package com.bridge.secto.dtos;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Dados da requisição de análise")
public class AnalysisRequestDto {
    
    @Schema(description = "Nome do cliente analisado", example = "João da Silva")
    private String clientName;

    @Schema(description = "Texto da transcrição (opcional se enviar arquivo de áudio)", example = "Meu nome é João...")
    private String transcription;

    @Schema(description = "Lista de itens do script (perguntas e respostas esperadas)")
    private List<ScriptItemInputDto> scriptItems;
}
