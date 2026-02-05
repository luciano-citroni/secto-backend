package com.bridge.secto.dtos;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Standard error response for the API")
public class ApiError {
    @Schema(description = "Timestamp when the error occurred", example = "2023-10-27T10:00:00")
    private LocalDateTime timestamp;

    @Schema(description = "HTTP Status code", example = "400")
    private int status;

    @Schema(description = "Short error description", example = "Bad Request")
    private String error;

    @Schema(description = "Detailed error message", example = "Validation failed for object='user'. Error count: 1")
    private String message;

    @Schema(description = "Request path that caused the error", example = "/api/users")
    private String path;

    @Schema(description = "List of specific validation errors or details", example = "[\"Email cannot be empty\", \"Password is too short\"]")
    private List<String> details;

    public ApiError() {
    }

    public ApiError(int status, String error, String message, String path) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    public ApiError(int status, String error, String message, String path, List<String> details) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.details = details;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<String> getDetails() {
        return details;
    }

    public void setDetails(List<String> details) {
        this.details = details;
    }
}
