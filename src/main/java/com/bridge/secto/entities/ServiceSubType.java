package com.bridge.secto.entities;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "service_sub_type", schema = "secto")
@Data
@EqualsAndHashCode(callSuper = false)
public class ServiceSubType extends BaseEntity{

    @Column(nullable = false, unique = true)
    private String name;

    @Column()
    private String description;

    @ManyToOne
    @JoinColumn(name="service_type_id", nullable = false)
    private ServiceType serviceType;

    @OneToMany(mappedBy = "serviceSubType", cascade=CascadeType.ALL)
    private List<Script> scripts;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="company_id", nullable = false)    
    private Company company;

}
