package com.bridge.secto.services;

import java.io.IOException;
import java.io.InputStream;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.XMPDM;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AudioMetadataService {

    /**
     * Extrai a duração exata de um arquivo de áudio usando Apache Tika
     * Suporta formatos: MP3, OGG, FLAC, MP4, M4A, WAV, WMA
     */
    public double getAudioDurationInSeconds(MultipartFile audioFile) {
        try (InputStream inputStream = audioFile.getInputStream()) {
            
            // Configurar parser do Tika
            AutoDetectParser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler();
            Metadata metadata = new Metadata();
            ParseContext parseContext = new ParseContext();
            
            // Fazer parsing do arquivo
            parser.parse(inputStream, handler, metadata, parseContext);
            
            // Tentar extrair duração de diferentes campos de metadados
            String duration = extractDurationFromMetadata(metadata);
            
            if (duration != null) {
                double durationInSeconds = parseDurationToSeconds(duration);
                log.info("Duração extraída do áudio: {} segundos para arquivo: {}", 
                        durationInSeconds, audioFile.getOriginalFilename());
                return durationInSeconds;
            } else {
                log.warn("Duração não encontrada nos metadados do arquivo: {}", 
                        audioFile.getOriginalFilename());
                return estimateAudioDurationBySize(audioFile);
            }
            
        } catch (IOException | SAXException | TikaException e) {
            log.error("Erro ao extrair metadados do áudio: {}", e.getMessage(), e);
            // Fallback: usar estimativa baseada no tamanho do arquivo
            return estimateAudioDurationBySize(audioFile);
        }
    }

    /**
     * Extrai duração dos metadados em diferentes formatos possíveis
     */
    private String extractDurationFromMetadata(Metadata metadata) {
        // Tentar diferentes campos onde a duração pode estar
        String[] durationFields = {
            XMPDM.DURATION.getName(),
            "duration",
            "Length",
            "xmpDM:duration",
            "Content-Length"
        };
        
        for (String field : durationFields) {
            String value = metadata.get(field);
            if (value != null && !value.trim().isEmpty()) {
                log.debug("Duração encontrada no campo '{}': {}", field, value);
                return value;
            }
        }
        
        // Log todos os metadados para debug
        log.debug("Metadados disponíveis:");
        for (String name : metadata.names()) {
            log.debug("  {}: {}", name, metadata.get(name));
        }
        
        return null;
    }

    /**
     * Converte string de duração para segundos
     * Suporta formatos: "125.5", "02:05.5", "125500" (ms), etc.
     */
    private double parseDurationToSeconds(String duration) {
        try {
            duration = duration.trim();
            
            // Se contém ":" é no formato mm:ss ou hh:mm:ss
            if (duration.contains(":")) {
                String[] parts = duration.split(":");
                double seconds = 0;
                
                if (parts.length == 2) { // mm:ss
                    seconds = Double.parseDouble(parts[0]) * 60 + Double.parseDouble(parts[1]);
                } else if (parts.length == 3) { // hh:mm:ss
                    seconds = Double.parseDouble(parts[0]) * 3600 + 
                             Double.parseDouble(parts[1]) * 60 + 
                             Double.parseDouble(parts[2]);
                }
                return seconds;
            } else {
                // Número simples - pode ser segundos ou milissegundos
                double value = Double.parseDouble(duration);
                
                // Se o valor for muito grande, provavelmente são milissegundos
                if (value > 86400) { // Mais que 24 horas em segundos = provável ms
                    return value / 1000.0;
                }
                
                return value;
            }
            
        } catch (NumberFormatException e) {
            log.warn("Não foi possível converter duração '{}' para segundos: {}", duration, e.getMessage());
            return 0;
        }
    }

    /**
     * Método de fallback: estima duração baseada no tamanho
     * Usado quando não é possível extrair metadados
     */
    private double estimateAudioDurationBySize(MultipartFile file) {
        log.warn("Usando estimativa por tamanho para arquivo: {} ({}KB)", 
                file.getOriginalFilename(), file.getSize() / 1024);
        
        // Estimativa baseada no tipo de arquivo e tamanho
        String filename = file.getOriginalFilename();
        long fileSizeInBytes = file.getSize();
        
        if (filename != null) {
            String lowerCaseFilename = filename.toLowerCase();
            
            // MP3: ~1MB por minuto (128kbps)
            if (lowerCaseFilename.endsWith(".mp3")) {
                double fileSizeInMB = fileSizeInBytes / (1024.0 * 1024.0);
                return fileSizeInMB * 60; // 1MB ≈ 1 minuto
            }
            // WAV: ~10MB por minuto (sem compressão)
            else if (lowerCaseFilename.endsWith(".wav")) {
                double fileSizeInMB = fileSizeInBytes / (1024.0 * 1024.0);
                return fileSizeInMB * 6; // 10MB ≈ 1 minuto
            }
            // OGG/FLAC: qualidade similar ao MP3
            else {
                double fileSizeInMB = fileSizeInBytes / (1024.0 * 1024.0);
                return fileSizeInMB * 60; // estimativa conservadora
            }
        }
        
        // Fallback genérico
        double fileSizeInMB = fileSizeInBytes / (1024.0 * 1024.0);
        return fileSizeInMB * 60;
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