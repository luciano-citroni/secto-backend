package com.bridge.secto.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bridge.secto.entities.CompanyCredit;
import com.bridge.secto.entities.CreditTransaction;
import com.bridge.secto.exceptions.BusinessRuleException;
import com.bridge.secto.repositories.CompanyCreditRepository;
import com.bridge.secto.repositories.CreditTransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditService {

    private final CompanyCreditRepository companyCreditRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final AuthService authService;

    /**
     * Calcula créditos baseado na duração exata do áudio.
     * Regra: 1 crédito a cada 60 segundos, com 1 casa decimal.
     * Sempre arredonda a segunda casa decimal para cima.
     * Ex: 90s = 1.5 créditos, 30s = 0.5, 104s = 1.8
     * Mínimo: 0.1 crédito
     */
    public double calculateCreditsForDuration(double durationInSeconds) {
        double credits = Math.ceil(durationInSeconds / 60.0 * 10.0) / 10.0;
        return Math.max(credits, 0.1);
    }

    /**
     * Recalcula o saldo de créditos da empresa considerando apenas lotes não expirados.
     * Atualiza o campo creditAmount da CompanyCredit.
     */
    @Transactional
    public BigDecimal recalculateBalance(CompanyCredit companyCredit) {
        BigDecimal validBalance = creditTransactionRepository.sumValidRemainingCredits(
                companyCredit.getId(), Instant.now());
        companyCredit.setCreditAmount(validBalance);
        companyCreditRepository.save(companyCredit);
        log.debug("Saldo recalculado para company_credit {}: {}", companyCredit.getId(), validBalance);
        return validBalance;
    }

    /**
     * Retorna o saldo de créditos válidos (não expirados) da empresa.
     */
    @Transactional
    public BigDecimal getValidCredits(UUID companyId) {
        CompanyCredit companyCredit = getCompanyCredit(companyId);
        return recalculateBalance(companyCredit);
    }

    /**
     * Verifica se a empresa tem créditos válidos suficientes (recalcula saldo antes)
     */
    @Transactional
    public boolean hasEnoughCredits(UUID companyId, double requiredCredits) {
        BigDecimal validBalance = getValidCredits(companyId);
        return validBalance.compareTo(BigDecimal.valueOf(requiredCredits)) >= 0;
    }

    /**
     * Desconta créditos usando FIFO — consome primeiro dos lotes que expiram mais cedo.
     * Cria uma transação de uso (valor negativo) e atualiza o saldo.
     */
    @Transactional
    public void debitCredits(UUID companyId, double creditsToDebit, String description) {
        debitCredits(companyId, creditsToDebit, description, null);
    }

    @Transactional
    public void debitCredits(UUID companyId, double creditsToDebit, String description, UUID analysisResultId) {
        CompanyCredit companyCredit = getCompanyCredit(companyId);

        // Recalcular saldo com base em lotes válidos
        BigDecimal validBalance = recalculateBalance(companyCredit);
        BigDecimal debitAmount = BigDecimal.valueOf(creditsToDebit);

        if (validBalance.compareTo(debitAmount) < 0) {
            throw new BusinessRuleException("Créditos insuficientes. Saldo atual: " + validBalance + " créditos");
        }

        // FIFO: buscar lotes válidos ordenados por expiração (mais próximo primeiro)
        List<CreditTransaction> validLots = creditTransactionRepository
                .findByCompanyCreditIdAndAmountGreaterThanAndRemainingAmountGreaterThanAndExpiresAtAfterOrderByExpiresAtAsc(
                        companyCredit.getId(), BigDecimal.ZERO, BigDecimal.ZERO, Instant.now());

        BigDecimal remaining = debitAmount;
        for (CreditTransaction lot : validLots) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal lotRemaining = lot.getRemainingAmount();
            if (lotRemaining.compareTo(remaining) >= 0) {
                // Este lote cobre o restante
                lot.setRemainingAmount(lotRemaining.subtract(remaining));
                remaining = BigDecimal.ZERO;
            } else {
                // Consome tudo deste lote e segue para o próximo
                remaining = remaining.subtract(lotRemaining);
                lot.setRemainingAmount(BigDecimal.ZERO);
            }
            creditTransactionRepository.save(lot);
        }

        // Criar transação de débito (valor negativo)
        CreditTransaction transaction = new CreditTransaction();
        transaction.setAmount(debitAmount.negate());
        transaction.setCompanyCredit(companyCredit);
        transaction.setRemainingAmount(BigDecimal.ZERO);
        transaction.setSourceType("USAGE");
        transaction.setAnalysisResultId(analysisResultId);

        // Registrar o usuário responsável pela transação
        try {
            authService.getCurrentUser().ifPresent(user -> {
                transaction.setPurchasedBy(user.getKeycloakId());
                transaction.setPurchasedByName(user.getName() != null ? user.getName() : user.getUsername());
            });
        } catch (Exception e) {
            log.debug("Não foi possível identificar o usuário da transação: {}", e.getMessage());
        }

        creditTransactionRepository.save(transaction);

        // Recalcular saldo após débito
        recalculateBalance(companyCredit);

        log.info("Créditos debitados com sucesso (FIFO). Empresa: {}, Valor: {}, Saldo atual: {}",
                companyId, creditsToDebit, companyCredit.getCreditAmount());
    }

    /**
     * Desconta créditos baseado na duração do áudio e informações da análise
     */
    @Transactional
    public void debitCreditsForAnalysis(String clientName, Double audioDurationInSeconds, UUID analysisResultId) {
        UUID companyId = authService.getCurrentCompanyId();
        
        if (audioDurationInSeconds == null || audioDurationInSeconds <= 0) {
            log.info("Análise sem arquivo de áudio, não há cobrança de créditos");
            return;
        }

        double creditsToDebit = calculateCreditsForDuration(audioDurationInSeconds);
        String description = String.format("Análise de áudio - Cliente: %s (%.1fs)", 
                clientName, audioDurationInSeconds);
        
        debitCredits(companyId, creditsToDebit, description, analysisResultId);
    }

    /**
     * Obtém o ID da empresa atual do contexto de autenticação
     */
    public UUID getCurrentCompanyId() {
        return authService.getCurrentCompanyId();
    }

    /**
     * Obtém crédito da empresa ou lança exceção se não existir
     */
    private CompanyCredit getCompanyCredit(UUID companyId) {
        CompanyCredit companyCredit = companyCreditRepository.findByCompanyId(companyId);
        if (companyCredit == null) {
            throw new BusinessRuleException("Empresa não possui conta de créditos configurada");
        }
        return companyCredit;
    }
}