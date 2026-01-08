package com.bridge.secto.services;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bridge.secto.dtos.CompanyRegistrationRequest;
import com.bridge.secto.dtos.CompanyRegistrationResponse;
import com.bridge.secto.entities.Company;
import com.bridge.secto.entities.CompanyCredit;
import com.bridge.secto.repositories.CompanyRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyRegistrationService {

    private final CompanyRepository companyRepository;
    private final KeycloakAdminService keycloakAdminService;

    @Transactional
    public CompanyRegistrationResponse registerCompany(CompanyRegistrationRequest request) {
        try {
            // 1. Criar empresa no banco
            Company company = createCompany(request);
            
            // 2. Criar admin no Keycloak
            String adminKeycloakId = keycloakAdminService.createUser(
                request.getAdminFirstName(),
                request.getAdminLastName(), 
                request.getAdminEmail(),
                request.getAdminUsername(),
                request.getAdminPassword(),
                company.getId()
            );
            
            company.setOwnerId(UUID.fromString(adminKeycloakId));
            companyRepository.save(company);
            
            log.info("Empresa registrada: {} (ID: {}) com admin: {}", 
                    company.getName(), company.getId(), request.getAdminEmail());

            return CompanyRegistrationResponse.builder()
                .companyId(company.getId())
                .companyName(company.getName())
                .ownerId(UUID.fromString(adminKeycloakId))
                .adminEmail(request.getAdminEmail())
                .message("Empresa criada com sucesso! Verifique seu email para ativação.")
                .build();
                
        } catch (Exception e) {
            log.error("Erro ao registrar empresa: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao registrar empresa: " + e.getMessage());
        }
    }

    private Company createCompany(CompanyRegistrationRequest request) {
        // Criar crédito inicial da empresa
        CompanyCredit credit = new CompanyCredit();
        credit.setCreditAmount(BigDecimal.valueOf(100.00)); // Crédito inicial
        
        // Criar empresa
        Company company = new Company();
        company.setName(request.getCompanyName());
        company.setCompanyCredit(credit);
        
        // Estabelecer relação bidirecional
        credit.setCompany(company);
        
        return companyRepository.save(company);
    }

    /**
     * Adicionar usuário à empresa existente (usado pelo admin da empresa)
     */
    @Transactional
    public String addUserToCompany(String firstName, String lastName, String email,
                                 String username, String password, java.util.UUID companyId) {
        
        // Verificar se empresa existe
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));
            
        // Criar usuário no Keycloak
        return keycloakAdminService.createCompanyUser(
            firstName, lastName, email, username, password, companyId);
    }
}