package com.bridge.secto.dtos;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompanyRegistrationResponse {
    private UUID companyId;
    private String companyName;
    private UUID ownerId;
    private String adminEmail;
    private String message;
}