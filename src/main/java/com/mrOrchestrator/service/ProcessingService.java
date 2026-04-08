package com.mrOrchestrator.service;

import com.mrOrchestrator.api.GitLabApiClient;
import com.mrOrchestrator.config.AppConfig;
import com.mrOrchestrator.ui.model.TableRowModel;
import com.mrOrchestrator.util.AppLogger;
import javafx.application.Platform;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Сервис управления параллельной обработкой всех строк таблицы
 */
public class ProcessingService {

    private static final AppLogger logger = AppLogger.getInstance();

    private final AppConfig config;
    private final GitLabApiClient apiClient;
    private final MergeRequestService mrService;

    public ProcessingService(AppConfig config, GitLabApiClient apiClient) {
        this.config = config;
        this.apiClient = apiClient;
        this.mrService = new MergeRequestService(apiClient);
    }

    /**
     * Запустить обработку всех строк таблицы в пуле потоков.
     *
     * @param rows         список строк таблицы
     * @param repoUrl      URL репозитория
     * @param targetBranch целевая ветка
     * @param logCallback  callback для записи в UI лог
     */
    public void processAllRows(List<TableRowModel> rows, String repoUrl,
                               String targetBranch, Consumer<String> logCallback) {
        logger.setUiCallback(logCallback);

        int threadCount = config.getExecution().getThreadCount();
        boolean dryRun = config.getExecution().isDryRun();

        String projectId = apiClient.extractProjectId(repoUrl);
        logger.info("Начало обработки: проект " + projectId + ", ветка " + targetBranch
                + ", потоков: " + threadCount + ", dry-run: " + dryRun);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        // Семафор ограничивает число одновременных API вызовов
        Semaphore semaphore = new Semaphore(threadCount);

        for (TableRowModel row : rows) {
            executor.submit(() -> {
                try {
                    semaphore.acquire();
                    try {
                        mrService.processRow(row, projectId, targetBranch, dryRun);
                    } finally {
                        semaphore.release();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Platform.runLater(() -> {
                        row.setStatus("ошибка");
                        row.setComment("Поток прерван");
                    });
                }
            });
        }

        executor.shutdown();
        new Thread(() -> {
            try {
                executor.awaitTermination(2, TimeUnit.HOURS);
                logger.info("Обработка завершена");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Ожидание завершения прервано", e);
            }
        }, "processing-monitor").start();
    }
}
