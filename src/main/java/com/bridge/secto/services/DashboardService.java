package com.bridge.secto.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bridge.secto.dtos.CreditTransactionResponseDTO;
import com.bridge.secto.dtos.DashboardResponseDto;
import com.bridge.secto.entities.CompanyCredit;
import com.bridge.secto.entities.CreditTransaction;
import com.bridge.secto.repositories.AnalysisResultRepository;
import com.bridge.secto.repositories.ClientRepository;
import com.bridge.secto.repositories.CompanyCreditRepository;
import com.bridge.secto.repositories.CreditTransactionRepository;
import com.bridge.secto.repositories.ScriptRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final AuthService authService;
    private final KeycloakAdminService keycloakAdminService;
    private final CompanyCreditRepository companyCreditRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final ClientRepository clientRepository;
    private final ScriptRepository scriptRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final CreditService creditService;

    @Transactional(readOnly = true)
    public DashboardResponseDto getDashboardData(int month, int year) {
        UUID companyId = authService.getCurrentCompanyId();

        YearMonth yearMonth = YearMonth.of(year, month);
        Instant startOfMonth = yearMonth.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endOfMonth = yearMonth.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        // Credit transactions in the period
        CompanyCredit companyCredit = companyCreditRepository.findByCompanyId(companyId);

        List<CreditTransactionResponseDTO> creditTransactions = List.of();
        BigDecimal totalUsed = BigDecimal.ZERO;
        BigDecimal totalPurchased = BigDecimal.ZERO;
        BigDecimal currentBalance = BigDecimal.ZERO;

        if (companyCredit != null) {
            List<CreditTransaction> transactions = creditTransactionRepository
                    .findByCompanyCreditIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                            companyCredit.getId(), startOfMonth, endOfMonth);

            creditTransactions = transactions.stream()
                    .map(this::mapToResponseDto)
                    .collect(Collectors.toList());

            totalUsed = creditTransactionRepository.sumUsageInPeriod(
                    companyCredit.getId(), startOfMonth, endOfMonth);

            totalPurchased = creditTransactionRepository.sumPurchasesInPeriod(
                    companyCredit.getId(), startOfMonth, endOfMonth);

            currentBalance = creditService.getValidCredits(companyId);
        }

        // Total users (from Keycloak)
        long totalUsers;
        try {
            totalUsers = keycloakAdminService.getCompanyUsers(companyId).size();
        } catch (Exception e) {
            log.warn("Não foi possível obter total de usuários do Keycloak: {}", e.getMessage());
            totalUsers = 0;
        }

        // Total clients
        long totalClients = clientRepository.findByCompanyId(companyId).size();

        // Total scripts
        long totalScripts = scriptRepository.findByCompanyId(companyId).size();

        // Total analyses in the period
        long totalAnalyses = analysisResultRepository.countByCompanyIdAndCreatedAtBetween(
                companyId, startOfMonth, endOfMonth);

        return DashboardResponseDto.builder()
                .creditTransactions(creditTransactions)
                .totalCreditsUsed(totalUsed)
                .totalCreditsPurchased(totalPurchased)
                .currentCreditBalance(currentBalance)
                .totalUsers(totalUsers)
                .totalClients(totalClients)
                .totalScripts(totalScripts)
                .totalAnalysesInPeriod(totalAnalyses)
                .month(month)
                .year(year)
                .build();
    }

    private CreditTransactionResponseDTO mapToResponseDto(CreditTransaction ct) {
        CreditTransactionResponseDTO dto = new CreditTransactionResponseDTO();
        dto.setId(ct.getId());
        dto.setAmount(ct.getAmount());
        dto.setStripeSessionId(ct.getStripeSessionId());
        dto.setPurchasedBy(ct.getPurchasedBy());
        dto.setPurchasedByName(ct.getPurchasedByName());
        dto.setCreatedAt(ct.getCreatedAt());
        dto.setExpiresAt(ct.getExpiresAt());
        dto.setRemainingAmount(ct.getRemainingAmount());
        dto.setSourceType(ct.getSourceType());
        dto.setIntervalType(ct.getIntervalType());
        dto.setAnalysisResultId(ct.getAnalysisResultId());
        return dto;
    }
}
