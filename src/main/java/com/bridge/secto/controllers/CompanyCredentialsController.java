package com.bridge.secto.controllers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bridge.secto.entities.Company;
import com.bridge.secto.services.AuthService;
import com.bridge.secto.services.KeycloakAdminService;
import com.bridge.secto.repositories.CompanyRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/companies/current")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Company Management", description = "Gestão da empresa atual")
public class CompanyCredentialsController {

    private final AuthService authService;
    private final CompanyRepository companyRepository;
    private final KeycloakAdminService keycloakAdminService;

    @GetMapping("/credentials")
    @PreAuthorize("@authService.isCompanyAdmin() or @authService.isUserInCompany(@authService.getCurrentUser().orElseThrow().getCompanyId())")
    @SecurityRequirement(name = "keycloak")
    @Operation(summary = "Obter credenciais da empresa atual", 
               description = "Retorna client_id e client_secret da empresa do usuário logado")
    public ResponseEntity<Map<String, String>> getCompanyCredentials() {
        
        UUID companyId = authService.getCurrentUser()
            .map(AuthService.UserInfo::getCompanyId)
            .orElseThrow(() -> new RuntimeException("Company ID não encontrado"));

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));

        if (company.getClientId() == null) {
            return ResponseEntity.ok(Map.of(
                "message", "Nenhuma credencial de client encontrada para esta empresa"
            ));
        }

        return ResponseEntity.ok(Map.of(
            "clientId", company.getClientId(),
            "clientSecret", company.getClientSecret() != null ? company.getClientSecret() : ""
        ));
    }

    @PostMapping("/credentials/regenerate")
    @PreAuthorize("@authService.isCompanyAdmin()")
    @SecurityRequirement(name = "keycloak")
    @Operation(summary = "Regenerar client secret", 
               description = "Regenera o client_secret da empresa atual. Apenas admins podem fazer isso.")
    public ResponseEntity<Map<String, String>> regenerateClientSecret() {
        
        UUID companyId = authService.getCurrentUser()
            .map(AuthService.UserInfo::getCompanyId)
            .orElseThrow(() -> new RuntimeException("Company ID não encontrado"));

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));

        if (company.getClientId() == null) {
            throw new RuntimeException("Empresa não possui client credentials configurados");
        }

        try {
            // Regenerar client secret no Keycloak
            Map<String, String> newCredentials = keycloakAdminService.regenerateClientSecret(
                company.getClientId(), company.getName());
            
            // Atualizar no banco de dados
            company.setClientSecret(newCredentials.get("clientSecret"));
            companyRepository.save(company);
            
            log.info("Client secret regenerado para empresa: {} (ID: {})", company.getName(), company.getId());
            
            return ResponseEntity.ok(Map.of(
                "clientId", company.getClientId(),
                "clientSecret", company.getClientSecret()
            ));
            
        } catch (Exception e) {
            log.error("Erro ao regenerar client secret para empresa {}: {}", company.getName(), e.getMessage());
            throw new RuntimeException("Erro ao regenerar client secret: " + e.getMessage());
        }
    }

    @GetMapping("/users")
    @PreAuthorize("@authService.isCompanyAdmin() or @authService.isUserInCompany(@authService.getCurrentUser().orElseThrow().getCompanyId())")
    @SecurityRequirement(name = "keycloak")
    @Operation(summary = "Listar usuários da empresa atual", 
               description = "Retorna todos os usuários da empresa do usuário logado")
    public ResponseEntity<List<Map<String, Object>>> getCompanyUsers() {
        
        UUID companyId = authService.getCurrentUser()
            .map(AuthService.UserInfo::getCompanyId)
            .orElseThrow(() -> new RuntimeException("Company ID não encontrado"));

        try {
            List<Map<String, Object>> users = keycloakAdminService.getCompanyUsers(companyId);
            return ResponseEntity.ok(users);
            
        } catch (Exception e) {
            log.error("Erro ao buscar usuários da empresa: {}", e.getMessage());
            throw new RuntimeException("Erro ao buscar usuários da empresa: " + e.getMessage());
        }
    }
}