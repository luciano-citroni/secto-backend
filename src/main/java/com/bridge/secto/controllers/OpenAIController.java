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
    @Operation(summary = "Gerar análise sobre a transcrição do áudio", description = "Gera um json sobre a transcrição do áudio fornecido comparando-o com o script selecionado.")
    public ResponseEntity<OpenAiAnalysisResponseDTO> generateText(
            @RequestPart("data") AnalysisRequestDto request,
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
