package com.bridge.secto.controllers;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bridge.secto.dtos.CompanyRegistrationRequest;
import com.bridge.secto.dtos.CompanyRegistrationResponse;
import com.bridge.secto.services.AuthService;
import com.bridge.secto.services.CompanyRegistrationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Company Registration", description = "Registro e gestão de empresas")
public class CompanyRegistrationController {

    private final CompanyRegistrationService registrationService;
    private final AuthService authService;

    @PostMapping("/public/register/company")
    @Operation(summary = "Registrar nova empresa", 
               description = "Endpoint público para registro de empresa + admin inicial")
    public ResponseEntity<CompanyRegistrationResponse> registerCompany(
            @Valid @RequestBody CompanyRegistrationRequest request) {
        
        CompanyRegistrationResponse response = registrationService.registerCompany(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/company/users")
    @PreAuthorize("@authService.isCompanyAdmin()")
    @SecurityRequirement(name = "keycloak")
    @Operation(summary = "Adicionar usuário à empresa", 
               description = "Apenas admins da empresa podem adicionar usuários")
    public ResponseEntity<?> addUserToCompany(@RequestBody Map<String, String> userData) {
        
        UUID companyId = authService.getCurrentUser()
            .map(AuthService.UserInfo::getCompanyId)
            .orElseThrow(() -> new RuntimeException("Company ID não encontrado"));

        String userId = registrationService.addUserToCompany(
            userData.get("firstName"),
            userData.get("lastName"),
            userData.get("email"),
            userData.get("username"),
            userData.get("password"),
            companyId
        );

        return ResponseEntity.ok(Map.of(
            "message", "Usuário criado com sucesso",
            "userId", userId
        ));
    }
}