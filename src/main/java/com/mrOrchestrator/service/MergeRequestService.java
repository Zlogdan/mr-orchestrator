package com.mrOrchestrator.service;

import com.mrOrchestrator.api.GitLabApiClient;
import com.mrOrchestrator.api.model.MergeRequest;
import com.mrOrchestrator.ui.model.TableRowModel;
import com.mrOrchestrator.util.AppLogger;
import javafx.application.Platform;

import java.util.List;

/**
 * Сервис обработки одной строки таблицы: поиск/создание MR, одобрение, слияние.
 */
public class MergeRequestService {

    private static final AppLogger logger = AppLogger.getInstance();

    // Максимальное количество попыток повтора для API вызовов
    private static final int MAX_RETRIES = 3;

    // Параметры ожидания одобрения: 10 попыток по 3 секунды
    private static final int APPROVE_POLL_ATTEMPTS = 10;
    private static final long APPROVE_POLL_INTERVAL_MS = 3000;

    // Параметры ожидания слияния: 20 попыток по 5 секунд
    private static final int MERGE_POLL_ATTEMPTS = 20;
    private static final long MERGE_POLL_INTERVAL_MS = 5000;

    private final GitLabApiClient apiClient;

    public MergeRequestService(GitLabApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Обработать одну строку: найти или создать MR, одобрить и влить.
     *
     * @param row          строка таблицы
     * @param projectId    URL-закодированный идентификатор проекта
     * @param targetBranch целевая ветка
     * @param dryRun       режим dry-run (без реальных изменений)
     */
    public void processRow(TableRowModel row, String projectId, String targetBranch, boolean dryRun) {
        String sourceBranch = row.getSelectedBranch();

        // Пропустить строку, если выбран вариант "Пропустить"
        if (sourceBranch == null || TableRowModel.SKIP_OPTION.equals(sourceBranch)) {
            Platform.runLater(() -> {
                row.setStatus("пропущено");
                row.setComment("Пользователь выбрал пропуск");
                row.setProgress(1.0);
            });
            logger.info("Строка пропущена: " + row.getSearchTerm());
            return;
        }

        try {
            Platform.runLater(() -> {
                row.setStatus("выполнение");
                row.setProgress(0.1);
            });
            logger.info("Обработка: " + sourceBranch + " → " + targetBranch);

            // Шаг 1: Найти существующий MR или создать новый
            MergeRequest mr = findOrCreateMr(projectId, sourceBranch, targetBranch, dryRun);
            if (mr == null) {
                // dry-run режим без реального MR
                Platform.runLater(() -> {
                    row.setStatus("выполнено");
                    row.setComment("Dry-run: MR не создавался");
                    row.setProgress(1.0);
                });
                return;
            }
            Platform.runLater(() -> row.setProgress(0.3));

            // Шаг 2: Дождаться готовности к одобрению
            waitForApprovable(projectId, mr.getIid());
            Platform.runLater(() -> row.setProgress(0.5));

            // Шаг 3: Одобрить MR
            if (!dryRun) {
                GitLabApiClient.withRetry(() -> {
                    apiClient.approveMergeRequest(projectId, mr.getIid());
                    return null;
                }, MAX_RETRIES);
                logger.info("MR одобрен: " + mr.getIid());
            } else {
                logger.info("Dry-run: пропуск одобрения MR " + mr.getIid());
            }
            Platform.runLater(() -> row.setProgress(0.7));

            // Шаг 4: Дождаться возможности слияния
            waitForMergeable(projectId, mr.getIid());
            Platform.runLater(() -> row.setProgress(0.85));

            // Шаг 5: Влить MR
            if (!dryRun) {
                MergeRequest merged = GitLabApiClient.withRetry(
                        () -> apiClient.mergeMergeRequest(projectId, mr.getIid()), MAX_RETRIES);
                logger.info("MR влит: " + merged.getIid());
            } else {
                logger.info("Dry-run: пропуск слияния MR " + mr.getIid());
            }

            Platform.runLater(() -> {
                row.setStatus("выполнено");
                row.setComment(dryRun ? "Dry-run выполнен" : "MR влит успешно");
                row.setProgress(1.0);
            });

        } catch (ConflictException e) {
            logger.warn("Конфликт при обработке " + sourceBranch + ": " + e.getMessage());
            Platform.runLater(() -> {
                row.setStatus("пропущено");
                row.setComment("Конфликт: " + e.getMessage());
                row.setProgress(1.0);
            });
        } catch (Exception e) {
            logger.error("Ошибка при обработке " + sourceBranch, e);
            Platform.runLater(() -> {
                row.setStatus("ошибка");
                row.setComment(e.getMessage());
                row.setProgress(1.0);
            });
        }
    }

    /**
     * Найти существующий открытый MR или создать новый.
     */
    private MergeRequest findOrCreateMr(String projectId, String sourceBranch,
                                         String targetBranch, boolean dryRun) throws Exception {
        List<MergeRequest> existing = GitLabApiClient.withRetry(
                () -> apiClient.getMergeRequests(projectId, sourceBranch, targetBranch), MAX_RETRIES);

        if (!existing.isEmpty()) {
            MergeRequest mr = existing.get(0);
            logger.info("Найден существующий MR: " + mr.getIid());
            return mr;
        }

        if (dryRun) {
            logger.info("Dry-run: MR " + sourceBranch + " → " + targetBranch + " не будет создан");
            return null;
        }

        String title = "MR: " + sourceBranch + " → " + targetBranch;
        MergeRequest created = GitLabApiClient.withRetry(
                () -> apiClient.createMergeRequest(projectId, sourceBranch, targetBranch, title), MAX_RETRIES);
        logger.info("Создан новый MR: " + created.getIid());
        return created;
    }

    /**
     * Ожидать, пока MR станет готов к одобрению.
     */
    private void waitForApprovable(String projectId, Long iid) throws Exception {
        for (int i = 0; i < APPROVE_POLL_ATTEMPTS; i++) {
            MergeRequest mr = GitLabApiClient.withRetry(
                    () -> apiClient.getMergeRequest(projectId, iid), MAX_RETRIES);

            String detailedStatus = mr.getDetailedMergeStatus();
            if ("not_approved".equals(detailedStatus)
                    || "mergeable".equals(detailedStatus)
                    || "approved".equals(detailedStatus)
                    || "ci_still_running".equals(detailedStatus)
                    || "ci_must_pass".equals(detailedStatus)) {
                return;
            }

            if ("broken_status".equals(detailedStatus) || "conflicts".equals(detailedStatus)) {
                throw new ConflictException("MR " + iid + " имеет конфликты или сломан: " + detailedStatus);
            }

            logger.info("Ожидание готовности MR " + iid + " к одобрению, статус: " + detailedStatus
                    + " (попытка " + (i + 1) + "/" + APPROVE_POLL_ATTEMPTS + ")");
            Thread.sleep(APPROVE_POLL_INTERVAL_MS);
        }
        logger.warn("Таймаут ожидания одобрения MR " + iid + ", продолжаем...");
    }

    /**
     * Ожидать, пока MR станет доступен для слияния (пайплайн завершён).
     */
    private void waitForMergeable(String projectId, Long iid) throws Exception {
        for (int i = 0; i < MERGE_POLL_ATTEMPTS; i++) {
            MergeRequest mr = GitLabApiClient.withRetry(
                    () -> apiClient.getMergeRequest(projectId, iid), MAX_RETRIES);

            String detailedStatus = mr.getDetailedMergeStatus();
            if ("mergeable".equals(detailedStatus) || "approved".equals(detailedStatus)) {
                return;
            }

            if ("broken_status".equals(detailedStatus) || "conflicts".equals(detailedStatus)) {
                throw new ConflictException("MR " + iid + " имеет конфликты или сломан перед слиянием: " + detailedStatus);
            }

            logger.info("Ожидание готовности MR " + iid + " к слиянию, статус: " + detailedStatus
                    + " (попытка " + (i + 1) + "/" + MERGE_POLL_ATTEMPTS + ")");
            Thread.sleep(MERGE_POLL_INTERVAL_MS);
        }
        logger.warn("Таймаут ожидания слияния MR " + iid + ", продолжаем...");
    }

    /**
     * Исключение для конфликтов при слиянии
     */
    public static class ConflictException extends Exception {
        public ConflictException(String message) {
            super(message);
        }
    }
}
