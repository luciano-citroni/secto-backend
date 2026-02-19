package com.bridge.secto.entities;

import java.time.LocalDate;

import com.bridge.secto.enums.Gender;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "client", schema = "secto")
@Data
@EqualsAndHashCode(callSuper = false)
public class Client extends BaseEntity {

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(unique = true)
    private String cpf;

    @Column
    private String rg;

    @Column
    private String address;

    @Column
    private String phone;

    @Column
    private String email;

    @Column
    private Boolean status = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 20)
    private Gender gender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
}