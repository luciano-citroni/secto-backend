package com.bridge.secto.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bridge.secto.entities.ServiceSubType;

@Repository
public interface ServiceSubTypeRepository extends JpaRepository<ServiceSubType, UUID> {
    java.util.List<ServiceSubType> findByCompanyId(UUID companyId);
}
