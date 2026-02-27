package com.bridge.secto.services;

import java.math.BigDecimal;
import java.util.Map;
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
            Company company = createCompany(request);
            
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
        
        String clientId = UUID.randomUUID().toString();
        
        Company company = new Company();
        company.setName(request.getCompanyName());
        company.setCompanyCredit(credit);
        company.setClientId(clientId);
        
        // Estabelecer relação bidirecional
        credit.setCompany(company);
        
        // Salvar primeiro para ter o ID da empresa
        company = companyRepository.save(company);
        
        // Criar client no Keycloak
        try {
            Map<String, String> clientCredentials = keycloakAdminService.createClientCredentials(clientId, company.getName());
            company.setClientSecret(clientCredentials.get("clientSecret"));
            
            // Salvar novamente com o client_secret
            company = companyRepository.save(company);
            
            log.info("Client credentials criado para empresa {}: clientId={}", company.getName(), clientId);
        } catch (Exception e) {
            log.error("Erro ao criar client credentials para empresa {}: {}", company.getName(), e.getMessage());
            // Não falhar a criação da empresa se houver erro no client
            log.warn("Empresa criada sem client credentials");
        }
        
        return company;
    }

    /**
     * Adicionar usuário à empresa existente (usado pelo admin da empresa)
     */
    @Transactional
    public String addUserToCompany(String firstName, String lastName, String email,
                                 String username, String password, java.util.UUID companyId,
                                 boolean isAdmin) {
        
        // Verificar se empresa existe
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));
            
        // Criar usuário no Keycloak
        return keycloakAdminService.createCompanyUser(
            firstName, lastName, email, username, password, companyId, isAdmin);
    }
}