package com.bridge.secto.controllers;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.bridge.secto.dtos.AnalysisRequestDto;
import com.bridge.secto.dtos.OpenAiAnalysisResponseDTO;
import com.bridge.secto.services.OpenAIService;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/analyze")
@RequiredArgsConstructor
@Tag(name = "AI Analysis", description = "Endpoints for interacting with OpenAI")
public class OpenAIController {

    private final OpenAIService openAIService;

    @PostMapping(value = "/generate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Gerar análise sobre a transcrição do áudio", 
        description = "Recebe um arquivo de áudio (opcional) e um JSON com o script esperado. Realiza a transcrição (se áudio enviado) e compara com o script para validar as respostas.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Análise realizada com sucesso", 
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenAiAnalysisResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Requisição inválida. É necessário fornecer uma transcrição no JSON ou um arquivo de áudio.")
        }
    )
    public ResponseEntity<OpenAiAnalysisResponseDTO> generateText(
            @Parameter(description = "Objeto JSON contendo o script de perguntas/respostas e metadados do cliente", required = true, schema = @Schema(implementation = AnalysisRequestDto.class))
            @RequestPart("data") AnalysisRequestDto request,
            
            @Parameter(description = "Arquivo de áudio para ser transcrito (MP3, WAV, etc). Se fornecido, a transcrição gerada terá prioridade sobre o campo 'transcription' do JSON.", required = false)
            @RequestPart(value = "file", required = false) MultipartFile file) {
        
        String transcription = request.getTranscription();

        if (file != null && !file.isEmpty()) {
            transcription = openAIService.transcribeAudio(file);
        }

        if (transcription == null || transcription.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        
        String audioFilename = (file != null) ? file.getOriginalFilename() : null;
        
        OpenAiAnalysisResponseDTO response = openAIService.compareTranscribedTextAndScript(
            transcription, 
            request.getScriptItems(), 
            request.getClientName(), 
            audioFilename
        );
        return ResponseEntity.ok(response);
    }


    
}
