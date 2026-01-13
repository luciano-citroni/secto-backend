package com.bridge.secto.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.bridge.secto.dtos.OpenAiAnalysisResponseDTO;
import com.bridge.secto.dtos.ScriptItemInputDto;
import com.bridge.secto.entities.AnalysisResult;
import com.bridge.secto.entities.Company;
import com.bridge.secto.repositories.AnalysisResultRepository;
import com.bridge.secto.repositories.CompanyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.models.ChatModel;
import com.openai.models.audio.AudioModel;
import com.openai.models.audio.transcriptions.TranscriptionCreateParams;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.StructuredChatCompletion;
import com.openai.models.chat.completions.StructuredChatCompletionCreateParams;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OpenAIService {

    private final OpenAIClient openAIClient;
    private final AnalysisResultRepository analysisResultRepository;
    private final CompanyRepository companyRepository;
    private final AuthService authService;
    private final ObjectMapper objectMapper;

    public OpenAiAnalysisResponseDTO compareTranscribedTextAndScript(String transcription, List<ScriptItemInputDto> scriptItems, String clientName, String audioFilename, String audioUrl) {

        String scriptText = scriptItems.stream()
                .map(item -> String.format("Question: %s\nAnswer: %s", item.getQuestion(), item.getAnswer()))
                .collect(Collectors.joining("\n\n"));

        String prompt = String.format("Você é um avaliador rigoroso de conformidade contratual. Analise a transcrição com base no script fornecido em JSON. Para cada item do script, identifique a resposta presente na transcrição e compare com o campo correctAnswer. Defina o campo correct como true somente se a resposta encontrada for semanticamente equivalente, confirmativa ou compatível com a resposta correta esperada. Caso contrário, marque como false. No campo analysis, explique objetivamente o motivo da decisão, citando o trecho relevante da transcrição e comparando-o com a resposta esperada. Não diga que 'a transcrição contém a pergunta e a resposta' ou qualquer explicação óbvia. Explique exclusivamente o critério de validação usado (ex.: confirmação explícita, divergência de dados, ausência de resposta, inconsistência de valor, equivalência semântica). Retorne exclusivamente no formato JSON especificado, sem texto adicional.\n\nScript:\n%s\n\nTranscrição:\n%s", scriptText, transcription);


        StructuredChatCompletionCreateParams<OpenAiAnalysisResponseDTO> params = ChatCompletionCreateParams.builder()
            .addUserMessage(prompt)
            .model(ChatModel.GPT_5_MINI)
            .responseFormat(OpenAiAnalysisResponseDTO.class)
            .build();

        StructuredChatCompletion<OpenAiAnalysisResponseDTO> chatCompletion = openAIClient.chat().completions().create(params);

        OpenAiAnalysisResponseDTO response = chatCompletion.choices().stream()
                .findFirst()
                .flatMap(choice -> choice.message().content())
                .orElse(null);

        if (response != null) {
            response.setTranscription(transcription);
            
            boolean approved = response.getOutput() != null && 
                java.util.Arrays.stream(response.getOutput())
                    .allMatch(OpenAiAnalysisResponseDTO.OutputItem::isCorrect);
            
            response.setApproved(approved);

            try {
                UUID companyId = authService.getCurrentUser()
                    .map(AuthService.UserInfo::getCompanyId)
                    .orElseThrow(() -> new RuntimeException("User not associated with any company"));

                Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new RuntimeException("Company not found with id: " + companyId));

                AnalysisResult result = new AnalysisResult();
                result.setClientName(clientName);
                result.setAudioFilename(audioFilename);
                result.setAudioUrl(audioUrl);
                result.setTranscription(transcription);
                result.setScriptJson(objectMapper.writeValueAsString(scriptItems));
                result.setAiOutputJson(objectMapper.writeValueAsString(response));
                result.setApproved(approved);
                result.setCompany(company);
                
                analysisResultRepository.save(result);
            } catch (Exception e) {
                // Log error but don't fail the request? Or fail?
                // For now, just print stack trace or let it bubble if critical.
                // Better to log and continue or throw.
                e.printStackTrace(); 
            }
        }

        return response;
    }

    public String transcribeAudio(MultipartFile audioFile) {
        try {
            String originalFilename = audioFile.getOriginalFilename();
            String extension = ".mp3";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            Path tempFile = Files.createTempFile("audio", extension);
            Files.copy(audioFile.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

            TranscriptionCreateParams params = TranscriptionCreateParams.builder()
                .file(tempFile)
                .model(AudioModel.WHISPER_1)
                .build();

            String transcription = openAIClient.audio().transcriptions().create(params).asTranscription().text();
            
            Files.deleteIfExists(tempFile);
            
            return transcription;
        } catch (IOException e) {
            throw new RuntimeException("Failed to process audio file", e);
        }
    }
    
}
