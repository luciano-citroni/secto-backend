package com.bridge.secto.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Table(name = "script_item", schema = "secto")
@EqualsAndHashCode(callSuper = false)
public class ScriptItem extends BaseEntity {

    @Column(nullable = false)
    private String question;
    
    @Column()
    private String answer;
    
    @ManyToOne
    @JoinColumn(name = "script_id", nullable = false)
    private Script script;
}
