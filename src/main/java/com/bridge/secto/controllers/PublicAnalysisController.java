package com.bridge.secto.controllers;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bridge.secto.dtos.ListenStatusResponseDto;
import com.bridge.secto.repositories.AnalysisResultRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/public/analysis")
@RequiredArgsConstructor
@Tag(name = "Public Analysis", description = "Public endpoints for analysis status lookup")
public class PublicAnalysisController {

    private static final Pattern LISTEN_FILENAME_PATTERN =
            Pattern.compile("^listen-([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})\\.\\w+$");

    private final AnalysisResultRepository repository;

    @GetMapping("/status")
    @Operation(summary = "Get analysis approval status by listen filename")
    public ResponseEntity<ListenStatusResponseDto> getStatusByFilename(@RequestParam String filename) {
        Matcher matcher = LISTEN_FILENAME_PATTERN.matcher(filename);
        if (!matcher.matches()) {
            return ResponseEntity.badRequest().build();
        }

        UUID analysisId = UUID.fromString(matcher.group(1));

        return repository.findById(analysisId)
                .map(result -> ResponseEntity.ok(new ListenStatusResponseDto(result.getApproved())))
                .orElse(ResponseEntity.notFound().build());
    }
}
