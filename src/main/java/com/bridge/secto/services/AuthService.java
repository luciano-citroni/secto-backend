package com.bridge.secto.services;

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

    /**
     * Obtém a company atual baseada no contexto de autenticação
     * Funciona tanto para usuários logados quanto para client credentials
     */
    public Optional<Company> getCurrentCompany() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            log.debug("JWT claims: {}", jwt.getClaims());
            
            // Verificar se é client credentials (service account)
            if (isServiceAccount(jwt)) {
                String clientId = jwt.getClaimAsString("client_id");
                if (clientId == null) {
                    // Fallback: tentar extrair do azp (authorized party)
                    clientId = jwt.getClaimAsString("azp");
                }
                
                if (clientId != null) {
                    log.debug("Buscando company pelo client_id: {}", clientId);
                    return companyRepository.findByClientId(clientId);
                }
            } else {
                // É um usuário normal, usar companyId do token
                // Tenta tanto "companyId" (camelCase) quanto "company_id" (underscore)
                String companyIdString = jwt.getClaimAsString("companyId");
                if (companyIdString == null) {
                    companyIdString = jwt.getClaimAsString("company_id");
                }
                log.debug("Company ID do token: {}", companyIdString);
                
                if (companyIdString != null) {
                    try {
                        UUID companyId = UUID.fromString(companyIdString);
                        return companyRepository.findById(companyId);
                    } catch (Exception e) {
                        log.error("Erro ao converter companyId: {}", companyIdString, e);
                    }
                }

                // Fallback: buscar company pelo ownerId (admin) ou keycloak_id do usuário
                String keycloakId = jwt.getSubject();
                log.debug("Tentando buscar company pelo ownerId (keycloakId={})", keycloakId);
                if (keycloakId != null) {
                    try {
                        Optional<Company> byOwner = companyRepository.findByOwnerId(UUID.fromString(keycloakId));
                        if (byOwner.isPresent()) {
                            return byOwner;
                        }
                    } catch (Exception e) {
                        log.warn("Erro ao buscar company por ownerId {}: {}", keycloakId, e.getMessage());
                    }
                }
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Verifica se o JWT é de um service account (client credentials)
     */
    private boolean isServiceAccount(Jwt jwt) {
        // Service accounts no Keycloak têm:
        // 1. preferred_username que começa com "service-account-"
        // 2. não têm email
        String preferredUsername = jwt.getClaimAsString("preferred_username");
        String email = jwt.getClaimAsString("email");
        
        return preferredUsername != null && 
               preferredUsername.startsWith("service-account-") && 
               email == null;
    }

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
     * @deprecated Use getCurrentCompany() que suporta tanto usuários quanto client credentials
     */
    @Deprecated
    public Optional<Company> getCurrentUserCompany() {
        return getCurrentCompany();
    }

    /**
     * Obtém o ID da company atual (usuário ou client credentials)
     */
    public UUID getCurrentCompanyId() {
        return getCurrentCompany()
            .map(Company::getId)
            .orElseThrow(() -> new RuntimeException("Nenhuma empresa associada ao contexto atual"));
    }


    public String getCurrentUserKeycloakId() {
        return getCurrentUser()
            .map(UserInfo::getKeycloakId)
            .orElse(null);
    }


    public boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return false;
        }

        // Verificar realm roles
        Object realmAccessObj = jwt.getClaim("realm_access");
        if (realmAccessObj instanceof java.util.Map<?, ?> realmAccess) {
            Object rolesObj = realmAccess.get("roles");
            if (rolesObj instanceof java.util.List<?> roles) {
                for (Object r : roles) {
                    if (role.equals(r)) {
                        return true;
                    }
                }
            }
        }

        // Verificar client roles em qualquer client
        Object resourceAccessObj = jwt.getClaim("resource_access");
        if (resourceAccessObj instanceof java.util.Map<?, ?> resources) {
            for (Object resource : resources.values()) {
                if (resource instanceof java.util.Map<?, ?> client) {
                    Object rolesObj = client.get("roles");
                    if (rolesObj instanceof java.util.List<?> roles) {
                        for (Object r : roles) {
                            if (role.equals(r)) {
                                return true;
                            }
                        }
                    }
                }
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