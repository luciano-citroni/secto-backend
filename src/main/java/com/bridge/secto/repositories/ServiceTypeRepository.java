package com.bridge.secto.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bridge.secto.entities.ServiceType;

@Repository
public interface ServiceTypeRepository extends JpaRepository<ServiceType, UUID> {
    List<ServiceType> findByCompanyId(UUID companyId);
    List<ServiceType> findByServiceSubTypeId(UUID serviceSubTypeId);
}
