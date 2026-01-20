package com.bridge.secto.entities;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Table(name = "service_type", schema = "secto")
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(
    name = "ServiceType",
    description = "Representa um tipo de serviço no sistema de análise de voz",
    example = "{\"id\": \"123e4567-e89b-12d3-a456-426614174000\", \"name\": \"Atendimento ao Cliente\", \"description\": \"Serviços relacionados ao atendimento e suporte ao cliente\"}"
)
public class ServiceType extends BaseEntity {

    @Schema(
        description = "Nome único do tipo de serviço",
        example = "Atendimento ao Cliente",
        required = true
    )
    @Column(nullable = false, unique = true)
    private String name;

    @Schema(
        description = "Descrição detalhada do tipo de serviço",
        example = "Serviços relacionados ao atendimento e suporte ao cliente"
    )
    @Column()
    private String description;

    @Column()
    private Boolean status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_sub_type_id", nullable = false)
    private ServiceSubType serviceSubType;

    @OneToMany(mappedBy = "serviceType", cascade=CascadeType.ALL)
    private List<Script> scripts;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="company_id", nullable = false)    
    private Company company;
}
