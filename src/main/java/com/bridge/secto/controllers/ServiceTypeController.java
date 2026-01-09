package com.bridge.secto.controllers;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bridge.secto.dtos.ServiceTypeResponseDto;
import com.bridge.secto.entities.Company;
import com.bridge.secto.entities.ServiceType;
import com.bridge.secto.repositories.CompanyRepository;
import com.bridge.secto.repositories.ServiceTypeRepository;
import com.bridge.secto.services.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/service-types")
@RequiredArgsConstructor
public class ServiceTypeController {

    private final ServiceTypeRepository serviceTypeRepository;
    private final CompanyRepository companyRepository;
    private final AuthService authService;

    @Operation(summary = "Get Service Types by Company")
    @ApiResponse(responseCode = "200", description = "Successful operation")
    @GetMapping
    public ResponseEntity<List<ServiceTypeResponseDto>> getServiceTypesByCompany() {
        UUID companyId = authService.getCurrentUser()
            .map(AuthService.UserInfo::getCompanyId)
            .orElseThrow(() -> new RuntimeException("User not associated with any company"));

        List<ServiceTypeResponseDto> dtos = serviceTypeRepository.findByCompanyId(companyId).stream()
            .map(serviceType -> {
                ServiceTypeResponseDto dto = new ServiceTypeResponseDto();
                dto.setId(serviceType.getId());
                dto.setName(serviceType.getName());
                dto.setDescription(serviceType.getDescription());
                return dto;
            })
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Create Service Type")
    @ApiResponse(responseCode = "200", description = "Successful operation")
    @PostMapping
    public ResponseEntity<ServiceTypeResponseDto> createServiceType(@RequestBody ServiceType request) {
        UUID companyId = authService.getCurrentUser()
            .map(AuthService.UserInfo::getCompanyId)
            .orElseThrow(() -> new RuntimeException("User not associated with any company"));

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new RuntimeException("Company not found with id: " + companyId));
        
        ServiceType serviceType = new ServiceType();
        serviceType.setName(request.getName());
        serviceType.setDescription(request.getDescription());
        serviceType.setCompany(company);
        
        serviceTypeRepository.save(serviceType);
        
        ServiceTypeResponseDto dto = new ServiceTypeResponseDto();
        dto.setId(serviceType.getId());
        dto.setName(serviceType.getName());
        dto.setDescription(serviceType.getDescription());
        
        return ResponseEntity.ok(dto);
    }
}