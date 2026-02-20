package com.bridge.secto.services;

import java.math.BigDecimal;
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
     * Verifica se a empresa tem créditos suficientes
     */
    public boolean hasEnoughCredits(UUID companyId, double requiredCredits) {
        CompanyCredit companyCredit = getCompanyCredit(companyId);
        return companyCredit.getCreditAmount().compareTo(BigDecimal.valueOf(requiredCredits)) >= 0;
    }

    /**
     * Desconta créditos da empresa e cria transação
     */
    @Transactional
    public void debitCredits(UUID companyId, double creditsToDebit, String description) {
        CompanyCredit companyCredit = getCompanyCredit(companyId);
        
        BigDecimal currentAmount = companyCredit.getCreditAmount();
        BigDecimal debitAmount = BigDecimal.valueOf(creditsToDebit);
        
        if (currentAmount.compareTo(debitAmount) < 0) {
            throw new BusinessRuleException("Créditos insuficientes. Saldo atual: " + currentAmount + " créditos");
        }

        // Criar transação de débito (valor negativo)
        CreditTransaction transaction = new CreditTransaction();
        transaction.setAmount(debitAmount.negate());
        transaction.setCompanyCredit(companyCredit);
        creditTransactionRepository.save(transaction);

        // Atualizar saldo da empresa
        companyCredit.setCreditAmount(currentAmount.subtract(debitAmount));
        companyCreditRepository.save(companyCredit);

        log.info("Créditos debitados com sucesso. Empresa: {}, Valor: {}, Saldo atual: {}", 
                companyId, creditsToDebit, companyCredit.getCreditAmount());
    }

    /**
     * Desconta créditos baseado na duração do áudio e informações da análise
     */
    @Transactional
    public void debitCreditsForAnalysis(String clientName, Double audioDurationInSeconds) {
        UUID companyId = authService.getCurrentCompanyId();
        
        if (audioDurationInSeconds == null || audioDurationInSeconds <= 0) {
            log.info("Análise sem arquivo de áudio, não há cobrança de créditos");
            return;
        }

        double creditsToDebit = calculateCreditsForDuration(audioDurationInSeconds);
        String description = String.format("Análise de áudio - Cliente: %s (%.1fs)", 
                clientName, audioDurationInSeconds);
        
        debitCredits(companyId, creditsToDebit, description);
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