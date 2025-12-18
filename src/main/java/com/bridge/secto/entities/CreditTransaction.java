package com.bridge.secto.entities;

import java.math.BigDecimal;

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
}
