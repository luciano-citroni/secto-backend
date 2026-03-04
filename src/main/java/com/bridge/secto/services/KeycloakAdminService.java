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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.bridge.secto.exceptions.BusinessRuleException;

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

    @Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
    private String keycloakClientId;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-secret}")
    private String keycloakClientSecret;

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

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                throw new BusinessRuleException("Usuário com esse email ou nome de usuário já existe");
            }
            log.error("Erro ao criar usuário no Keycloak: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao criar usuário: " + e.getMessage());
        } catch (Exception e) {
            log.error("Erro ao criar usuário no Keycloak: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao criar usuário: " + e.getMessage());
        }
    }

    /**
     * Criar usuário regular (não admin) na empresa.
     * Se isAdmin=true, atribui a role company-admin.
     */
    public String createCompanyUser(String firstName, String lastName, String email,
                                  String username, String password, UUID companyId, boolean isAdmin) {
        try {
            String adminToken = getAdminToken();

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

            String createUserUrl = String.format("%s/admin/realms/%s/users", getNormalizedBaseUrl(), realm);
            log.info("Creating company user at URL: {}", createUserUrl);

            ResponseEntity<String> response = restTemplate.postForEntity(createUserUrl, request, String.class);

            if (response.getStatusCode() == HttpStatus.CREATED) {
                String location = response.getHeaders().getLocation().toString();
                String userId = location.substring(location.lastIndexOf("/") + 1);

                if (isAdmin) {
                    assignCompanyAdminRole(userId, adminToken);
                }

                log.info("Usuário da empresa criado com sucesso: {} (ID: {}, admin: {})", username, userId, isAdmin);
                return userId;
            }

            throw new RuntimeException("Falha ao criar usuário: " + response.getStatusCode());

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                throw new BusinessRuleException("Usuário com esse email ou nome de usuário já existe");
            }
            log.error("Erro ao criar usuário no Keycloak: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao criar usuário: " + e.getMessage());
        } catch (Exception e) {
            log.error("Erro ao criar usuário no Keycloak: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao criar usuário: " + e.getMessage());
        }
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
        log.info("Atribuindo role company-admin ao usuário: {}", userId);

        String baseUrl = getNormalizedBaseUrl();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        // 1. Buscar a representação da role "company-admin" no realm
        String getRoleUrl = String.format("%s/admin/realms/%s/roles/%s", baseUrl, realm, "company-admin");
        try {
            ResponseEntity<Map> roleResponse = restTemplate.exchange(
                    getRoleUrl,
                    org.springframework.http.HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );

            if (roleResponse.getStatusCode() != HttpStatus.OK || roleResponse.getBody() == null) {
                throw new RuntimeException("Role company-admin não encontrada no Keycloak");
            }

            Map<String, Object> roleRepresentation = roleResponse.getBody();

            // 2. Atribuir a role ao usuário
            String assignRoleUrl = String.format("%s/admin/realms/%s/users/%s/role-mappings/realm",
                    baseUrl, realm, userId);

            HttpEntity<List<Map<String, Object>>> assignRequest = new HttpEntity<>(List.of(roleRepresentation), headers);
            restTemplate.postForEntity(assignRoleUrl, assignRequest, String.class);

            log.info("Role company-admin atribuída com sucesso ao usuário: {}", userId);
        } catch (Exception e) {
            log.error("Erro ao atribuir role company-admin ao usuário {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Falha ao atribuir role company-admin: " + e.getMessage());
        }
    }

    /**
     * Remover a role company-admin de um usuário
     */
    public void removeCompanyAdminRole(String userId) {
        log.info("Removendo role company-admin do usuário: {}", userId);

        String adminToken = getAdminToken();
        String baseUrl = getNormalizedBaseUrl();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        String getRoleUrl = String.format("%s/admin/realms/%s/roles/%s", baseUrl, realm, "company-admin");
        try {
            ResponseEntity<Map> roleResponse = restTemplate.exchange(
                    getRoleUrl,
                    org.springframework.http.HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );

            if (roleResponse.getStatusCode() != HttpStatus.OK || roleResponse.getBody() == null) {
                throw new RuntimeException("Role company-admin não encontrada no Keycloak");
            }

            Map<String, Object> roleRepresentation = roleResponse.getBody();

            String deleteRoleUrl = String.format("%s/admin/realms/%s/users/%s/role-mappings/realm",
                    baseUrl, realm, userId);

            HttpEntity<List<Map<String, Object>>> deleteRequest = new HttpEntity<>(List.of(roleRepresentation), headers);
            restTemplate.exchange(deleteRoleUrl, org.springframework.http.HttpMethod.DELETE, deleteRequest, String.class);

            log.info("Role company-admin removida com sucesso do usuário: {}", userId);
        } catch (Exception e) {
            log.error("Erro ao remover role company-admin do usuário {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Falha ao remover role company-admin: " + e.getMessage());
        }
    }

    /**
     * Atribuir ou remover a role company-admin com base no valor de isAdmin
     */
    public void setCompanyAdminRole(String userId, boolean isAdmin) {
        boolean currentlyAdmin = hasCompanyAdminRole(userId);
        if (isAdmin && !currentlyAdmin) {
            String adminToken = getAdminToken();
            assignCompanyAdminRole(userId, adminToken);
        } else if (!isAdmin && currentlyAdmin) {
            removeCompanyAdminRole(userId);
        }
    }

    /**
     * Verificar se um usuário possui a role company-admin
     */
    @SuppressWarnings("unchecked")
    public boolean hasCompanyAdminRole(String userId) {
        try {
            String adminToken = getAdminToken();
            String baseUrl = getNormalizedBaseUrl();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);

            String rolesUrl = String.format("%s/admin/realms/%s/users/%s/role-mappings/realm",
                    baseUrl, realm, userId);

            ResponseEntity<List> response = restTemplate.exchange(
                    rolesUrl,
                    org.springframework.http.HttpMethod.GET,
                    new HttpEntity<>(headers),
                    List.class
            );

            if (response.getBody() == null) return false;

            List<Map<String, Object>> roles = (List<Map<String, Object>>) response.getBody();
            return roles.stream().anyMatch(r -> "company-admin".equals(r.get("name")));
        } catch (Exception e) {
            log.error("Erro ao verificar role company-admin do usuário {}: {}", userId, e.getMessage());
            return false;
        }
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

            // Reuse admin token for role checks
            String baseUrl = getNormalizedBaseUrl();
            HttpHeaders roleHeaders = new HttpHeaders();
            roleHeaders.setBearerAuth(adminToken);
            
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
                        // Check if user has company-admin role
                        boolean isAdmin = false;
                        try {
                            String userId = (String) user.get("id");
                            String rolesUrl = String.format("%s/admin/realms/%s/users/%s/role-mappings/realm",
                                    baseUrl, realm, userId);
                            ResponseEntity<List> rolesResponse = restTemplate.exchange(
                                    rolesUrl,
                                    org.springframework.http.HttpMethod.GET,
                                    new HttpEntity<>(roleHeaders),
                                    List.class
                            );
                            if (rolesResponse.getBody() != null) {
                                List<Map<String, Object>> roles = (List<Map<String, Object>>) rolesResponse.getBody();
                                isAdmin = roles.stream().anyMatch(r -> "company-admin".equals(r.get("name")));
                            }
                        } catch (Exception e) {
                            log.warn("Não foi possível verificar roles do usuário {}: {}", user.get("id"), e.getMessage());
                        }
                        
                        // Add isAdmin field to user data
                        Map<String, Object> enrichedUser = new HashMap<>(user);
                        enrichedUser.put("isAdmin", isAdmin);
                        companyUsers.add(enrichedUser);
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

    /**
     * Desativar um usuário no Keycloak (enabled = false)
     */
    public void disableUser(String userId) {
        try {
            String adminToken = getAdminToken();

            Map<String, Object> payload = new HashMap<>();
            payload.put("enabled", false);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(adminToken);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            String updateUserUrl = String.format("%s/admin/realms/%s/users/%s",
                    getNormalizedBaseUrl(), realm, userId);

            ResponseEntity<String> response = restTemplate.exchange(
                    updateUserUrl, org.springframework.http.HttpMethod.PUT, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Falha ao desativar usuário: " + response.getStatusCode());
            }

            log.info("Usuário desativado com sucesso: {}", userId);
        } catch (Exception e) {
            log.error("Erro ao desativar usuário no Keycloak: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao desativar usuário: " + e.getMessage());
        }
    }

    /**
     * Ativar um usuário no Keycloak (enabled = true)
     */
    public void enableUser(String userId) {
        try {
            String adminToken = getAdminToken();

            Map<String, Object> payload = new HashMap<>();
            payload.put("enabled", true);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(adminToken);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            String updateUserUrl = String.format("%s/admin/realms/%s/users/%s",
                    getNormalizedBaseUrl(), realm, userId);

            ResponseEntity<String> response = restTemplate.exchange(
                    updateUserUrl, org.springframework.http.HttpMethod.PUT, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Falha ao ativar usuário: " + response.getStatusCode());
            }

            log.info("Usuário ativado com sucesso: {}", userId);
        } catch (Exception e) {
            log.error("Erro ao ativar usuário no Keycloak: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao ativar usuário: " + e.getMessage());
        }
    }

    /**
     * Atualizar dados de um usuário no Keycloak (firstName, lastName, email)
     */
    public void updateUser(String userId, String firstName, String lastName, String email) {
        try {
            String adminToken = getAdminToken();

            Map<String, Object> payload = new HashMap<>();
            payload.put("firstName", firstName);
            payload.put("lastName", lastName);
            payload.put("email", email);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(adminToken);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            String updateUserUrl = String.format("%s/admin/realms/%s/users/%s",
                    getNormalizedBaseUrl(), realm, userId);

            ResponseEntity<String> response = restTemplate.exchange(
                    updateUserUrl, org.springframework.http.HttpMethod.PUT, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Falha ao atualizar usuário: " + response.getStatusCode());
            }

            log.info("Usuário atualizado com sucesso: {}", userId);
        } catch (Exception e) {
            log.error("Erro ao atualizar usuário no Keycloak: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao atualizar usuário: " + e.getMessage());
        }
    }

    /**
     * Resetar a senha de um usuário no Keycloak
     */
    public void resetUserPassword(String userId, String newPassword, boolean temporary) {
        try {
            String adminToken = getAdminToken();

            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "password");
            payload.put("value", newPassword);
            payload.put("temporary", temporary);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(adminToken);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            String resetPasswordUrl = String.format("%s/admin/realms/%s/users/%s/reset-password",
                    getNormalizedBaseUrl(), realm, userId);

            restTemplate.exchange(
                    resetPasswordUrl, org.springframework.http.HttpMethod.PUT, request, String.class);

            log.info("Senha resetada com sucesso para o usuário: {}", userId);
        } catch (Exception e) {
            log.error("Erro ao resetar senha do usuário no Keycloak: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao resetar senha: " + e.getMessage());
        }
    }

    /**
     * Validar senha atual de um usuário tentando obter token
     */
    public boolean validateUserPassword(String username, String currentPassword) {
        try {
            String tokenUrl = getNormalizedBaseUrl() + "/realms/" + realm + "/protocol/openid-connect/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("client_id", keycloakClientId);
            map.add("client_secret", keycloakClientSecret);
            map.add("username", username);
            map.add("password", currentPassword);
            map.add("grant_type", "password");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.debug("Validação de senha falhou para o usuário: {}", username);
            return false;
        }
    }

    /**
     * Verificar se um usuário pertence a uma empresa específica
     */
    @SuppressWarnings("unchecked")
    public boolean isUserInCompany(String userId, UUID companyId) {
        try {
            String adminToken = getAdminToken();

            String getUserUrl = String.format("%s/admin/realms/%s/users/%s",
                    getNormalizedBaseUrl(), realm, userId);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);

            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    getUserUrl, org.springframework.http.HttpMethod.GET, request, Map.class);

            if (response.getBody() == null) {
                return false;
            }

            Map<String, Object> user = response.getBody();
            Map<String, Object> attributes = (Map<String, Object>) user.get("attributes");
            if (attributes == null || !attributes.containsKey("company_id")) {
                return false;
            }

            Object companyIdAttr = attributes.get("company_id");
            String userCompanyId = null;

            if (companyIdAttr instanceof List) {
                List<String> companyIdList = (List<String>) companyIdAttr;
                if (!companyIdList.isEmpty()) {
                    userCompanyId = companyIdList.get(0);
                }
            } else if (companyIdAttr instanceof String) {
                userCompanyId = (String) companyIdAttr;
            }

            return companyId.toString().equals(userCompanyId);
        } catch (Exception e) {
            log.error("Erro ao verificar se usuário pertence à empresa: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Configurar protocol mappers para incluir clientId no token
     */
    private void configureClientProtocolMappers(String internalClientId, String clientId, String adminToken) {
        try {
            // Protocol mapper para incluir o clientId no token
            Map<String, Object> protocolMapper = new HashMap<>();
            protocolMapper.put("name", "client-id-mapper");
            protocolMapper.put("protocol", "openid-connect");
            protocolMapper.put("protocolMapper", "oidc-hardcoded-claim-mapper");
            
            Map<String, Object> config = new HashMap<>();
            config.put("claim.name", "clientId");
            config.put("claim.value", clientId);
            config.put("jsonType.label", "String");
            config.put("id.token.claim", "true");
            config.put("access.token.claim", "true");
            
            protocolMapper.put("config", config);
            
            String protocolMapperUrl = String.format("%s/admin/realms/%s/clients/%s/protocol-mappers/models", 
                getNormalizedBaseUrl(), realm, internalClientId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(adminToken);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(protocolMapper, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(protocolMapperUrl, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Protocol mapper configurado com sucesso para client: {}", clientId);
            } else {
                log.warn("Falha ao configurar protocol mapper para client: {} - Status: {}", clientId, response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Erro ao configurar protocol mapper para client {}: {}", clientId, e.getMessage(), e);
            // Não falhar a criação do client por causa do protocol mapper
        }
    }
}