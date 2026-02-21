package com.bridge.secto.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Dados para alteração da própria senha pelo usuário autenticado")
public class ChangeOwnPasswordDto {

    @NotBlank(message = "Senha atual é obrigatória")
    @Schema(description = "Senha atual do usuário")
    private String currentPassword;

    @NotBlank(message = "Nova senha é obrigatória")
    @Size(min = 8, message = "Nova senha deve ter no mínimo 8 caracteres")
    @Schema(description = "Nova senha desejada", example = "NovaSenha@123")
    private String newPassword;
}
