package com.mrOrchestrator.config;

/**
 * Конфигурация приложения, загружаемая из config.yaml
 */
public class AppConfig {

    private GitLabConfig gitlab;
    private ExecutionConfig execution;

    public GitLabConfig getGitlab() {
        return gitlab;
    }

    public void setGitlab(GitLabConfig gitlab) {
        this.gitlab = gitlab;
    }

    public ExecutionConfig getExecution() {
        return execution;
    }

    public void setExecution(ExecutionConfig execution) {
        this.execution = execution;
    }

    /**
     * Настройки подключения к GitLab
     */
    public static class GitLabConfig {
        private String baseUrl;
        private String token;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }

    /**
     * Настройки выполнения
     */
    public static class ExecutionConfig {
        private int threadCount = 1;
        private boolean dryRun = false;

        public int getThreadCount() {
            return threadCount;
        }

        public void setThreadCount(int threadCount) {
            this.threadCount = threadCount;
        }

        public boolean isDryRun() {
            return dryRun;
        }

        public void setDryRun(boolean dryRun) {
            this.dryRun = dryRun;
        }
    }
}
