package com.bridge.secto.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bridge.secto.entities.CreditTransaction;

@Repository
public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, UUID> {
    boolean existsByStripeSessionId(String stripeSessionId);
}
