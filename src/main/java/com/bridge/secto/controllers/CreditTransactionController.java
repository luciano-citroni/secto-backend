package com.bridge.secto.controllers;

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

import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/creditTransactions")
@RequiredArgsConstructor
public class CreditTransactionController {

    private final CompanyCreditRepository companyCreditRepository;
    private final CreditTransactionRepository creditTransactionRepository;

    @GetMapping("/byCompanyCredit/{id}")
    public ResponseEntity<List<CreditTransactionResponseDTO>> getTransactionsByCompanyCreditId(@PathVariable("id") UUID companyCreditId) {
        CompanyCredit companyCredit = this.companyCreditRepository.findById(companyCreditId)
            .orElseThrow(() -> new RuntimeException("CompanyCredit not found with id: " + companyCreditId));

        List<CreditTransactionResponseDTO> dtos = companyCredit.getCreditTransactions().stream()
            .map(transaction -> {
                CreditTransactionResponseDTO dto = new CreditTransactionResponseDTO();
                dto.setId(transaction.getId());
                dto.setAmount(transaction.getAmount());
                return dto;
            })
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/byCompanyCredit/{id}")
    @Transactional
    public ResponseEntity<CreditTransactionResponseDTO> createTransaction(@PathVariable("id") UUID companyCreditId, @RequestBody CreditTransaction request) {
        CompanyCredit companyCredit = this.companyCreditRepository.findById(companyCreditId)
            .orElseThrow(() -> new RuntimeException("CompanyCredit not found with id: " + companyCreditId));
        
        CreditTransaction creditTransaction = new CreditTransaction();
        creditTransaction.setAmount(request.getAmount());
        creditTransaction.setCompanyCredit(companyCredit);

        creditTransactionRepository.save(creditTransaction);

        companyCredit.setCreditAmount(companyCredit.getCreditAmount().add(request.getAmount()));
        
        companyCreditRepository.save(companyCredit);

        CreditTransactionResponseDTO dto = new CreditTransactionResponseDTO();
        dto.setId(creditTransaction.getId());
        dto.setAmount(creditTransaction.getAmount());
        
        return ResponseEntity.ok(dto);
    }
    
}
