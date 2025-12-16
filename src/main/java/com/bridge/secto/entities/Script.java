package com.bridge.secto.entities;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "script", schema = "secto")
@Data
@EqualsAndHashCode(callSuper = false)
public class Script extends BaseEntity{

    @Column(nullable = false, unique = true)
    private String name;

    @Column()
    private Boolean status;
    
    @OneToMany(mappedBy = "script", cascade = CascadeType.ALL)
    private List<ScriptItem> scriptItems;
    
    @ManyToOne
    @JoinColumn(name = "service_sub_type_id", nullable = false)
    private ServiceSubType serviceSubType;
}
