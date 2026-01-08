package com.bridge.secto.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompanyRegistrationRequest {
    
    // Dados da empresa
    @NotBlank(message = "Nome da empresa é obrigatório")
    private String companyName;
    
    // Dados do admin inicial
    @NotBlank(message = "Nome é obrigatório")
    private String adminFirstName;
    
    @NotBlank(message = "Sobrenome é obrigatório") 
    private String adminLastName;
    
    @Email(message = "Email deve ser válido")
    @NotBlank(message = "Email é obrigatório")
    private String adminEmail;
    
    @NotBlank(message = "Username é obrigatório")
    private String adminUsername;
    
    @NotBlank(message = "Senha é obrigatória")
    private String adminPassword;
}