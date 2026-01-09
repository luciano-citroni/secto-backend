package com.bridge.secto.controllers;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bridge.secto.dtos.ServiceSubTypeResponseDto;
import com.bridge.secto.entities.ServiceSubType;
import com.bridge.secto.entities.ServiceType;
import com.bridge.secto.repositories.ServiceSubTypeRepository;
import com.bridge.secto.repositories.ServiceTypeRepository;
import com.bridge.secto.services.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/service-sub-types")
@RequiredArgsConstructor
public class ServiceSubTypeController {

    private final ServiceSubTypeRepository serviceSubTypeRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final AuthService authService;

    @Operation(summary = "Get Service Sub Types by Service Type")
    @ApiResponse(responseCode = "200", description = "Successful operation")
    @GetMapping("/byServiceType/{serviceTypeId}")
    public ResponseEntity<List<ServiceSubTypeResponseDto>> getServiceSubTypesByServiceType(@PathVariable UUID serviceTypeId) {
        UUID userCompanyId = authService.getCurrentUser()
            .map(AuthService.UserInfo::getCompanyId)
            .orElseThrow(() -> new RuntimeException("User not associated with any company"));

        ServiceType serviceType = serviceTypeRepository.findById(serviceTypeId)
            .orElseThrow(() -> new RuntimeException("ServiceType not found"));

        if (!serviceType.getCompany().getId().equals(userCompanyId)) {
            throw new RuntimeException("Unauthorized: ServiceType does not belong to your company");
        }

        List<ServiceSubTypeResponseDto> dtos = serviceSubTypeRepository.findByServiceTypeId(serviceTypeId).stream()
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
    @PostMapping("/byServiceType/{serviceTypeId}")
    public ResponseEntity<ServiceSubTypeResponseDto> createServiceSubType(@PathVariable UUID serviceTypeId, @RequestBody ServiceSubType request) {
        UUID userCompanyId = authService.getCurrentUser()
            .map(AuthService.UserInfo::getCompanyId)
            .orElseThrow(() -> new RuntimeException("User not associated with any company"));

        ServiceType serviceType = serviceTypeRepository.findById(serviceTypeId)
            .orElseThrow(() -> new RuntimeException("ServiceType not found with id: " + serviceTypeId));
        
        if (!serviceType.getCompany().getId().equals(userCompanyId)) {
            throw new RuntimeException("Unauthorized: ServiceType does not belong to your company");
        }

        ServiceSubType serviceSubType = new ServiceSubType();
        serviceSubType.setName(request.getName());
        serviceSubType.setDescription(request.getDescription());
        serviceSubType.setServiceType(serviceType);
        serviceSubType.setCompany(serviceType.getCompany());
        
        serviceSubTypeRepository.save(serviceSubType);
        
        ServiceSubTypeResponseDto dto = new ServiceSubTypeResponseDto();
        dto.setId(serviceSubType.getId());
        dto.setName(serviceSubType.getName());
        dto.setDescription(serviceSubType.getDescription());
        
        return ResponseEntity.ok(dto);
    }
}
