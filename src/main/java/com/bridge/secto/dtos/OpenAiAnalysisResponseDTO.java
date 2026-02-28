package com.bridge.secto.dtos;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OpenAiAnalysisResponseDTO {
    private Boolean approved;
    private String transcription;
    private OutputItem[] output;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OutputItem {
        private String question;
        private String answer;
        private boolean correct;
        private boolean questionAsked;
        private String analysis;
    }
}

/* 
            "question": "Qual é o seu nome?",
            "answer": "Meu nome é João.",
            "correct": true,
            "analysis": "A resposta está correta porque o nome corresponde ao esperado."
*/