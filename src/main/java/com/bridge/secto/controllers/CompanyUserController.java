package com.bridge.secto.controllers;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bridge.secto.dtos.ChangeOwnPasswordDto;
import com.bridge.secto.dtos.PasswordResetRequestDto;
import com.bridge.secto.dtos.UserUpdateRequestDto;
import com.bridge.secto.exceptions.UnauthorizedActionException;
import com.bridge.secto.services.AuthService;
import com.bridge.secto.services.KeycloakAdminService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/company/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "keycloak")
@Tag(name = "Gerenciamento de Usuários da Company", description = "Endpoints para gerenciar usuários da empresa: atualizar, desativar e alterar senha")
public class CompanyUserController {

    private final KeycloakAdminService keycloakAdminService;
    private final AuthService authService;

    @Operation(summary = "Atualizar usuário da empresa",
               description = "Atualiza firstName, lastName e email de um usuário da empresa. Apenas administradores da empresa podem usar este endpoint. O usuário alvo deve pertencer à mesma empresa.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Usuário atualizado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "403", description = "Sem permissão para atualizar este usuário"),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @PutMapping("/{userId}")
    public ResponseEntity<?> updateUser(
            @Parameter(description = "ID do usuário no Keycloak") @PathVariable String userId,
            @Valid @RequestBody UserUpdateRequestDto request) {

        UUID companyId = authService.getCurrentCompanyId();
        String currentUserId = authService.getCurrentUserKeycloakId();
        boolean isSelf = userId.equals(currentUserId);
        boolean isAdmin = authService.isCompanyAdmin();

        // Somente o próprio usuário ou um admin da empresa pode editar
        if (!isSelf && !isAdmin) {
            throw new UnauthorizedActionException("Você só pode editar seu próprio perfil, a menos que seja administrador da empresa");
        }

        // Verificar se o usuário alvo pertence à mesma empresa
        if (!keycloakAdminService.isUserInCompany(userId, companyId)) {
            throw new UnauthorizedActionException("Usuário não pertence à sua empresa");
        }

        keycloakAdminService.updateUser(userId, request.getFirstName(), request.getLastName(), request.getEmail());

        return ResponseEntity.ok(Map.of("message", "Usuário atualizado com sucesso"));
    }

    @Operation(summary = "Desativar usuário da empresa",
               description = "Desativa (enabled=false) um usuário da empresa no Keycloak. O usuário não poderá mais fazer login. Apenas administradores da empresa podem usar este endpoint.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Usuário desativado com sucesso"),
        @ApiResponse(responseCode = "403", description = "Sem permissão para desativar este usuário"),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @PutMapping("/{userId}/disable")
    public ResponseEntity<?> disableUser(
            @Parameter(description = "ID do usuário no Keycloak") @PathVariable String userId) {

        // Apenas admin da empresa pode desativar usuários
        if (!authService.isCompanyAdmin()) {
            throw new UnauthorizedActionException("Apenas administradores da empresa podem desativar usuários");
        }

        UUID companyId = authService.getCurrentCompanyId();

        // Verificar se o usuário alvo pertence à mesma empresa
        if (!keycloakAdminService.isUserInCompany(userId, companyId)) {
            throw new UnauthorizedActionException("Usuário não pertence à sua empresa");
        }

        // Impedir que o admin desative a si mesmo
        String currentUserId = authService.getCurrentUserKeycloakId();
        if (userId.equals(currentUserId)) {
            throw new UnauthorizedActionException("Você não pode desativar sua própria conta");
        }

        keycloakAdminService.disableUser(userId);

        return ResponseEntity.ok(Map.of("message", "Usuário desativado com sucesso"));
    }

    @Operation(summary = "Resetar senha de outro usuário da empresa",
               description = "Permite que o administrador da empresa resete a senha de outro usuário. A senha pode ser temporária (o usuário será obrigado a trocar no próximo login).")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Senha resetada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "403", description = "Sem permissão para resetar a senha deste usuário"),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @PutMapping("/{userId}/password")
    public ResponseEntity<?> resetUserPassword(
            @Parameter(description = "ID do usuário no Keycloak") @PathVariable String userId,
            @Valid @RequestBody PasswordResetRequestDto request) {

        // Apenas admin da empresa pode resetar senha de outros usuários
        if (!authService.isCompanyAdmin()) {
            throw new UnauthorizedActionException("Apenas administradores da empresa podem resetar senha de outros usuários");
        }

        UUID companyId = authService.getCurrentCompanyId();

        // Verificar se o usuário alvo pertence à mesma empresa
        if (!keycloakAdminService.isUserInCompany(userId, companyId)) {
            throw new UnauthorizedActionException("Usuário não pertence à sua empresa");
        }

        keycloakAdminService.resetUserPassword(userId, request.getNewPassword(), request.isTemporary());

        return ResponseEntity.ok(Map.of("message", "Senha resetada com sucesso"));
    }

    @Operation(summary = "Alterar própria senha",
               description = "Permite que o usuário autenticado altere sua própria senha, informando a senha atual para validação e a nova senha desejada.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Senha alterada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos ou senha atual incorreta"),
        @ApiResponse(responseCode = "401", description = "Senha atual incorreta")
    })
    @PutMapping("/me/password")
    public ResponseEntity<?> changeOwnPassword(@Valid @RequestBody ChangeOwnPasswordDto request) {

        AuthService.UserInfo currentUser = authService.getCurrentUser()
                .orElseThrow(() -> new UnauthorizedActionException("Usuário não autenticado"));

        // Validar senha atual
        boolean isValid = keycloakAdminService.validateUserPassword(
                currentUser.getUsername(), request.getCurrentPassword());

        if (!isValid) {
            return ResponseEntity.badRequest().body(Map.of("message", "Senha atual incorreta"));
        }

        keycloakAdminService.resetUserPassword(currentUser.getKeycloakId(), request.getNewPassword(), false);

        return ResponseEntity.ok(Map.of("message", "Senha alterada com sucesso"));
    }
}
