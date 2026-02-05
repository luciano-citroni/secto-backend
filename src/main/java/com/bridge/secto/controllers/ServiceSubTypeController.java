package com.bridge.secto.controllers;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bridge.secto.dtos.ServiceSubTypeResponseDto;
import com.bridge.secto.entities.Company;
import com.bridge.secto.entities.ServiceSubType;
import com.bridge.secto.exceptions.ResourceNotFoundException;
import com.bridge.secto.exceptions.UnauthorizedActionException;
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
            .orElseThrow(() -> new UnauthorizedActionException("User not associated with any company"));

        List<ServiceSubTypeResponseDto> dtos = serviceSubTypeRepository.findByCompanyId(userCompanyId).stream()
            .map(subType -> {
                ServiceSubTypeResponseDto dto = new ServiceSubTypeResponseDto();
                dto.setId(subType.getId());
                dto.setName(subType.getName());
                dto.setDescription(subType.getDescription());
                dto.setStatus(subType.getStatus());
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
            .orElseThrow(() -> new UnauthorizedActionException("User not associated with any company"));

        Company company = companyRepository.findById(userCompanyId)
             .orElseThrow(() -> new ResourceNotFoundException("Company not found")); // Assuming authService returns valid ID, but good to check or load proxy

        ServiceSubType serviceSubType = new ServiceSubType();
        serviceSubType.setName(request.getName());
        serviceSubType.setDescription(request.getDescription());
        serviceSubType.setStatus(request.getStatus() != null ? request.getStatus() : true);
        // serviceSubType.setServiceType(serviceType); // REMOVED
        serviceSubType.setCompany(company);
        
        serviceSubTypeRepository.save(serviceSubType);
        
        ServiceSubTypeResponseDto dto = new ServiceSubTypeResponseDto();
        dto.setId(serviceSubType.getId());
        dto.setName(serviceSubType.getName());
        dto.setDescription(serviceSubType.getDescription());
        dto.setStatus(serviceSubType.getStatus());
        
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Update Service Sub Type")
    @ApiResponse(responseCode = "200", description = "Successful operation")
    @PutMapping("/{id}")
    public ResponseEntity<ServiceSubTypeResponseDto> updateServiceSubType(@PathVariable UUID id, @RequestBody ServiceSubType request) {
        UUID userCompanyId = authService.getCurrentUser()
            .map(AuthService.UserInfo::getCompanyId)
            .orElseThrow(() -> new UnauthorizedActionException("User not associated with any company"));

        ServiceSubType serviceSubType = serviceSubTypeRepository.findById(id)
             .orElseThrow(() -> new ResourceNotFoundException("ServiceSubType not found"));

        if (!serviceSubType.getCompany().getId().equals(userCompanyId)) {
             throw new UnauthorizedActionException("Unauthorized");
        }

        serviceSubType.setName(request.getName());
        serviceSubType.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            serviceSubType.setStatus(request.getStatus());
        }
        
        serviceSubTypeRepository.save(serviceSubType);
        
        ServiceSubTypeResponseDto dto = new ServiceSubTypeResponseDto();
        dto.setId(serviceSubType.getId());
        dto.setName(serviceSubType.getName());
        dto.setDescription(serviceSubType.getDescription());
        dto.setStatus(serviceSubType.getStatus());
        
        return ResponseEntity.ok(dto);
    }
}
