package com.bridge.secto.dtos;

import java.time.LocalDate;
import java.util.UUID;

import lombok.Data;

@Data
public class ClientResponseDto {
    private UUID id;
    private String name;
    private String surname;
    private LocalDate birthDate;
    private String cpf;
    private String rg;
    private String address;
    private Boolean status;
    private UUID companyId;
}