package com.bridge.secto.repositories;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bridge.secto.entities.CreditTransaction;

@Repository
public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, UUID> {
    boolean existsByStripeSessionId(String stripeSessionId);

    /**
     * Find valid (non-expired, has remaining) credit lots ordered by expiration (FIFO).
     */
    List<CreditTransaction> findByCompanyCreditIdAndAmountGreaterThanAndRemainingAmountGreaterThanAndExpiresAtAfterOrderByExpiresAtAsc(
            UUID companyCreditId, BigDecimal amountThreshold, BigDecimal remainingThreshold, Instant now);

    /**
     * Sum remaining credits from valid (non-expired) lots.
     */
    @Query("SELECT COALESCE(SUM(ct.remainingAmount), 0) FROM CreditTransaction ct " +
           "WHERE ct.companyCredit.id = :ccId AND ct.amount > 0 AND ct.remainingAmount > 0 AND ct.expiresAt > :now")
    BigDecimal sumValidRemainingCredits(@Param("ccId") UUID companyCreditId, @Param("now") Instant now);

    /**
     * Find all positive (purchase) lots for a company credit, ordered by expiration date.
     */
    List<CreditTransaction> findByCompanyCreditIdAndAmountGreaterThanOrderByExpiresAtAsc(
            UUID companyCreditId, BigDecimal amountThreshold);
}
