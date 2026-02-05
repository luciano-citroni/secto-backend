package com.bridge.secto.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "credit_packages", schema = "secto")
@Data
@EqualsAndHashCode(callSuper = true)
public class CreditPackage extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String identifier;

    @Column(name = "price_in_cents", nullable = false)
    private Long priceInCents;

    @Column(nullable = false)
    private Integer credits;

    @Column(nullable = false)
    private Boolean active = true;
}
