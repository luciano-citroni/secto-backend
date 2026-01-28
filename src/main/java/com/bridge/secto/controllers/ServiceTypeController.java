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

import com.bridge.secto.dtos.ServiceTypeResponseDto;
import com.bridge.secto.entities.ServiceSubType;
import com.bridge.secto.entities.ServiceType;
import com.bridge.secto.repositories.ServiceSubTypeRepository;
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
    private final ServiceSubTypeRepository serviceSubTypeRepository;
    private final AuthService authService;

    @Operation(summary = "Get All Service Types by Company")
    @ApiResponse(responseCode = "200", description = "Successful operation")
    @GetMapping
    public ResponseEntity<List<ServiceTypeResponseDto>> getServiceTypes() {
        UUID companyId = authService.getCurrentCompanyId();
        List<ServiceTypeResponseDto> dtos = serviceTypeRepository.findByCompanyId(companyId).stream()
            .map(serviceType -> {
                ServiceTypeResponseDto dto = new ServiceTypeResponseDto();
                dto.setId(serviceType.getId());
                dto.setName(serviceType.getName());
                dto.setDescription(serviceType.getDescription());
                dto.setStatus(serviceType.getStatus());
                if (serviceType.getServiceSubType() != null) {
                    dto.setServiceSubTypeId(serviceType.getServiceSubType().getId());
                    dto.setServiceSubTypeName(serviceType.getServiceSubType().getName());
                }
                return dto;
            })
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Get Service Types by Service Sub Type")
    @ApiResponse(responseCode = "200", description = "Successful operation")
    @GetMapping("/byServiceSubType/{serviceSubTypeId}")
    public ResponseEntity<List<ServiceTypeResponseDto>> getServiceTypesByServiceSubType(@PathVariable UUID serviceSubTypeId) {
        // Validate company ownership of sub type
        ServiceSubType subType = serviceSubTypeRepository.findById(serviceSubTypeId)
             .orElseThrow(() -> new RuntimeException("ServiceSubType not found"));
             
        UUID companyId = authService.getCurrentCompanyId();
        if (!subType.getCompany().getId().equals(companyId)) {
             throw new RuntimeException("Unauthorized");
        }

        List<ServiceTypeResponseDto> dtos = serviceTypeRepository.findByServiceSubTypeId(serviceSubTypeId).stream()
            .map(serviceType -> {
                ServiceTypeResponseDto dto = new ServiceTypeResponseDto();
                dto.setId(serviceType.getId());
                dto.setName(serviceType.getName());
                dto.setDescription(serviceType.getDescription());
                dto.setStatus(serviceType.getStatus());
                // Populate SubType info since we have it
                dto.setServiceSubTypeId(subType.getId());
                dto.setServiceSubTypeName(subType.getName());
                return dto;
            })
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Create Service Type")
    @ApiResponse(responseCode = "200", description = "Successful operation")
    @PostMapping("/byServiceSubType/{serviceSubTypeId}")
    public ResponseEntity<ServiceTypeResponseDto> createServiceType(@PathVariable UUID serviceSubTypeId, @RequestBody ServiceType request) {
        ServiceSubType subType = serviceSubTypeRepository.findById(serviceSubTypeId)
             .orElseThrow(() -> new RuntimeException("ServiceSubType not found"));

        UUID companyId = authService.getCurrentCompanyId();
        if (!subType.getCompany().getId().equals(companyId)) {
             throw new RuntimeException("Unauthorized");
        }
        
        ServiceType serviceType = new ServiceType();
        serviceType.setName(request.getName());
        serviceType.setDescription(request.getDescription());
        serviceType.setStatus(request.getStatus() != null ? request.getStatus() : true);
        serviceType.setCompany(subType.getCompany());
        serviceType.setServiceSubType(subType);
        
        serviceTypeRepository.save(serviceType);
        
        ServiceTypeResponseDto dto = new ServiceTypeResponseDto();
        dto.setId(serviceType.getId());
        dto.setName(serviceType.getName());
        dto.setDescription(serviceType.getDescription());
        dto.setStatus(serviceType.getStatus());
        
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Update Service Type")
    @ApiResponse(responseCode = "200", description = "Successful operation")
    @PutMapping("/{id}")
    public ResponseEntity<ServiceTypeResponseDto> updateServiceType(@PathVariable UUID id, @RequestBody ServiceType request) {
        UUID userCompanyId = authService.getCurrentUser()
            .map(AuthService.UserInfo::getCompanyId)
            .orElseThrow(() -> new RuntimeException("User not associated with any company"));

        ServiceType serviceType = serviceTypeRepository.findById(id)
             .orElseThrow(() -> new RuntimeException("ServiceType not found"));

        if (!serviceType.getCompany().getId().equals(userCompanyId)) {
             throw new RuntimeException("Unauthorized");
        }

        serviceType.setName(request.getName());
        serviceType.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            serviceType.setStatus(request.getStatus());
        }
        
        // Optionally allow updating subType if provided, but simpler to skip for now unless requested
        
        serviceTypeRepository.save(serviceType);
        
        ServiceTypeResponseDto dto = new ServiceTypeResponseDto();
        dto.setId(serviceType.getId());
        dto.setName(serviceType.getName());
        dto.setDescription(serviceType.getDescription());
        dto.setStatus(serviceType.getStatus());
        if (serviceType.getServiceSubType() != null) {
            dto.setServiceSubTypeId(serviceType.getServiceSubType().getId());
            dto.setServiceSubTypeName(serviceType.getServiceSubType().getName());
        }
        
        return ResponseEntity.ok(dto);
    }
}