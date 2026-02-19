package com.bridge.secto.dtos;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Dados para criação/atualização de cliente")
public class ClientRequestDto {

    @NotBlank(message = "Full name is required")
    @Size(max = 200, message = "Full name must have at most 200 characters")
    @Schema(description = "Client full name", example = "John Smith")
    private String fullName;

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

    @Size(max = 20, message = "Phone must have at most 20 characters")
    @Schema(description = "Client phone number", example = "11999998888")
    private String phone;

    @Schema(description = "Client email", example = "john@example.com")
    @jakarta.validation.constraints.Email(message = "Invalid email format")
    private String email;

    @Schema(description = "Status ativo do cliente", example = "true")
    private Boolean status;

    @Schema(description = "Gender", example = "MALE", allowableValues = {"MALE", "FEMALE", "OTHER"})
    private String gender;
}