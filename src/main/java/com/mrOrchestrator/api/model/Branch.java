package com.mrOrchestrator.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Модель ветки репозитория GitLab
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Branch {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
