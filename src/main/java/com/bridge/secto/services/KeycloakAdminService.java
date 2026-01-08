package com.bridge.secto.services;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakAdminService {

    @Value("${keycloak.admin-url}")
    private String keycloakAdminUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.admin-username}")
    private String adminUsername;

    @Value("${keycloak.admin-password}")
    private String adminPassword;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Helper para normalizar a base URL do Keycloak (sem /admin e sem barra final)
     */
    private String getNormalizedBaseUrl() {
        String url = keycloakAdminUrl;
        if (url == null) return "";
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (url.endsWith("/admin")) {
            url = url.substring(0, url.length() - 6);
        }
        return url;
    }

    /**
     * Criar usuário no Keycloak e associar à empresa
     */
    public String createUser(String firstName, String lastName, String email, 
                           String username, String password, UUID companyId) {
        try {
            // 1. Obter token admin
            String adminToken = getAdminToken();
            
            // 2. Criar usuário
            Map<String, Object> userPayload = Map.of(
                "firstName", firstName,
                "lastName", lastName,
                "email", email,
                "username", username,
                "enabled", true,
                "credentials", new Object[]{Map.of(
                    "type", "password",
                    "value", password,
                    "temporary", false
                )},
                "attributes", Map.of(
                    "company_id", companyId.toString()
                )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(adminToken);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(userPayload, headers);
            
            // Constroi URL: BASE_URL + /admin/realms/{realm}/users
            String createUserUrl = String.format("%s/admin/realms/%s/users", getNormalizedBaseUrl(), realm);
            log.info("Creating user at URL: {}", createUserUrl);

            ResponseEntity<String> response = restTemplate.postForEntity(createUserUrl, request, String.class);
            
            if (response.getStatusCode() == HttpStatus.CREATED) {
                // Extrair user ID do Location header
                String location = response.getHeaders().getLocation().toString();
                String userId = location.substring(location.lastIndexOf("/") + 1);
                
                // 3. Atribuir role de admin da empresa
                assignCompanyAdminRole(userId, adminToken);
                
                log.info("Usuário criado com sucesso: {} (ID: {})", username, userId);
                return userId;
            }
            
            throw new RuntimeException("Falha ao criar usuário: " + response.getStatusCode());
            
        } catch (Exception e) {
            log.error("Erro ao criar usuário no Keycloak: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao criar usuário: " + e.getMessage());
        }
    }

    /**
     * Criar usuário regular (não admin) na empresa
     */
    public String createCompanyUser(String firstName, String lastName, String email,
                                  String username, String password, UUID companyId) {
        
        String userId = createUser(firstName, lastName, email, username, password, companyId);
        
        // Atribuir role de usuário regular
        try {
            String adminToken = getAdminToken();
            assignCompanyUserRole(userId, adminToken);
        } catch (Exception e) {
            log.warn("Erro ao atribuir role de usuário: {}", e.getMessage());
        }
        
        return userId;
    }

    private String getAdminToken() {
        String tokenUrl = getNormalizedBaseUrl() + "/realms/master/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", "admin-cli");
        map.add("username", adminUsername);
        map.add("password", adminPassword);
        map.add("grant_type", "password");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);
            if (response.getBody() != null && response.getBody().containsKey("access_token")) {
                return (String) response.getBody().get("access_token");
            }
            throw new RuntimeException("Token não encontrado na resposta");
        } catch (Exception e) {
            log.error("Erro ao obter admin token: {}", e.getMessage());
            throw new RuntimeException("Falha na autenticação com Keycloak: " + e.getMessage());
        }
    }

    private void assignCompanyAdminRole(String userId, String token) {
        // Implementar atribuição de role company-admin
        log.info("Atribuindo role company-admin ao usuário: {}", userId);
    }

    private void assignCompanyUserRole(String userId, String token) {
        // Implementar atribuição de role company-user
        log.info("Atribuindo role company-user ao usuário: {}", userId);
    }
}