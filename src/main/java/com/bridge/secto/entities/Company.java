package com.bridge.secto.entities;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "company", schema = "secto")
@Data
public class Company extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "client_id")
    private String clientId;

    @Column(name = "client_secret")
    private String clientSecret;

    @OneToOne(cascade = CascadeType.ALL, fetch=FetchType.LAZY)
    @JoinColumn(name = "company_credit_id", referencedColumnName = "id")
    private CompanyCredit companyCredit;

    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;
    
    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, fetch=FetchType.LAZY)
    private List<ServiceType> serviceTypes;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, fetch=FetchType.LAZY)
    private List<ServiceSubType> serviceSubTypes;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, fetch=FetchType.LAZY)
    private List<Script> scripts;


}
