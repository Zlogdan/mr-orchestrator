package com.mrOrchestrator.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Модель пайплайна GitLab
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Pipeline {

    private Long id;
    private String status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Pipeline{id=" + id + ", status=" + status + "}";
    }
}
