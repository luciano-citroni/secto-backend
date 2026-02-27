package com.bridge.secto.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Dados para atualização de um usuário da empresa")
public class UserUpdateRequestDto {

    @NotBlank(message = "Nome é obrigatório")
    @Schema(description = "Primeiro nome do usuário", example = "João")
    private String firstName;

    @NotBlank(message = "Sobrenome é obrigatório")
    @Schema(description = "Sobrenome do usuário", example = "Silva")
    private String lastName;

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    @Schema(description = "E-mail do usuário", example = "joao@empresa.com")
    private String email;

    @Schema(description = "Se o usuário deve possuir a role de administrador da empresa", example = "false")
    private Boolean isAdmin;
}
