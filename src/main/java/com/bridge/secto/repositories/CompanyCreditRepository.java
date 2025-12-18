package com.bridge.secto.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bridge.secto.entities.CompanyCredit;

@Repository
public interface CompanyCreditRepository extends JpaRepository<CompanyCredit, UUID> {
    
    CompanyCredit findByCompanyId(@Param("companyId") UUID companyId);
}
