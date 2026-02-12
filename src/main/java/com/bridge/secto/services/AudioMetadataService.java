package com.bridge.secto.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AudioMetadataService {

    /**
     * Extrai a duração exata de um arquivo de áudio usando FFprobe.
     * FFprobe decodifica o stream real, garantindo precisão total inclusive com VBR.
     */
    public double getAudioDurationInSeconds(MultipartFile audioFile) {
        File tempFile = null;
        try {
            // Salvar em arquivo temporário preservando a extensão
            String originalFilename = audioFile.getOriginalFilename();
            String suffix = ".tmp";
            if (originalFilename != null && originalFilename.contains(".")) {
                suffix = originalFilename.substring(originalFilename.lastIndexOf('.'));
            }
            tempFile = Files.createTempFile("audio_", suffix).toFile();
            Files.copy(audioFile.getInputStream(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // Executar FFprobe para obter duração exata
            ProcessBuilder pb = new ProcessBuilder(
                "ffprobe",
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                tempFile.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = reader.readLine();
            }

            int exitCode = process.waitFor();
            if (exitCode != 0 || output == null || output.isBlank()) {
                throw new RuntimeException("FFprobe não conseguiu ler a duração do arquivo: " + originalFilename);
            }

            double durationInSeconds = Double.parseDouble(output.trim());
            if (durationInSeconds <= 0) {
                throw new RuntimeException("Duração inválida extraída do arquivo: " + originalFilename);
            }

            log.info("Duração exata extraída via FFprobe: {} segundos para arquivo: {}",
                    durationInSeconds, originalFilename);
            return durationInSeconds;

        } catch (NumberFormatException e) {
            log.error("Erro ao converter duração do FFprobe: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao processar duração do áudio: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro ao extrair duração do áudio via FFprobe: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao ler duração do arquivo de áudio: " + e.getMessage(), e);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * Verifica se o arquivo é um formato de áudio suportado
     */
    public boolean isSupportedAudioFormat(String filename) {
        if (filename == null) return false;

        String lowerCaseFilename = filename.toLowerCase();
        return lowerCaseFilename.endsWith(".mp3") ||
               lowerCaseFilename.endsWith(".ogg") ||
               lowerCaseFilename.endsWith(".flac") ||
               lowerCaseFilename.endsWith(".m4a") ||
               lowerCaseFilename.endsWith(".mp4") ||
               lowerCaseFilename.endsWith(".wav") ||
               lowerCaseFilename.endsWith(".wma");
    }
}