package com.bridge.secto.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    /**
     * Criar client no Keycloak para acesso via client credentials
     */
    public Map<String, String> createClientCredentials(String clientId, String companyName) {
        try {
            String adminToken = getAdminToken();
            
            // Gerar client secret
            String clientSecret = UUID.randomUUID().toString();
            
            // Payload para criar client
            Map<String, Object> clientPayload = new HashMap<>();
            clientPayload.put("clientId", clientId);
            clientPayload.put("name", "Client para " + companyName);
            clientPayload.put("description", "Client credentials para acesso à API da empresa " + companyName);
            clientPayload.put("enabled", true);
            clientPayload.put("clientAuthenticatorType", "client-secret");
            clientPayload.put("secret", clientSecret);
            clientPayload.put("standardFlowEnabled", false);
            clientPayload.put("implicitFlowEnabled", false);
            clientPayload.put("directAccessGrantsEnabled", false);
            clientPayload.put("serviceAccountsEnabled", true);
            clientPayload.put("authorizationServicesEnabled", false);
            clientPayload.put("publicClient", false);
            clientPayload.put("protocol", "openid-connect");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(adminToken);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(clientPayload, headers);
            
            String createClientUrl = String.format("%s/admin/realms/%s/clients", getNormalizedBaseUrl(), realm);
            log.info("Creating client at URL: {}", createClientUrl);

            ResponseEntity<String> response = restTemplate.postForEntity(createClientUrl, request, String.class);
            
            if (response.getStatusCode() == HttpStatus.CREATED) {
                log.info("Client criado com sucesso: {} para empresa: {}", clientId, companyName);
                return Map.of(
                    "clientId", clientId,
                    "clientSecret", clientSecret
                );
            }
            
            throw new RuntimeException("Falha ao criar client: " + response.getStatusCode());
            
        } catch (Exception e) {
            log.error("Erro ao criar client no Keycloak: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao criar client: " + e.getMessage());
        }
    }

    /**
     * Regenerar client secret de um client existente
     */
    public Map<String, String> regenerateClientSecret(String clientId, String companyName) {
        try {
            String adminToken = getAdminToken();
            
            // 1. Buscar o client no Keycloak
            String getClientsUrl = String.format("%s/admin/realms/%s/clients?clientId=%s", 
                getNormalizedBaseUrl(), realm, clientId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            
            HttpEntity<Void> getRequest = new HttpEntity<>(headers);
            ResponseEntity<Object[]> getResponse = restTemplate.exchange(
                getClientsUrl, 
                org.springframework.http.HttpMethod.GET, 
                getRequest, 
                Object[].class
            );
            
            if (getResponse.getBody() == null || getResponse.getBody().length == 0) {
                throw new RuntimeException("Client não encontrado: " + clientId);
            }
            
            // Extrair o ID interno do client
            @SuppressWarnings("unchecked")
            Map<String, Object> clientData = (Map<String, Object>) getResponse.getBody()[0];
            String internalClientId = (String) clientData.get("id");
            
            // 2. Gerar novo client secret
            String newClientSecret = UUID.randomUUID().toString();
            
            // 3. Atualizar o client com o novo secret
            String updateClientUrl = String.format("%s/admin/realms/%s/clients/%s", 
                getNormalizedBaseUrl(), realm, internalClientId);
            
            Map<String, Object> updatePayload = new HashMap<>();
            updatePayload.put("secret", newClientSecret);
            
            HttpHeaders updateHeaders = new HttpHeaders();
            updateHeaders.setContentType(MediaType.APPLICATION_JSON);
            updateHeaders.setBearerAuth(adminToken);
            
            HttpEntity<Map<String, Object>> updateRequest = new HttpEntity<>(updatePayload, updateHeaders);
            
            ResponseEntity<String> updateResponse = restTemplate.exchange(
                updateClientUrl,
                org.springframework.http.HttpMethod.PUT,
                updateRequest,
                String.class
            );
            
            if (updateResponse.getStatusCode().is2xxSuccessful()) {
                log.info("Client secret regenerado com sucesso para: {} (empresa: {})", clientId, companyName);
                return Map.of(
                    "clientId", clientId,
                    "clientSecret", newClientSecret
                );
            }
            
            throw new RuntimeException("Falha ao regenerar client secret: " + updateResponse.getStatusCode());
            
        } catch (Exception e) {
            log.error("Erro ao regenerar client secret no Keycloak: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao regenerar client secret: " + e.getMessage());
        }
    }

    /**
     * Buscar usuários de uma empresa específica no Keycloak
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getCompanyUsers(UUID companyId) {
        try {
            String adminToken = getAdminToken();
            
            // Buscar usuários no Keycloak
            String getUsersUrl = String.format("%s/admin/realms/%s/users?max=1000", 
                getNormalizedBaseUrl(), realm);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            
            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<List> response = restTemplate.exchange(
                getUsersUrl, 
                org.springframework.http.HttpMethod.GET, 
                request, 
                List.class
            );
            
            if (response.getBody() == null) {
                return List.of();
            }
            
            // Filtrar usuários pela company_id
            List<Map<String, Object>> allUsers = (List<Map<String, Object>>) response.getBody();
            List<Map<String, Object>> companyUsers = new ArrayList<>();
            
            for (Map<String, Object> user : allUsers) {
                Map<String, Object> attributes = (Map<String, Object>) user.get("attributes");
                if (attributes != null && attributes.containsKey("company_id")) {
                    Object companyIdAttr = attributes.get("company_id");
                    String userCompanyId = null;
                    
                    // company_id pode vir como String ou List<String>
                    if (companyIdAttr instanceof List) {
                        List<String> companyIdList = (List<String>) companyIdAttr;
                        if (!companyIdList.isEmpty()) {
                            userCompanyId = companyIdList.get(0);
                        }
                    } else if (companyIdAttr instanceof String) {
                        userCompanyId = (String) companyIdAttr;
                    }
                    
                    if (companyId.toString().equals(userCompanyId)) {
                        companyUsers.add(user);
                    }
                }
            }
            
            log.info("Encontrados {} usuários para a empresa: {}", companyUsers.size(), companyId);
            return companyUsers;
            
        } catch (Exception e) {
            log.error("Erro ao buscar usuários da empresa no Keycloak: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao buscar usuários da empresa: " + e.getMessage());
        }
    }
}