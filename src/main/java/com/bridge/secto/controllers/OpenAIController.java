package com.bridge.secto.controllers;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.bridge.secto.dtos.AnalysisRequestDto;
import com.bridge.secto.dtos.CreditEstimationResponseDto;
import com.bridge.secto.dtos.OpenAiAnalysisResponseDTO;
import com.bridge.secto.exceptions.BusinessRuleException;
import com.bridge.secto.services.AudioMetadataService;
import com.bridge.secto.services.AuthService;
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
    private final AuthService authService;

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
        
        OpenAIService.AnalysisProcessingResult result = openAIService.compareTranscribedTextAndScript(
            transcription, 
            request.getScriptItems(), 
            request.getClientId(),
            audioFilename,
            audioUrl,
            request.getScriptId(),
            audioDurationInSeconds != null ? creditService.calculateCreditsForDuration(audioDurationInSeconds) : null,
            authService.getCurrentUser().map(AuthService.UserInfo::getName).orElse(null)
        );

        // Descontar créditos após análise bem-sucedida
        if (audioDurationInSeconds != null && audioDurationInSeconds > 0) {
            if (result.analysisResultId() == null) {
                throw new BusinessRuleException("Não foi possível registrar o resultado da análise para vincular a cobrança de créditos.");
            }

            String clientNameForCredits = null;
            if (request.getClientId() != null) {
                clientNameForCredits = "Cliente ID: " + request.getClientId();
            }
            creditService.debitCreditsForAnalysis(clientNameForCredits, audioDurationInSeconds, result.analysisResultId());
        }

        return ResponseEntity.ok(result.response());
    }

    @PostMapping(value = "/calculate-credits", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Calcular créditos exatos para análise", 
        description = "Recebe o arquivo de áudio e calcula os créditos necessários baseado na duração exata. Regra: 1 crédito a cada 60 segundos (arredondado para cima, mínimo 1).",
        responses = {
            @ApiResponse(responseCode = "200", description = "Cálculo realizado com sucesso", 
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = CreditEstimationResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Arquivo inválido ou duração não detectável")
        }
    )
    public ResponseEntity<CreditEstimationResponseDto> calculateCredits(
            @Parameter(description = "Arquivo de áudio para calcular a duração exata", required = true)
            @RequestPart("file") MultipartFile file) {
        
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Arquivo de áudio é obrigatório");
        }

        try {
            double durationInSeconds = audioMetadataService.getAudioDurationInSeconds(file);
            double estimatedCredits = creditService.calculateCreditsForDuration(durationInSeconds);

            CreditEstimationResponseDto response = new CreditEstimationResponseDto();
            response.setDurationInSeconds(durationInSeconds);
            response.setEstimatedCredits(estimatedCredits);
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            throw new BusinessRuleException("Não foi possível determinar a duração do áudio: " + e.getMessage());
        }
    }


    
}
