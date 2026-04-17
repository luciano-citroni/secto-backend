package com.bridge.secto.dtos;

import java.time.LocalDate;
import java.util.UUID;

import lombok.Data;

@Data
public class ClientResponseDto {
    private UUID id;
    private String fullName;
    private LocalDate birthDate;
    private String cpf;
    private String rg;
    private String address;
    private String phone;
    private String email;
    private Boolean status;
    private String gender;
    private String representativeName;
    private String representativeCpf;
    private UUID companyId;
}