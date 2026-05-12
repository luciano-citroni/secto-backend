package com.bridge.secto.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AudioMetadataService {

    /**
     * Extrai a duração exata de um arquivo de áudio usando FFprobe.
     * Utiliza múltiplas estratégias em cascata para garantir compatibilidade
     * com todos os formatos (MP3, FLAC, OGG, WAV, M4A, etc.).
     */
    public double getAudioDurationInSeconds(MultipartFile audioFile) {
        File tempFile = null;
        try {
            String originalFilename = audioFile.getOriginalFilename();
            String suffix = ".tmp";
            if (originalFilename != null && originalFilename.contains(".")) {
                suffix = originalFilename.substring(originalFilename.lastIndexOf('.'));
            }
            tempFile = Files.createTempFile("audio_", suffix).toFile();
            Files.copy(audioFile.getInputStream(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // Estratégia 1: format=duration (funciona para a maioria dos formatos)
            Double duration = tryFormatDuration(tempFile);
            if (duration != null) {
                log.info("Duração extraída via format=duration: {} segundos para: {}", duration, originalFilename);
                return duration;
            }

            // Estratégia 2: stream=duration (fallback para alguns formatos)
            duration = tryStreamDuration(tempFile);
            if (duration != null) {
                log.info("Duração extraída via stream=duration: {} segundos para: {}", duration, originalFilename);
                return duration;
            }

            // Estratégia 3: calcular a partir de nb_samples/sample_rate (confiável para FLAC)
            duration = tryCalculateFromSamples(tempFile);
            if (duration != null) {
                log.info("Duração calculada via nb_samples/sample_rate: {} segundos para: {}", duration, originalFilename);
                return duration;
            }

            // Estratégia 4: decodificar o arquivo para medir duração real (mais lento, mas mais confiável)
            duration = tryDecodeDuration(tempFile);
            if (duration != null) {
                log.info("Duração extraída via decodificação completa: {} segundos para: {}", duration, originalFilename);
                return duration;
            }

            throw new RuntimeException("Não foi possível determinar a duração do arquivo: " + originalFilename);

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
     * Estratégia 1: duração do container (format=duration).
     */
    private Double tryFormatDuration(File file) {
        return runFfprobeForDuration(file, List.of(
                "ffprobe", "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                file.getAbsolutePath()
        ));
    }

    /**
     * Estratégia 2: duração do stream de áudio (stream=duration).
     */
    private Double tryStreamDuration(File file) {
        return runFfprobeForDuration(file, List.of(
                "ffprobe", "-v", "error",
                "-select_streams", "a:0",
                "-show_entries", "stream=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                file.getAbsolutePath()
        ));
    }

    /**
     * Estratégia 3: calcular duração a partir de duration_ts e sample_rate.
     * Muito confiável para FLAC e outros formatos lossless.
     */
    private Double tryCalculateFromSamples(File file) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ffprobe", "-v", "error",
                    "-select_streams", "a:0",
                    "-show_entries", "stream=sample_rate,duration_ts",
                    "-of", "default=noprint_wrappers=1",
                    file.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!sb.isEmpty()) sb.append("\n");
                    sb.append(line.trim());
                }
                output = sb.toString().trim();
            }

            int exitCode = process.waitFor();
            log.debug("Estratégia 3 (samples) - exitCode={}, output=[{}]", exitCode, output);
            if (exitCode != 0 || output.isEmpty()) {
                return null;
            }

            // Output formato: sample_rate=44100\nduration_ts=12345678
            Long sampleRate = null;
            Long durationTs = null;

            for (String line : output.split("\n")) {
                line = line.trim();
                if (line.startsWith("sample_rate=")) {
                    String val = line.substring("sample_rate=".length()).trim();
                    if (!"N/A".equalsIgnoreCase(val)) {
                        sampleRate = Long.parseLong(val);
                    }
                } else if (line.startsWith("duration_ts=")) {
                    String val = line.substring("duration_ts=".length()).trim();
                    if (!"N/A".equalsIgnoreCase(val)) {
                        durationTs = Long.parseLong(val);
                    }
                }
            }

            if (sampleRate != null && durationTs != null && sampleRate > 0 && durationTs > 0) {
                double duration = (double) durationTs / sampleRate;
                log.debug("Estratégia 3 calculou: {} / {} = {} segundos", durationTs, sampleRate, duration);
                return duration;
            }
        } catch (Exception e) {
            log.warn("Fallback nb_samples/sample_rate falhou: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Estratégia 4: usar ffmpeg para decodificar o arquivo completo e extrair a duração.
     * Mais lento, porém funciona para qualquer arquivo de áudio válido.
     */
    private Double tryDecodeDuration(File file) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-i", file.getAbsolutePath(),
                    "-f", "null",
                    "-"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String lastTimeLine = null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // ffmpeg imprime linhas como: "size=... time=00:04:30.12 ..."
                    if (line.contains("time=")) {
                        lastTimeLine = line;
                    }
                }
            }

            int exitCode = process.waitFor();
            log.debug("Estratégia 4 (ffmpeg decode) - exitCode={}, lastTimeLine=[{}]", exitCode, lastTimeLine);

            if (lastTimeLine != null) {
                // Extrair time=HH:MM:SS.ss
                int idx = lastTimeLine.indexOf("time=");
                if (idx >= 0) {
                    String timeStr = lastTimeLine.substring(idx + 5).trim();
                    // Cortar no próximo espaço
                    int spaceIdx = timeStr.indexOf(' ');
                    if (spaceIdx > 0) {
                        timeStr = timeStr.substring(0, spaceIdx);
                    }
                    // Parsear HH:MM:SS.ss ou HH:MM:SS
                    Double duration = parseTimeToSeconds(timeStr);
                    if (duration != null && duration > 0) {
                        log.debug("Estratégia 4 extraiu duração: {} segundos de time={}", duration, timeStr);
                        return duration;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Fallback por decodificação ffmpeg falhou: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Converte timestamp HH:MM:SS.ss para segundos.
     */
    private Double parseTimeToSeconds(String timeStr) {
        try {
            String[] parts = timeStr.split(":");
            if (parts.length == 3) {
                double hours = Double.parseDouble(parts[0]);
                double minutes = Double.parseDouble(parts[1]);
                double seconds = Double.parseDouble(parts[2]);
                return hours * 3600 + minutes * 60 + seconds;
            } else if (parts.length == 2) {
                double minutes = Double.parseDouble(parts[0]);
                double seconds = Double.parseDouble(parts[1]);
                return minutes * 60 + seconds;
            }
        } catch (NumberFormatException e) {
            log.debug("Erro ao parsear timestamp '{}': {}", timeStr, e.getMessage());
        }
        return null;
    }

    /**
     * Executa FFprobe com os argumentos fornecidos e tenta extrair uma duração numérica válida.
     */
    private Double runFfprobeForDuration(File file, List<String> command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String output = null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !"N/A".equalsIgnoreCase(line)) {
                        output = line;
                        break;
                    }
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0 || output == null) {
                return null;
            }

            double duration = Double.parseDouble(output);
            return duration > 0 ? duration : null;

        } catch (Exception e) {
            log.debug("FFprobe falhou com comando {}: {}", command.get(command.size() - 1), e.getMessage());
            return null;
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