package com.bridge.secto.dtos;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Dados para criação/atualização de cliente")
public class ClientRequestDto {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must have at most 100 characters")
    @Schema(description = "Client name", example = "John")
    private String name;

    @NotBlank(message = "Surname is required")
    @Size(max = 100, message = "Surname must have at most 100 characters")
    @Schema(description = "Client surname", example = "Smith")
    private String surname;

    @Schema(description = "Client birth date", example = "1990-01-01")
    private LocalDate birthDate;

    @Size(max = 11, message = "CPF must have at most 11 characters")
    @Schema(description = "Client CPF (numbers only)", example = "12345678901")
    private String cpf;

    @Size(max = 20, message = "RG must have at most 20 characters")
    @Schema(description = "Client RG", example = "123456789")
    private String rg;

    @Size(max = 255, message = "Address must have at most 255 characters")
    @Schema(description = "Client full address", example = "123 Main St, Downtown")
    private String address;

    @Schema(description = "Status ativo do cliente", example = "true")
    private Boolean status;
}