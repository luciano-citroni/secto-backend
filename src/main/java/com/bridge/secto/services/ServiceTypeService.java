package com.bridge.secto.services;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.bridge.secto.repositories.ServiceTypeRepository;

@Service
public class ServiceTypeService {

    private final ServiceTypeRepository serviceTypeRepository;

    public ServiceTypeService(ServiceTypeRepository serviceTypeRepository) {
        this.serviceTypeRepository = serviceTypeRepository;
    }
    
    private List<ServiceTypeResponseDto> getServiceTypesByCompany(UUID companyId) {
        return this.serviceTypeRepository.findByCompanyId(companyId).stream()
            .map(serviceType -> {
                ServiceTypeResponseDto dto = new ServiceTypeResponseDto();
                dto.setId(serviceType.getId());
                dto.setName(serviceType.getName());
                dto.setDescription(serviceType.getDescription());
                return dto;
            })
            .toList();
    }
}
