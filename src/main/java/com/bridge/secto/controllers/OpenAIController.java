package com.bridge.secto.controllers;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.bridge.secto.dtos.AnalysisRequestDto;
import com.bridge.secto.dtos.CreditEstimationResponseDto;
import com.bridge.secto.dtos.OpenAiAnalysisResponseDTO;
import com.bridge.secto.exceptions.BusinessRuleException;
import com.bridge.secto.services.AudioMetadataService;
import com.bridge.secto.services.CreditService;
import com.bridge.secto.services.OpenAIService;
import com.bridge.secto.services.S3StorageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/analyze")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI Analysis", description = "Endpoints for interacting with OpenAI")
public class OpenAIController {

    private final OpenAIService openAIService;
    private final S3StorageService s3StorageService;
    private final CreditService creditService;
    private final AudioMetadataService audioMetadataService;

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
        String audioFilename = null;
        String audioUrl = null;
        Double audioDurationInSeconds = null;

        if (file != null && !file.isEmpty()) {
            // Extrair duração exata do arquivo de áudio
            audioDurationInSeconds = audioMetadataService.getAudioDurationInSeconds(file);
            log.info("Duração do áudio extraída: {} segundos", audioDurationInSeconds);
            
            // Verificar se há créditos suficientes antes de processar
            double requiredCredits = creditService.calculateCreditsForDuration(audioDurationInSeconds);
            UUID companyId = creditService.getCurrentCompanyId();
            if (!creditService.hasEnoughCredits(companyId, requiredCredits)) {
                throw new BusinessRuleException("Créditos insuficientes para realizar esta análise. Necessário: " + requiredCredits + " créditos");
            }

            transcription = openAIService.transcribeAudio(file);
            var uploadResponse = s3StorageService.uploadFile(file);
            audioFilename = uploadResponse.fileName();
            audioUrl = uploadResponse.fileUrl();
        }

        if (transcription == null || transcription.isBlank()) {
            throw new BusinessRuleException("Transcription or audio file is required.");
        }
        
        OpenAiAnalysisResponseDTO response = openAIService.compareTranscribedTextAndScript(
            transcription, 
            request.getScriptItems(), 
            request.getClientName(), 
            audioFilename,
            audioUrl,
            request.getScriptId()
        );

        // Descontar créditos após análise bem-sucedida
        if (audioDurationInSeconds != null && audioDurationInSeconds > 0) {
            creditService.debitCreditsForAnalysis(request.getClientName(), audioDurationInSeconds);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/calculate-credits")
    @Operation(
        summary = "Calcular créditos estimados para análise", 
        description = "Calcula a estimativa de créditos necessários baseado na duração do áudio. Regra: 1 crédito por 60 segundos.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Cálculo realizado com sucesso", 
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = CreditEstimationResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Duração inválida")
        }
    )
    public ResponseEntity<CreditEstimationResponseDto> calculateCredits(
            @Parameter(description = "Duração do áudio em segundos", required = true)
            @RequestParam("duration") Double duration) {
        
        if (duration == null || duration <= 0) {
            throw new BusinessRuleException("Duration must be greater than 0");
        }

        // Usar o serviço de créditos para cálculo
        double estimatedCredits = creditService.calculateCreditsForDuration(duration);

        CreditEstimationResponseDto response = new CreditEstimationResponseDto();
        response.setDurationInSeconds(duration);
        response.setEstimatedCredits(estimatedCredits);
        
        return ResponseEntity.ok(response);
    }


    
}
