package com.bridge.secto.repositories;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bridge.secto.entities.AnalysisResult;

@Repository
public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, UUID> {
    List<AnalysisResult> findByCompanyId(UUID companyId);
    List<AnalysisResult> findByCompanyIdAndClientId(UUID companyId, UUID clientId);
    long countByCompanyIdAndCreatedAtBetween(UUID companyId, Instant start, Instant end);
}
