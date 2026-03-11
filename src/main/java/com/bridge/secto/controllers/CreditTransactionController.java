package com.bridge.secto.controllers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bridge.secto.dtos.CreditTransactionResponseDTO;
import com.bridge.secto.entities.CompanyCredit;
import com.bridge.secto.entities.CreditTransaction;
import com.bridge.secto.repositories.CompanyCreditRepository;
import com.bridge.secto.repositories.CreditTransactionRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/creditTransactions")
@RequiredArgsConstructor
@Tag(name = "Transações de Crédito", description = "Endpoints para consulta e criação de transações de crédito. Inclui informações de quem realizou cada compra.")
@SecurityRequirement(name = "keycloak")
public class CreditTransactionController {

    private final CompanyCreditRepository companyCreditRepository;
    private final CreditTransactionRepository creditTransactionRepository;

    @Operation(summary = "Listar transações de crédito", description = "Retorna todas as transações de crédito associadas a uma conta de crédito, incluindo informações do usuário que realizou a compra")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de transações retornada com sucesso"),
        @ApiResponse(responseCode = "404", description = "Conta de crédito não encontrada")
    })
    @GetMapping("/byCompanyCredit/{id}")
    public ResponseEntity<List<CreditTransactionResponseDTO>> getTransactionsByCompanyCreditId(
            @Parameter(description = "ID da conta de crédito da empresa") @PathVariable("id") UUID companyCreditId) {
        CompanyCredit companyCredit = this.companyCreditRepository.findById(companyCreditId)
            .orElseThrow(() -> new RuntimeException("CompanyCredit not found with id: " + companyCreditId));

        List<CreditTransactionResponseDTO> dtos = companyCredit.getCreditTransactions().stream()
            .map(this::mapToResponseDto)
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Criar transação de crédito manual", description = "Adiciona créditos manualmente a uma conta de crédito (ajuste administrativo)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transação criada com sucesso"),
        @ApiResponse(responseCode = "404", description = "Conta de crédito não encontrada")
    })
    @PostMapping("/byCompanyCredit/{id}")
    @Transactional
    public ResponseEntity<CreditTransactionResponseDTO> createTransaction(
            @Parameter(description = "ID da conta de crédito da empresa") @PathVariable("id") UUID companyCreditId,
            @RequestBody CreditTransaction request) {
        CompanyCredit companyCredit = this.companyCreditRepository.findById(companyCreditId)
            .orElseThrow(() -> new RuntimeException("CompanyCredit not found with id: " + companyCreditId));
        
        CreditTransaction creditTransaction = new CreditTransaction();
        creditTransaction.setAmount(request.getAmount());
        creditTransaction.setCompanyCredit(companyCredit);

        creditTransactionRepository.save(creditTransaction);

        companyCredit.setCreditAmount(companyCredit.getCreditAmount().add(request.getAmount()));
        
        companyCreditRepository.save(companyCredit);

        return ResponseEntity.ok(mapToResponseDto(creditTransaction));
    }

    @Operation(summary = "Listar lotes de créditos", description = "Retorna todos os lotes de créditos (transações positivas) com informações de validade e expiração, ordenados por data de expiração")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de lotes retornada com sucesso"),
        @ApiResponse(responseCode = "404", description = "Conta de crédito não encontrada")
    })
    @GetMapping("/lots/byCompanyCredit/{id}")
    public ResponseEntity<List<CreditTransactionResponseDTO>> getLotsByCompanyCreditId(
            @Parameter(description = "ID da conta de crédito da empresa") @PathVariable("id") UUID companyCreditId) {
        this.companyCreditRepository.findById(companyCreditId)
            .orElseThrow(() -> new RuntimeException("CompanyCredit not found with id: " + companyCreditId));

        List<CreditTransactionResponseDTO> dtos = creditTransactionRepository
            .findByCompanyCreditIdAndAmountGreaterThanOrderByExpiresAtAsc(companyCreditId, BigDecimal.ZERO)
            .stream()
            .map(this::mapToResponseDto)
            .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    private CreditTransactionResponseDTO mapToResponseDto(CreditTransaction transaction) {
        CreditTransactionResponseDTO dto = new CreditTransactionResponseDTO();
        dto.setId(transaction.getId());
        dto.setAmount(transaction.getAmount());
        dto.setStripeSessionId(transaction.getStripeSessionId());
        dto.setPurchasedBy(transaction.getPurchasedBy());
        dto.setPurchasedByName(transaction.getPurchasedByName());
        dto.setCreatedAt(transaction.getCreatedAt());
        dto.setExpiresAt(transaction.getExpiresAt());
        dto.setRemainingAmount(transaction.getRemainingAmount());
        dto.setSourceType(transaction.getSourceType());
        dto.setIntervalType(transaction.getIntervalType());
        dto.setAnalysisResultId(transaction.getAnalysisResultId());
        return dto;
    }
    
}
