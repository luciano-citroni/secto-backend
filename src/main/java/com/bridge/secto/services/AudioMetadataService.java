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
     * Estratégia 3: calcular duração a partir de nb_samples e sample_rate.
     * Muito confiável para FLAC e outros formatos lossless.
     */
    private Double tryCalculateFromSamples(File file) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ffprobe", "-v", "error",
                    "-select_streams", "a:0",
                    "-show_entries", "stream=sample_rate,duration_ts,nb_frames",
                    "-show_entries", "format_tags=",
                    "-of", "csv=p=0",
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
            if (exitCode != 0 || output.isEmpty()) {
                return null;
            }

            // CSV output: sample_rate,duration_ts,nb_frames
            String[] parts = output.split("[,\\n]");
            Long sampleRate = null;
            Long totalSamples = null;

            for (String part : parts) {
                part = part.trim();
                if (part.isEmpty() || "N/A".equalsIgnoreCase(part)) continue;
                try {
                    long val = Long.parseLong(part);
                    if (sampleRate == null && val >= 8000 && val <= 384000) {
                        sampleRate = val;
                    } else if (totalSamples == null && val > 0) {
                        totalSamples = val;
                    }
                } catch (NumberFormatException ignored) {
                }
            }

            if (sampleRate != null && totalSamples != null && sampleRate > 0) {
                double duration = (double) totalSamples / sampleRate;
                if (duration > 0) {
                    return duration;
                }
            }
        } catch (Exception e) {
            log.debug("Fallback nb_samples/sample_rate falhou: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Estratégia 4: decodificar o arquivo completo para obter a duração real.
     * Mais lento, porém funciona para qualquer formato válido.
     */
    private Double tryDecodeDuration(File file) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ffprobe", "-v", "error",
                    "-select_streams", "a:0",
                    "-count_packets",
                    "-show_entries", "stream=nb_read_packets,sample_rate,codec_time_base",
                    "-of", "csv=p=0",
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
            if (exitCode != 0 || output.isEmpty()) {
                return null;
            }

            // Tentar parsear codec_time_base (ex: 1/44100) e nb_read_packets
            String[] parts = output.split("[,\\n]");
            Long nbPackets = null;
            Long sampleRate = null;
            String codecTimeBase = null;

            for (String part : parts) {
                part = part.trim();
                if (part.isEmpty() || "N/A".equalsIgnoreCase(part)) continue;
                if (part.contains("/")) {
                    codecTimeBase = part;
                } else {
                    try {
                        long val = Long.parseLong(part);
                        if (sampleRate == null && val >= 8000 && val <= 384000) {
                            sampleRate = val;
                        } else if (val > 0) {
                            nbPackets = val;
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            // Calcular duração via codec_time_base * nb_read_packets
            if (nbPackets != null && codecTimeBase != null) {
                String[] tbParts = codecTimeBase.split("/");
                if (tbParts.length == 2) {
                    double num = Double.parseDouble(tbParts[0]);
                    double den = Double.parseDouble(tbParts[1]);
                    if (den > 0) {
                        double duration = nbPackets * (num / den);
                        if (duration > 0) return duration;
                    }
                }
            }

            // Fallback: nb_read_packets / sample_rate (para FLAC cada packet = 1 frame = 1 sample block)
            if (nbPackets != null && sampleRate != null && sampleRate > 0) {
                // Para FLAC, nb_read_packets normalmente corresponde ao número de frames,
                // mas a duração real depende do blocksize. Usar codec_time_base é preferível.
                log.debug("Fallback decode: packets={}, sampleRate={} - sem codec_time_base confiável", nbPackets, sampleRate);
            }

        } catch (Exception e) {
            log.debug("Fallback por decodificação falhou: {}", e.getMessage());
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