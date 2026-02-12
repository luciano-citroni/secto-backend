package com.bridge.secto.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "analysis_result")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisResult extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @Column(name = "audio_filename")
    private String audioFilename;

    @Column(name = "audio_url")
    private String audioUrl;

    @Column(columnDefinition = "TEXT")
    private String transcription;

    @Column(name = "script_json", columnDefinition = "TEXT")
    private String scriptJson;

    @Column(name = "ai_output_json", columnDefinition = "TEXT")
    private String aiOutputJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "script_id")
    private Script script;

    private Boolean approved;

    @Column(name = "credits_used")
    private Double creditsUsed;

    @Column(name = "executed_by")
    private String executedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
}
