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

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/companyCredits")
@RequiredArgsConstructor
public class CompanyCreditController {

    private final CompanyRepository companyRepository;

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

    @GetMapping("/byCompanyId/{id}")
    @ResponseBody
    @Transactional
    public ResponseEntity<CompanyCreditResponseDto> getCompanyCreditByCompanyId(@PathVariable("id") UUID companyId) {
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
