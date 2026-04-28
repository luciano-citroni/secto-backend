package com.bridge.secto.repositories;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bridge.secto.dtos.AnalysisResultSummaryDto;
import com.bridge.secto.entities.AnalysisResult;

@Repository
public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, UUID> {
    List<AnalysisResult> findByCompanyId(UUID companyId);
    List<AnalysisResult> findByCompanyIdAndClientId(UUID companyId, UUID clientId);
    long countByCompanyIdAndCreatedAtBetween(UUID companyId, Instant start, Instant end);

    @Query("SELECT new com.bridge.secto.dtos.AnalysisResultSummaryDto(" +
            "ar.id, ar.createdAt, c.id, c.fullName, c.cpf, " +
            "ar.audioFilename, ar.audioUrl, ar.approved, ar.creditsUsed, " +
            "ar.executedBy, s.name, st.name, sst.name) " +
            "FROM AnalysisResult ar " +
            "LEFT JOIN ar.client c " +
            "LEFT JOIN ar.script s " +
            "LEFT JOIN s.serviceType st " +
            "LEFT JOIN st.serviceSubType sst " +
            "WHERE ar.company.id = :companyId " +
            "ORDER BY ar.createdAt DESC")
    List<AnalysisResultSummaryDto> findSummaryByCompanyId(@Param("companyId") UUID companyId);

    @Query("SELECT new com.bridge.secto.dtos.AnalysisResultSummaryDto(" +
            "ar.id, ar.createdAt, c.id, c.fullName, c.cpf, " +
            "ar.audioFilename, ar.audioUrl, ar.approved, ar.creditsUsed, " +
            "ar.executedBy, s.name, st.name, sst.name) " +
            "FROM AnalysisResult ar " +
            "LEFT JOIN ar.client c " +
            "LEFT JOIN ar.script s " +
            "LEFT JOIN s.serviceType st " +
            "LEFT JOIN st.serviceSubType sst " +
            "WHERE ar.company.id = :companyId AND ar.client.id = :clientId " +
            "ORDER BY ar.createdAt DESC")
    List<AnalysisResultSummaryDto> findSummaryByCompanyIdAndClientId(
            @Param("companyId") UUID companyId, @Param("clientId") UUID clientId);
}
