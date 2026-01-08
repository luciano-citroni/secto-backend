package com.bridge.secto.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(name = "client_name")
    private String clientName;

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

    private Boolean approved;
}
