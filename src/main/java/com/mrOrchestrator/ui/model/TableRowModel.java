package com.mrOrchestrator.ui.model;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Модель строки таблицы для отображения прогресса обработки MR
 */
public class TableRowModel {

    /** Константа для пропуска строки */
    public static final String SKIP_OPTION = "Пропустить";

    private final StringProperty searchTerm = new SimpleStringProperty();
    private final StringProperty repositoryUrl = new SimpleStringProperty();
    private final StringProperty selectedBranch = new SimpleStringProperty();
    private final ObservableList<String> availableBranches = FXCollections.observableArrayList();
    private final StringProperty status = new SimpleStringProperty("ожидание");
    private final StringProperty comment = new SimpleStringProperty("");
    private final DoubleProperty progress = new SimpleDoubleProperty(0.0);

    public TableRowModel(String searchTerm) {
        this(searchTerm, "");
    }

    public TableRowModel(String searchTerm, String repositoryUrl) {
        this.searchTerm.set(searchTerm);
        this.repositoryUrl.set(repositoryUrl);
    }

    // --- searchTerm ---

    public StringProperty searchTermProperty() {
        return searchTerm;
    }

    public String getSearchTerm() {
        return searchTerm.get();
    }

    public void setSearchTerm(String value) {
        searchTerm.set(value);
    }

    // --- repositoryUrl ---

    public StringProperty repositoryUrlProperty() {
        return repositoryUrl;
    }

    public String getRepositoryUrl() {
        return repositoryUrl.get();
    }

    public void setRepositoryUrl(String value) {
        repositoryUrl.set(value);
    }

    // --- selectedBranch ---

    public StringProperty selectedBranchProperty() {
        return selectedBranch;
    }

    public String getSelectedBranch() {
        return selectedBranch.get();
    }

    public void setSelectedBranch(String value) {
        selectedBranch.set(value);
    }

    // --- availableBranches ---

    public ObservableList<String> getAvailableBranches() {
        return availableBranches;
    }

    public void setAvailableBranches(java.util.List<String> branches) {
        availableBranches.setAll(branches);
    }

    // --- status ---

    public StringProperty statusProperty() {
        return status;
    }

    public String getStatus() {
        return status.get();
    }

    public void setStatus(String value) {
        status.set(value);
    }

    // --- comment ---

    public StringProperty commentProperty() {
        return comment;
    }

    public String getComment() {
        return comment.get();
    }

    public void setComment(String value) {
        comment.set(value);
    }

    // --- progress ---

    public DoubleProperty progressProperty() {
        return progress;
    }

    public double getProgress() {
        return progress.get();
    }

    public void setProgress(double value) {
        progress.set(value);
    }
}
