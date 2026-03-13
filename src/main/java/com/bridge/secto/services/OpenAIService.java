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
import com.bridge.secto.entities.Client;
import com.bridge.secto.entities.Company;
import com.bridge.secto.repositories.AnalysisResultRepository;
import com.bridge.secto.repositories.ClientRepository;
import com.bridge.secto.repositories.CompanyRepository;
import com.bridge.secto.repositories.ScriptRepository;
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
    private final ClientRepository clientRepository;
    private final CompanyRepository companyRepository;
    private final ScriptRepository scriptRepository;
    private final AuthService authService;
    private final ObjectMapper objectMapper;

    public record AnalysisProcessingResult(OpenAiAnalysisResponseDTO response, UUID analysisResultId) {}

    public AnalysisProcessingResult compareTranscribedTextAndScript(String transcription, List<ScriptItemInputDto> scriptItems, UUID clientId, String audioFilename, String audioUrl, UUID scriptId, Double creditsUsed, String executedBy) {

        String scriptText = scriptItems.stream()
                .map(item -> String.format("Question: %s\nAnswer: %s", item.getQuestion(), item.getAnswer()))
                .collect(Collectors.joining("\n\n"));

        String prompt = String.format("Você é um avaliador rigoroso de conformidade contratual. Analise a transcrição com base no script fornecido. Para cada item do script, execute DUAS validações:\n\n" +
                "1. VALIDAÇÃO DA PERGUNTA (questionAsked): Verifique se a pergunta do script foi de fato realizada no áudio/transcrição. A pergunta não precisa ser idêntica palavra por palavra, mas deve preservar INTEGRALMENTE o sentido original, incluindo todas as condições, valores, prazos e termos contratuais. Defina questionAsked como true SOMENTE se a pergunta na transcrição for semanticamente fiel ao script, sem alterações que modifiquem o significado. Se a pergunta não foi feita ou foi feita com alterações que mudam o sentido, defina questionAsked como false.\n\n" +
                "ATENÇÃO — DIFERENÇAS SEMÂNTICAS QUE INVALIDAM A PERGUNTA:\n" +
                "Trate qualquer modificação que altere o significado real da frase como ERRO, mesmo que pareça sutil. Exemplos críticos:\n" +
                "- 'em 12 prestações' vs 'até 12 prestações' → significados DIFERENTES (valor fixo vs valor máximo).\n" +
                "- '3 vezes' vs 'até 3 vezes' → significados DIFERENTES.\n" +
                "- '30%%' vs '20%%' → valores DIFERENTES.\n" +
                "- 'e' vs 'ou' ligando condições → lógica DIFERENTE.\n" +
                "- 'será cobrado' vs 'poderá ser cobrado' → obrigatoriedade DIFERENTE.\n" +
                "- 'após' vs 'antes de' → temporalidade DIFERENTE.\n" +
                "- Adição ou remoção de palavras como 'até', 'no mínimo', 'no máximo', 'aproximadamente', 'a partir de' que alteram o escopo quantitativo ou temporal.\n" +
                "Em contexto contratual, cada palavra que qualifica valores, prazos, condições ou obrigações é essencial. Palavras como 'até', 'a partir de', 'no mínimo', 'no máximo' alteram fundamentalmente o sentido jurídico e devem ser tratadas com rigor absoluto.\n\n" +
                "2. VALIDAÇÃO DA RESPOSTA (correct): Identifique a resposta presente na transcrição e compare com o campo correctAnswer esperado. Defina correct como true somente se a resposta encontrada for semanticamente equivalente, confirmativa ou compatível com a resposta correta esperada. Se a pergunta não foi sequer feita (questionAsked=false), marque correct como false automaticamente.\n\n" +
                "REGRA ESPECIAL PARA RESPOSTAS 'Sim' E 'Não':\n" +
                "- Quando o campo correctAnswer for 'Sim', considere como corretas QUAISQUER expressões do CLIENTE que, no contexto da pergunta feita, representem uma confirmação ou concordância inequívoca. Exemplos incluem, mas NÃO se limitam a: sim, positivo, concordo, correto, isso, exatamente, com certeza, claro, isso mesmo, pode ser, afirmativo, tudo bem, tá bom, tá certo, ok, beleza, fechado, certo, perfeito, sem dúvida, com certeza, pois não, é isso, uhum, aham, e quaisquer outras expressões que no contexto conversacional brasileiro configurem aceitação ou concordância positiva. O critério é: se uma pessoa razoável entenderia a fala do cliente como uma confirmação no contexto daquela pergunta, então é 'Sim'.\n" +
                "- Quando o campo correctAnswer for 'Não', considere como corretas QUAISQUER expressões do CLIENTE que representem negação ou discordância inequívoca. Exemplos: não, negativo, discordo, incorreto, de forma nenhuma, nunca, nada disso, nem pensar, nada a ver, e quaisquer outras expressões equivalentes a uma negação.\n" +
                "- IMPORTANTE: Identifique claramente quem está falando. A transcrição alterna entre atendente e cliente. A RESPOSTA a ser avaliada é sempre a fala do CLIENTE que vem logo após a pergunta do atendente. Não confunda trechos da fala do atendente com a resposta do cliente.\n\n" +
                "No campo analysis, explique objetivamente: (a) se a pergunta foi feita e cite o trecho relevante — se houve divergência semântica, destaque exatamente qual termo foi alterado e por que isso muda o significado (ex: '\"em 12 prestações\" foi dito como \"até 12 prestações\", o que altera o sentido de valor fixo para valor máximo'), (b) se a resposta confere e o critério de validação usado (confirmação explícita, divergência de dados, ausência de resposta, inconsistência de valor, equivalência semântica). Retorne exclusivamente no formato JSON especificado, sem texto adicional.\n\nScript:\n%s\n\nTranscrição:\n%s", scriptText, transcription);


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

                Client client = null;
                if (clientId != null) {
                    client = clientRepository.findById(clientId)
                        .orElseThrow(() -> new RuntimeException("Client not found with id: " + clientId));
                }

                AnalysisResult result = new AnalysisResult();
                result.setClient(client);
                result.setAudioFilename(audioFilename);
                result.setAudioUrl(audioUrl);
                result.setTranscription(transcription);
                result.setScriptJson(objectMapper.writeValueAsString(scriptItems));
                result.setAiOutputJson(objectMapper.writeValueAsString(response));
                result.setApproved(approved);
                result.setCompany(company);
                result.setCreditsUsed(creditsUsed);
                result.setExecutedBy(executedBy);

                if (scriptId != null) {
                    scriptRepository.findById(scriptId).ifPresent(result::setScript);
                }
                
                analysisResultRepository.save(result);
                return new AnalysisProcessingResult(response, result.getId());
            } catch (Exception e) {
                e.printStackTrace(); 
            }
        }

        return new AnalysisProcessingResult(response, null);
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

            String transcription = transcribeAudioFromPath(tempFile);
            
            Files.deleteIfExists(tempFile);
            
            return transcription;
        } catch (IOException e) {
            throw new RuntimeException("Failed to process audio file", e);
        }
    }

    public String transcribeAudioFromPath(Path audioPath) {
        TranscriptionCreateParams params = TranscriptionCreateParams.builder()
            .file(audioPath)
            .model(AudioModel.GPT_4O_TRANSCRIBE)
            .build();

        return openAIClient.audio().transcriptions().create(params).asTranscription().text();
    }
    
}
