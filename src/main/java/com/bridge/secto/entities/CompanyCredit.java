package com.bridge.secto.entities;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "company_credit", schema = "secto")
public class CompanyCredit extends BaseEntity{

    @OneToOne(mappedBy="companyCredit")
    private Company company;
    
    @OneToMany(mappedBy = "companyCredit")
    private List<CreditTransaction> creditTransactions;

    @Column(precision=19, scale=2, nullable = false)
    private BigDecimal creditAmount;
}
