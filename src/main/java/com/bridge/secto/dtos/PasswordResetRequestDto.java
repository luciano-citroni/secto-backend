package com.bridge.secto.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Dados para reset de senha de um usuário (admin reseta senha de outro usuário)")
public class PasswordResetRequestDto {

    @NotBlank(message = "Nova senha é obrigatória")
    @Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
    @Schema(description = "Nova senha do usuário", example = "NovaSenha@123")
    private String newPassword;

    @Schema(description = "Se true, o usuário será obrigado a trocar a senha no próximo login", example = "true")
    private boolean temporary = true;
}
