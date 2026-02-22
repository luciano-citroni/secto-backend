package com.bridge.secto.entities;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "credit_transaction", schema = "secto")
public class CreditTransaction extends BaseEntity {

    @JsonIgnore
    @ManyToOne()
    @JoinColumn(name = "company_credit_id", nullable = false)
    private CompanyCredit companyCredit;

    @Column(precision=19, scale=2, nullable = false)
    private BigDecimal amount;

    @Column(name = "stripe_session_id")
    private String stripeSessionId;

    @Column(name = "purchased_by")
    private String purchasedBy;

    @Column(name = "purchased_by_name")
    private String purchasedByName;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "remaining_amount", precision = 19, scale = 2)
    private BigDecimal remainingAmount;

    @Column(name = "source_type", length = 20)
    private String sourceType;

    @Column(name = "interval_type", length = 20)
    private String intervalType;
}
