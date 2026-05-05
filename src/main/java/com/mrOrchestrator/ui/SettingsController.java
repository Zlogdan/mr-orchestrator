package com.mrOrchestrator.ui;

import com.mrOrchestrator.config.AppConfig;
import com.mrOrchestrator.config.ConfigLoader;
import com.mrOrchestrator.util.AppLogger;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Контроллер окна настроек приложения
 */
public class SettingsController {

    private static final AppLogger logger = AppLogger.getInstance();

    // GitLab
    @FXML private TextField     baseUrlField;
    @FXML private PasswordField tokenField;
    @FXML private CheckBox      disableSslCheckBox;
    @FXML private TextField     trustStorePathField;
    @FXML private PasswordField trustStorePasswordField;
    @FXML private TextField     trustStoreTypeField;

    // Выполнение
    @FXML private TextField threadCountField;
    @FXML private CheckBox  dryRunCheckBox;
    @FXML private TextField maxCommitsField;
    @FXML private CheckBox  approveOnlyCheckBox;
    @FXML private TextField stagingMergePatternField;

    private AppConfig config;
    private Stage stage;
    private Runnable onSaveCallback;

    /** Вызывается MainController'ом перед показом окна. */
    public void setConfig(AppConfig config) {
        this.config = config;
        populateFields();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    private void populateFields() {
        AppConfig.GitLabConfig gl = config.getGitlab();
        baseUrlField.setText(nullToEmpty(gl.getBaseUrl()));
        tokenField.setText(nullToEmpty(gl.getToken()));
        disableSslCheckBox.setSelected(gl.isDisableSslVerification());
        trustStorePathField.setText(nullToEmpty(gl.getTrustStorePath()));
        trustStorePasswordField.setText(nullToEmpty(gl.getTrustStorePassword()));
        trustStoreTypeField.setText(nullToEmpty(gl.getTrustStoreType()));

        AppConfig.ExecutionConfig ex = config.getExecution();
        threadCountField.setText(String.valueOf(ex.getThreadCount()));
        dryRunCheckBox.setSelected(ex.isDryRun());
        maxCommitsField.setText(String.valueOf(ex.getMaxCommitsWarning()));
        approveOnlyCheckBox.setSelected(ex.isApproveOnly());
        stagingMergePatternField.setText(nullToEmpty(ex.getStagingMergePattern()));
    }

    @FXML
    public void onSave() {
        try {
            int threadCount = Integer.parseInt(threadCountField.getText().trim());
            int maxCommits  = Integer.parseInt(maxCommitsField.getText().trim());

            AppConfig.GitLabConfig gl = config.getGitlab();
            gl.setBaseUrl(baseUrlField.getText().trim());
            gl.setToken(tokenField.getText().trim());
            gl.setDisableSslVerification(disableSslCheckBox.isSelected());
            gl.setTrustStorePath(trustStorePathField.getText().trim());
            gl.setTrustStorePassword(trustStorePasswordField.getText());
            gl.setTrustStoreType(trustStoreTypeField.getText().trim());

            AppConfig.ExecutionConfig ex = config.getExecution();
            ex.setThreadCount(threadCount);
            ex.setDryRun(dryRunCheckBox.isSelected());
            ex.setMaxCommitsWarning(maxCommits);
            ex.setApproveOnly(approveOnlyCheckBox.isSelected());
            ex.setStagingMergePattern(stagingMergePatternField.getText().trim());

            ConfigLoader.save(config);
            logger.info("Настройки сохранены в config.yaml");

            if (onSaveCallback != null) {
                onSaveCallback.run();
            }
            stage.close();

        } catch (NumberFormatException e) {
            showError("Ошибка ввода",
                    "Поля «Количество потоков» и «Макс. коммитов» должны быть целыми числами.");
        } catch (Exception e) {
            logger.error("Ошибка сохранения настроек", e);
            showError("Ошибка", "Не удалось сохранить настройки:\n" + e.getMessage());
        }
    }

    @FXML
    public void onCancel() {
        stage.close();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
