package com.mrOrchestrator.ui;

import com.mrOrchestrator.api.GitLabApiClient;
import com.mrOrchestrator.api.model.Branch;
import com.mrOrchestrator.config.AppConfig;
import com.mrOrchestrator.config.ConfigLoader;
import com.mrOrchestrator.service.ProcessingService;
import com.mrOrchestrator.ui.model.TableRowModel;
import com.mrOrchestrator.util.AppLogger;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Контроллер главного окна приложения
 */
public class MainController {

    private static final AppLogger logger = AppLogger.getInstance();

    // --- FXML-поля ---

    @FXML
    private TextField repoUrlField;

    @FXML
    private ComboBox<String> targetBranchCombo;

    @FXML
    private TextField branchNamesField;

    @FXML
    private CheckBox dryRunCheckBox;

    @FXML
    private TableView<TableRowModel> tableView;

    @FXML
    private TableColumn<TableRowModel, String> searchTermColumn;

    @FXML
    private TableColumn<TableRowModel, String> sourceBranchColumn;

    @FXML
    private TableColumn<TableRowModel, String> statusColumn;

    @FXML
    private TableColumn<TableRowModel, String> commentColumn;

    @FXML
    private TableColumn<TableRowModel, Double> progressColumn;

    @FXML
    private TextArea logArea;

    @FXML
    private Button runButton;

    // --- Состояние ---

    private AppConfig config;
    private GitLabApiClient apiClient;

    /**
     * Инициализация: загрузка конфигурации и настройка колонок таблицы
     */
    @FXML
    public void initialize() {
        try {
            config = ConfigLoader.load();
            apiClient = new GitLabApiClient(
                    config.getGitlab().getBaseUrl(),
                    config.getGitlab().getToken());
            logger.setUiCallback(this::appendLog);
            dryRunCheckBox.setSelected(config.getExecution().isDryRun());
            logger.info("Конфигурация загружена успешно");
        } catch (Exception e) {
            logger.error("Ошибка загрузки конфигурации", e);
            showError("Ошибка конфигурации", "Не удалось загрузить config.yaml:\n" + e.getMessage());
        }

        setupTableColumns();

        targetBranchCombo.setEditable(true);
    }

    /**
     * Настроить колонки таблицы
     */
    private void setupTableColumns() {
        // Исходное значение (поисковый термин)
        searchTermColumn.setCellValueFactory(data -> data.getValue().searchTermProperty());

        // Исходная ветка — ComboBox с доступными ветками
        sourceBranchColumn.setCellValueFactory(data -> data.getValue().selectedBranchProperty());
        sourceBranchColumn.setCellFactory(col -> new ComboBoxTableCell());
        sourceBranchColumn.setEditable(true);

        // Статус
        statusColumn.setCellValueFactory(data -> data.getValue().statusProperty());
        statusColumn.setCellFactory(col -> new StatusTableCell());

        // Комментарий
        commentColumn.setCellValueFactory(data -> data.getValue().commentProperty());
        commentColumn.setCellFactory(TextFieldTableCell.forTableColumn());

        // Прогресс
        progressColumn.setCellValueFactory(data -> data.getValue().progressProperty().asObject());
        progressColumn.setCellFactory(col -> new ProgressBarTableCell());

        tableView.setEditable(true);
    }

    /**
     * Обновить список веток для выпадающего списка целевой ветки
     */
    @FXML
    public void onRefreshBranches() {
        String repoUrl = repoUrlField.getText().trim();
        if (repoUrl.isEmpty()) {
            showError("Ошибка", "Введите URL репозитория");
            return;
        }

        new Thread(() -> {
            try {
                String projectId = apiClient.extractProjectId(repoUrl);
                List<Branch> branches = apiClient.getBranches(projectId);
                List<String> names = branches.stream()
                        .map(Branch::getName)
                        .sorted()
                        .collect(Collectors.toList());
                Platform.runLater(() -> {
                    targetBranchCombo.setItems(FXCollections.observableArrayList(names));
                    logger.info("Загружено веток: " + names.size());
                });
            } catch (Exception e) {
                logger.error("Ошибка загрузки веток", e);
                Platform.runLater(() -> showError("Ошибка", "Не удалось загрузить ветки:\n" + e.getMessage()));
            }
        }, "refresh-branches").start();
    }

    /**
     * Парсинг поля с именами веток и заполнение таблицы
     */
    @FXML
    public void onParse() {
        String repoUrl = repoUrlField.getText().trim();
        String input = branchNamesField.getText().trim();

        if (repoUrl.isEmpty()) {
            showError("Ошибка", "Введите URL репозитория");
            return;
        }
        if (input.isEmpty()) {
            showError("Ошибка", "Введите частичные имена веток");
            return;
        }

        List<String> searchTerms = Arrays.stream(input.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        tableView.getItems().clear();

        new Thread(() -> {
            String projectId = apiClient.extractProjectId(repoUrl);

            for (String term : searchTerms) {
                TableRowModel row = new TableRowModel(term);
                Platform.runLater(() -> tableView.getItems().add(row));

                try {
                    List<Branch> found = apiClient.searchBranches(projectId, term);
                    List<String> branchNames = found.stream()
                            .map(Branch::getName)
                            .collect(Collectors.toList());

                    // Добавить вариант "Пропустить" последним
                    branchNames.add(TableRowModel.SKIP_OPTION);

                    Platform.runLater(() -> {
                        row.setAvailableBranches(branchNames);
                        // Список всегда содержит SKIP_OPTION последним.
                        // Если найдена хотя бы одна реальная ветка (size > 1), выбираем первую;
                        // иначе выбираем SKIP_OPTION.
                        if (branchNames.size() > 1) {
                            row.setSelectedBranch(branchNames.get(0));
                        } else {
                            row.setSelectedBranch(TableRowModel.SKIP_OPTION);
                        }
                    });

                    logger.info("Для «" + term + "» найдено веток: " + (branchNames.size() - 1));
                } catch (Exception e) {
                    logger.error("Ошибка поиска веток для «" + term + "»", e);
                    Platform.runLater(() -> {
                        List<String> opts = new ArrayList<>();
                        opts.add(TableRowModel.SKIP_OPTION);
                        row.setAvailableBranches(opts);
                        row.setSelectedBranch(TableRowModel.SKIP_OPTION);
                        row.setComment("Ошибка: " + e.getMessage());
                    });
                }
            }
        }, "parse-branches").start();
    }

    /**
     * Запустить обработку всех строк
     */
    @FXML
    public void onRun() {
        if (tableView.getItems().isEmpty()) {
            showError("Ошибка", "Таблица пуста. Сначала выполните парсинг.");
            return;
        }

        String repoUrl = repoUrlField.getText().trim();
        String targetBranch = targetBranchCombo.getValue();

        if (repoUrl.isEmpty()) {
            showError("Ошибка", "Введите URL репозитория");
            return;
        }
        if (targetBranch == null || targetBranch.trim().isEmpty()) {
            showError("Ошибка", "Выберите целевую ветку");
            return;
        }

        // Применить состояние dry-run из чекбокса
        config.getExecution().setDryRun(dryRunCheckBox.isSelected());

        runButton.setDisable(true);

        List<TableRowModel> rows = new ArrayList<>(tableView.getItems());
        ProcessingService processingService = new ProcessingService(config, apiClient);

        new Thread(() -> {
            processingService.processAllRows(rows, repoUrl, targetBranch, this::appendLog);
            Platform.runLater(() -> runButton.setDisable(false));
        }, "processing-starter").start();
    }

    /**
     * Очистить таблицу
     */
    @FXML
    public void onClearTable() {
        tableView.getItems().clear();
    }

    /**
     * Очистить лог
     */
    @FXML
    public void onClearLog() {
        logArea.clear();
    }

    /**
     * Добавить строку в лог на UI-потоке
     */
    public void appendLog(String message) {
        Platform.runLater(() -> {
            logArea.appendText(message + "\n");
        });
    }

    /**
     * Показать диалог с ошибкой
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // =============================================
    // Внутренние классы-ячейки таблицы
    // =============================================

    /**
     * Ячейка таблицы с выпадающим списком веток
     */
    private static class ComboBoxTableCell extends TableCell<TableRowModel, String> {

        private final ComboBox<String> comboBox = new ComboBox<>();

        public ComboBoxTableCell() {
            comboBox.setMaxWidth(Double.MAX_VALUE);
            comboBox.setOnAction(e -> {
                TableRowModel row = getTableRow().getItem();
                if (row != null) {
                    row.setSelectedBranch(comboBox.getValue());
                    commitEdit(comboBox.getValue());
                }
            });
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            TableRowModel row = getTableRow().getItem();
            comboBox.setItems(row.getAvailableBranches());
            String selected = row.getSelectedBranch();
            if (selected != null) {
                comboBox.setValue(selected);
            }
            setGraphic(comboBox);
            setText(null);
        }
    }

    /**
     * Ячейка с прогресс-баром
     */
    private static class ProgressBarTableCell extends TableCell<TableRowModel, Double> {

        private final ProgressBar progressBar = new ProgressBar(0);

        public ProgressBarTableCell() {
            progressBar.setMaxWidth(Double.MAX_VALUE);
        }

        @Override
        protected void updateItem(Double item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }
            progressBar.setProgress(item);
            setGraphic(progressBar);
            setText(null);
        }
    }

    /**
     * Ячейка статуса с цветовым выделением
     */
    private static class StatusTableCell extends TableCell<TableRowModel, String> {

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            getStyleClass().removeAll("status-success", "status-error", "status-skipped", "status-running");
            if (empty || item == null) {
                setText(null);
                return;
            }
            setText(item);
            switch (item) {
                case "выполнено" -> getStyleClass().add("status-success");
                case "ошибка" -> getStyleClass().add("status-error");
                case "пропущено" -> getStyleClass().add("status-skipped");
                case "выполнение" -> getStyleClass().add("status-running");
            }
        }
    }
}
