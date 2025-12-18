package com.bridge.secto.controllers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bridge.secto.dtos.CompanyResponseDto;
import com.bridge.secto.entities.Company;
import com.bridge.secto.entities.CompanyCredit;
import com.bridge.secto.repositories.CompanyRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyRepository companyRepository;

    @Operation(summary = "List all companies")
    @ApiResponse(responseCode = "200", description = "Successful operation")
    @GetMapping
    public ResponseEntity<List<CompanyResponseDto>> getAllCompanies() {
        List<CompanyResponseDto> dtos = companyRepository.findAll().stream()
            .map(company -> {
                CompanyResponseDto dto = new CompanyResponseDto();
                dto.setId(company.getId());
                dto.setName(company.getName());
                dto.setOwnerId(company.getOwnerId());
                return dto;
            })
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Get Company by ID")
    @ApiResponse(responseCode = "200", description = "Successful operation")
    @GetMapping("/{id}")
    public ResponseEntity<CompanyResponseDto> getCompanyById(@PathVariable UUID id) {
        Company company = companyRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Company not found with id: " + id));
            
        CompanyResponseDto dto = new CompanyResponseDto();
        dto.setId(company.getId());
        dto.setName(company.getName());
        dto.setOwnerId(company.getOwnerId());
        
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Create Company")
    @ApiResponse(responseCode = "200", description = "Successful operation")
    @PostMapping
    public ResponseEntity<CompanyResponseDto> createCompany(@RequestBody Company request) {
        Company company = new Company();
        company.setName(request.getName());
        company.setOwnerId(request.getOwnerId());
        
        CompanyCredit companyCredit = new CompanyCredit();
        companyCredit.setCreditAmount(BigDecimal.ZERO);
        companyCredit.setCompany(company);
        company.setCompanyCredit(companyCredit);

        companyRepository.save(company);
        
        CompanyResponseDto dto = new CompanyResponseDto();
        dto.setId(company.getId());
        dto.setName(company.getName());
        dto.setOwnerId(company.getOwnerId());
        
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Update Company")
    @ApiResponse(responseCode = "200", description = "Successful operation")
    @PatchMapping("/{id}")
    public ResponseEntity<CompanyResponseDto> updateCompany(@PathVariable UUID id, @RequestBody Company request) {
        Company company = companyRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Company not found with id: " + id));

        if (request.getName() != null) {
            company.setName(request.getName());
        }
        if (request.getOwnerId() != null) {
            company.setOwnerId(request.getOwnerId());
        }

        companyRepository.save(company);

        CompanyResponseDto dto = new CompanyResponseDto();
        dto.setId(company.getId());
        dto.setName(company.getName());
        dto.setOwnerId(company.getOwnerId());

        return ResponseEntity.ok(dto);
    }
}
