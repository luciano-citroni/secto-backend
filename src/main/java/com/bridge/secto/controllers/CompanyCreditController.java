package com.bridge.secto.controllers;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.bridge.secto.dtos.CompanyCreditRequest;
import com.bridge.secto.dtos.CompanyCreditResponseDto;
import com.bridge.secto.entities.Company;
import com.bridge.secto.entities.CompanyCredit;
import com.bridge.secto.exceptions.BusinessRuleException;
import com.bridge.secto.exceptions.ResourceNotFoundException;
import com.bridge.secto.repositories.CompanyRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/companyCredits")
@RequiredArgsConstructor
@Tag(name = "Créditos da Empresa", description = "Endpoints para gerenciamento de contas de crédito das empresas")
public class CompanyCreditController {

    private final CompanyRepository companyRepository;

    @Operation(summary = "Criar conta de crédito", description = "Cria uma conta de crédito para uma empresa. Cada empresa pode ter apenas uma conta de crédito")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Conta de crédito criada com sucesso"),
        @ApiResponse(responseCode = "404", description = "Empresa não encontrada"),
        @ApiResponse(responseCode = "400", description = "Empresa já possui conta de crédito")
    })
    @PostMapping("/")
    @ResponseBody
    @Transactional
    public ResponseEntity<CompanyCredit> createCompanyCredit(@RequestBody CompanyCreditRequest request) {
        Company company = companyRepository.findById(request.getCompany())
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with id: " + request.getCompany()));

        if (company.getCompanyCredit() != null) {
            throw new BusinessRuleException("Company already has a credit account");
        }

        CompanyCredit credit = new CompanyCredit();
        credit.setCreditAmount(request.getCreditAmount());
        credit.setCompany(company);

        company.setCompanyCredit(credit);
        
        companyRepository.save(company);
        
        return ResponseEntity.ok(company.getCompanyCredit());
    }

    @Operation(summary = "Consultar saldo de créditos", description = "Retorna o saldo de créditos da empresa. Cria automaticamente uma conta com saldo zero se não existir")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Saldo retornado com sucesso"),
        @ApiResponse(responseCode = "404", description = "Empresa não encontrada")
    })
    @GetMapping("/byCompanyId/{id}")
    @ResponseBody
    @Transactional
    public ResponseEntity<CompanyCreditResponseDto> getCompanyCreditByCompanyId(
            @Parameter(description = "ID da empresa") @PathVariable("id") UUID companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with id: " + companyId));
        
        CompanyCredit credit = company.getCompanyCredit();
        
        // Se não existe CompanyCredit, criar um com saldo zero
        if (credit == null) {
            credit = new CompanyCredit();
            credit.setCreditAmount(BigDecimal.ZERO);
            credit.setCompany(company);
            company.setCompanyCredit(credit);
            companyRepository.save(company);
        }
        
        CompanyCreditResponseDto dto = new CompanyCreditResponseDto();
        dto.setId(credit.getId());
        dto.setCreditAmount(credit.getCreditAmount());
        
        return ResponseEntity.ok(dto);
    }
}
