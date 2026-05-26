package com.mrOrchestrator.ui;

import com.mrOrchestrator.config.AppConfig;
import com.mrOrchestrator.config.ConfigLoader;
import com.mrOrchestrator.util.AppLogger;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Контроллер окна списка репозиториев.
 */
public class RepositoriesController {

    private static final AppLogger logger = AppLogger.getInstance();

    @FXML private TableView<RepositoryRow> repositoriesTable;
    @FXML private TableColumn<RepositoryRow, Boolean> enabledColumn;
    @FXML private TableColumn<RepositoryRow, String> urlColumn;

    private AppConfig config;
    private Stage stage;
    private Runnable onSaveCallback;

    @FXML
    public void initialize() {
        repositoriesTable.setEditable(true);

        enabledColumn.setCellValueFactory(data -> data.getValue().enabledProperty());
        enabledColumn.setCellFactory(CheckBoxTableCell.forTableColumn(enabledColumn));
        enabledColumn.setEditable(true);

        urlColumn.setCellValueFactory(data -> data.getValue().urlProperty());
        urlColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        urlColumn.setOnEditCommit(event -> event.getRowValue().setUrl(event.getNewValue().trim()));
        urlColumn.setEditable(true);
    }

    public void setConfig(AppConfig config) {
        this.config = config;
        populateRows();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    @FXML
    public void onAdd() {
        RepositoryRow row = new RepositoryRow("", true);
        repositoriesTable.getItems().add(row);
        repositoriesTable.getSelectionModel().select(row);
        repositoriesTable.edit(repositoriesTable.getItems().size() - 1, urlColumn);
    }

    @FXML
    public void onRemove() {
        RepositoryRow selected = repositoriesTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            repositoriesTable.getItems().remove(selected);
        }
    }

    @FXML
    public void onSave() {
        try {
            List<AppConfig.RepositoryConfig> repositories = readRepositories();
            if (repositories == null) {
                return;
            }

            config.setRepositories(repositories);
            ConfigLoader.save(config);
            logger.info("Список репозиториев сохранен в config.yaml");

            if (onSaveCallback != null) {
                onSaveCallback.run();
            }
            stage.close();
        } catch (Exception e) {
            logger.error("Ошибка сохранения списка репозиториев", e);
            showError("Ошибка", "Не удалось сохранить список репозиториев:\n" + e.getMessage());
        }
    }

    @FXML
    public void onCancel() {
        stage.close();
    }

    private void populateRows() {
        repositoriesTable.getItems().clear();
        for (AppConfig.RepositoryConfig repository : config.getRepositories()) {
            if (repository == null) {
                continue;
            }
            repositoriesTable.getItems().add(new RepositoryRow(
                    repository.getUrl() == null ? "" : repository.getUrl(),
                    repository.isEnabled()
            ));
        }
    }

    private List<AppConfig.RepositoryConfig> readRepositories() {
        List<AppConfig.RepositoryConfig> repositories = new ArrayList<>();
        Set<String> uniqueUrls = new LinkedHashSet<>();

        for (RepositoryRow row : repositoriesTable.getItems()) {
            String url = row.getUrl() == null ? "" : row.getUrl().trim();
            if (url.isEmpty()) {
                showError("Ошибка ввода", "URL репозитория не может быть пустым.");
                return null;
            }
            if (!uniqueUrls.add(url)) {
                showError("Ошибка ввода", "Репозиторий уже есть в списке:\n" + url);
                return null;
            }
            repositories.add(new AppConfig.RepositoryConfig(url, row.isEnabled()));
        }

        return repositories;
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static class RepositoryRow {
        private final StringProperty url = new SimpleStringProperty();
        private final BooleanProperty enabled = new SimpleBooleanProperty(true);

        public RepositoryRow(String url, boolean enabled) {
            this.url.set(url);
            this.enabled.set(enabled);
        }

        public StringProperty urlProperty() {
            return url;
        }

        public String getUrl() {
            return url.get();
        }

        public void setUrl(String value) {
            url.set(value);
        }

        public BooleanProperty enabledProperty() {
            return enabled;
        }

        public boolean isEnabled() {
            return enabled.get();
        }
    }
}
