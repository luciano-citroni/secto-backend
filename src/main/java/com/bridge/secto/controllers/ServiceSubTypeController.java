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

import com.bridge.secto.dtos.ServiceSubTypeResponseDto;
import com.bridge.secto.entities.Company;
import com.bridge.secto.entities.ServiceSubType;
import com.bridge.secto.repositories.CompanyRepository;
import com.bridge.secto.repositories.ServiceSubTypeRepository;
import com.bridge.secto.services.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/service-sub-types")
@RequiredArgsConstructor
public class ServiceSubTypeController {

    private final ServiceSubTypeRepository serviceSubTypeRepository;
    private final CompanyRepository companyRepository;
    private final AuthService authService;

    @Operation(summary = "Get Service Sub Types by Company")
    @ApiResponse(responseCode = "200", description = "Successful operation")
    @GetMapping
    public ResponseEntity<List<ServiceSubTypeResponseDto>> getServiceSubTypes() {
        UUID userCompanyId = authService.getCurrentUser()
            .map(AuthService.UserInfo::getCompanyId)
            .orElseThrow(() -> new RuntimeException("User not associated with any company"));

        List<ServiceSubTypeResponseDto> dtos = serviceSubTypeRepository.findByCompanyId(userCompanyId).stream()
            .map(subType -> {
                ServiceSubTypeResponseDto dto = new ServiceSubTypeResponseDto();
                dto.setId(subType.getId());
                dto.setName(subType.getName());
                dto.setDescription(subType.getDescription());
                return dto;
            })
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Create Service Sub Type")
    @ApiResponse(responseCode = "200", description = "Successful operation")
    @PostMapping
    public ResponseEntity<ServiceSubTypeResponseDto> createServiceSubType(@RequestBody ServiceSubType request) {
        UUID userCompanyId = authService.getCurrentUser()
            .map(AuthService.UserInfo::getCompanyId)
            .orElseThrow(() -> new RuntimeException("User not associated with any company"));

        Company company = companyRepository.findById(userCompanyId)
             .orElseThrow(() -> new RuntimeException("Company not found")); // Assuming authService returns valid ID, but good to check or load proxy

        ServiceSubType serviceSubType = new ServiceSubType();
        serviceSubType.setName(request.getName());
        serviceSubType.setDescription(request.getDescription());
        // serviceSubType.setServiceType(serviceType); // REMOVED
        serviceSubType.setCompany(company);
        
        serviceSubTypeRepository.save(serviceSubType);
        
        ServiceSubTypeResponseDto dto = new ServiceSubTypeResponseDto();
        dto.setId(serviceSubType.getId());
        dto.setName(serviceSubType.getName());
        dto.setDescription(serviceSubType.getDescription());
        
        return ResponseEntity.ok(dto);
    }
}
