package com.bridge.secto.services;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.bridge.secto.entities.Company;
import com.bridge.secto.repositories.CompanyRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final CompanyRepository companyRepository;

    public Optional<UserInfo> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            try {
                String keycloakId = jwt.getSubject();
                String email = jwt.getClaimAsString("email");
                String name = jwt.getClaimAsString("name");
                String preferredUsername = jwt.getClaimAsString("preferred_username");
                
                String companyIdString = jwt.getClaimAsString("companyId");
                UUID companyId = companyIdString != null ? UUID.fromString(companyIdString) : null;
                
                return Optional.of(UserInfo.builder()
                    .keycloakId(keycloakId)
                    .email(email)
                    .name(name)
                    .username(preferredUsername)
                    .companyId(companyId)
                    .build());
                    
            } catch (Exception e) {
                log.error("Erro ao extrair informações do usuário do JWT", e);
                return Optional.empty();
            }
        }
        
        return Optional.empty();
    }

    /**
     * Obtém a empresa atual do usuário logado
     */
    public Optional<Company> getCurrentUserCompany() {
        return getCurrentUser()
            .filter(user -> user.getCompanyId() != null)
            .flatMap(user -> companyRepository.findById(user.getCompanyId()));
    }


    public String getCurrentUserKeycloakId() {
        return getCurrentUser()
            .map(UserInfo::getKeycloakId)
            .orElse(null);
    }


    public boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            // Verificar roles no realm_access
            List<String> realmRoles = jwt.getClaimAsStringList("realm_access.roles");
            if (realmRoles != null && realmRoles.contains(role)) {
                return true;
            }
            
            // Verificar roles no resource_access para o client específico
            List<String> clientRoles = jwt.getClaimAsStringList("resource_access.secto-client.roles");
            if (clientRoles != null && clientRoles.contains(role)) {
                return true;
            }
        }
        
        return false;
    }

    public boolean isCompanyAdmin() {
        return hasRole("company-admin");
    }

    public boolean isUserInCompany(UUID companyId) {
        return getCurrentUser()
            .map(user -> user.getCompanyId() != null && user.getCompanyId().equals(companyId))
            .orElse(false);
    }

    public boolean canAccessCompany(UUID companyId) {
        return isUserInCompany(companyId);
    }

    @lombok.Data
    @lombok.Builder
    public static class UserInfo {
        private String keycloakId;
        private String email;
        private String name;
        private String username;
        private UUID companyId;
    }
}