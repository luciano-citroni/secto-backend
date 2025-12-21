CREATE TABLE analysis_result (
    id UUID PRIMARY KEY,
    client_name TEXT,
    audio_filename TEXT,
    transcription TEXT,
    script_json TEXT,
    ai_output_json TEXT,
    approved BOOLEAN,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
