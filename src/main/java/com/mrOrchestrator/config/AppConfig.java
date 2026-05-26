package com.mrOrchestrator.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Конфигурация приложения, загружаемая из config.yaml
 */
public class AppConfig {

    private GitLabConfig gitlab;
    private ExecutionConfig execution;
    private List<RepositoryConfig> repositories = new ArrayList<>();

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

    public List<RepositoryConfig> getRepositories() {
        if (repositories == null) {
            repositories = new ArrayList<>();
        }
        return repositories;
    }

    public void setRepositories(List<RepositoryConfig> repositories) {
        this.repositories = repositories == null ? new ArrayList<>() : repositories;
    }

    /**
     * Репозиторий, который можно заранее указать для поиска веток.
     */
    public static class RepositoryConfig {
        private String url;
        private boolean enabled = true;

        public RepositoryConfig() {
        }

        public RepositoryConfig(String url, boolean enabled) {
            this.url = url;
            this.enabled = enabled;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * Настройки подключения к GitLab
     */
    public static class GitLabConfig {
        private String baseUrl;
        private String token;
        private String trustStorePath;
        private String trustStorePassword;
        private String trustStoreType = "JKS";
        private boolean disableSslVerification = false;

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

        public String getTrustStorePath() {
            return trustStorePath;
        }

        public void setTrustStorePath(String trustStorePath) {
            this.trustStorePath = trustStorePath;
        }

        public String getTrustStorePassword() {
            return trustStorePassword;
        }

        public void setTrustStorePassword(String trustStorePassword) {
            this.trustStorePassword = trustStorePassword;
        }

        public String getTrustStoreType() {
            return trustStoreType;
        }

        public void setTrustStoreType(String trustStoreType) {
            this.trustStoreType = trustStoreType;
        }

        public boolean isDisableSslVerification() {
            return disableSslVerification;
        }

        public void setDisableSslVerification(boolean disableSslVerification) {
            this.disableSslVerification = disableSslVerification;
        }
    }

    /**
     * Настройки выполнения
     */
    public static class ExecutionConfig {
        private int threadCount = 1;
        private boolean dryRun = false;
        private int maxCommitsWarning = 0;
        private boolean approveOnly = false;
        private String stagingMergePattern = "(?i)merge.*branch.*staging";

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

        public int getMaxCommitsWarning() {
            return maxCommitsWarning;
        }

        public void setMaxCommitsWarning(int maxCommitsWarning) {
            this.maxCommitsWarning = maxCommitsWarning;
        }

        public boolean isApproveOnly() {
            return approveOnly;
        }

        public void setApproveOnly(boolean approveOnly) {
            this.approveOnly = approveOnly;
        }

        public String getStagingMergePattern() {
            return stagingMergePattern;
        }

        public void setStagingMergePattern(String stagingMergePattern) {
            this.stagingMergePattern = stagingMergePattern;
        }
    }
}
