package com.bridge.secto.entities;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "company", schema = "secto")
@Data
public class Company extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, fetch=FetchType.LAZY)
    private List<ServiceType> serviceTypes;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, fetch=FetchType.LAZY)
    private List<ServiceSubType> serviceSubTypes;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, fetch=FetchType.LAZY)
    private List<Script> scripts;


}
