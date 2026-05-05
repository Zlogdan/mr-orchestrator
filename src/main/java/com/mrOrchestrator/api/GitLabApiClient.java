package com.mrOrchestrator.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mrOrchestrator.api.model.Branch;
import com.mrOrchestrator.api.model.MergeRequest;
import com.mrOrchestrator.api.model.Pipeline;
import com.mrOrchestrator.config.AppConfig;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

/**
 * Клиент для взаимодействия с GitLab REST API
 */
public class GitLabApiClient {

    private final String baseUrl;
    private final String token;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GitLabApiClient(String baseUrl, String token) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.token = token;
        this.httpClient = createHttpClient(null, null, null, false);
        this.objectMapper = new ObjectMapper();
    }

    public GitLabApiClient(AppConfig.GitLabConfig gitLabConfig) {
        this.baseUrl = gitLabConfig.getBaseUrl().endsWith("/")
                ? gitLabConfig.getBaseUrl().substring(0, gitLabConfig.getBaseUrl().length() - 1)
                : gitLabConfig.getBaseUrl();
        this.token = gitLabConfig.getToken();
        this.httpClient = createHttpClient(
                gitLabConfig.getTrustStorePath(),
                gitLabConfig.getTrustStorePassword(),
                gitLabConfig.getTrustStoreType(),
                gitLabConfig.isDisableSslVerification()
        );
        this.objectMapper = new ObjectMapper();
    }

    private HttpClient createHttpClient(String trustStorePath, String trustStorePassword,
                                        String trustStoreType, boolean disableSslVerification) {
        try {
            HttpClient.Builder builder = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(30));

            if (disableSslVerification) {
                TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            @Override
                            public void checkClientTrusted(X509Certificate[] chain, String authType) {
                                // no-op
                            }

                            @Override
                            public void checkServerTrusted(X509Certificate[] chain, String authType) {
                                // no-op
                            }

                            @Override
                            public X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[0];
                            }
                        }
                };

                SSLContext insecureSslContext = SSLContext.getInstance("TLS");
                insecureSslContext.init(null, trustAllCerts, null);

                SSLParameters sslParameters = new SSLParameters();
                sslParameters.setEndpointIdentificationAlgorithm(null);

                builder.sslContext(insecureSslContext);
                builder.sslParameters(sslParameters);
                return builder.build();
            }

            if (trustStorePath != null && !trustStorePath.isBlank()) {
                KeyStore keyStore = KeyStore.getInstance(
                        (trustStoreType == null || trustStoreType.isBlank()) ? "JKS" : trustStoreType);

                char[] password = (trustStorePassword == null || trustStorePassword.isBlank())
                        ? null
                        : trustStorePassword.toCharArray();

                try (FileInputStream fis = new FileInputStream(trustStorePath)) {
                    keyStore.load(fis, password);
                }

                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(keyStore);

                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, tmf.getTrustManagers(), null);

                builder.sslContext(sslContext);
            }

            return builder.build();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Ошибка инициализации SSL/truststore: " + e.getMessage(), e);
        }
    }

    /**
     * Получить все ветки проекта (с пагинацией)
     */
    public List<Branch> getBranches(String projectId) throws Exception {
        List<Branch> allBranches = new ArrayList<>();
        int page = 1;
        while (true) {
            String url = baseUrl + "/api/v4/projects/" + projectId
                    + "/repository/branches?per_page=100&page=" + page;
            List<Branch> pageBranches = getList(url, new TypeReference<List<Branch>>() {});
            if (pageBranches.isEmpty()) break;
            allBranches.addAll(pageBranches);
            if (pageBranches.size() < 100) break;
            page++;
        }
        return allBranches;
    }

    /**
     * Поиск веток по частичному совпадению имени
     */
    public List<Branch> searchBranches(String projectId, String search) throws Exception {
        String encodedSearch = URLEncoder.encode(search, StandardCharsets.UTF_8);
        String url = baseUrl + "/api/v4/projects/" + projectId
                + "/repository/branches?search=" + encodedSearch + "&per_page=100";
        return getList(url, new TypeReference<List<Branch>>() {});
    }

    /**
     * Получить открытые MR по исходной и целевой ветке
     */
    public List<MergeRequest> getMergeRequests(String projectId, String sourceBranch, String targetBranch) throws Exception {
        String encodedSource = URLEncoder.encode(sourceBranch, StandardCharsets.UTF_8);
        String encodedTarget = URLEncoder.encode(targetBranch, StandardCharsets.UTF_8);
        String url = baseUrl + "/api/v4/projects/" + projectId
                + "/merge_requests?source_branch=" + encodedSource
                + "&target_branch=" + encodedTarget
                + "&state=opened";
        return getList(url, new TypeReference<List<MergeRequest>>() {});
    }

    /**
     * Создать новый мёрж-реквест
     */
    public MergeRequest createMergeRequest(String projectId, String sourceBranch, String targetBranch, String title) throws Exception {
        String url = baseUrl + "/api/v4/projects/" + projectId + "/merge_requests";
        Map<String, String> body = new HashMap<>();
        body.put("source_branch", sourceBranch);
        body.put("target_branch", targetBranch);
        body.put("title", title);
        String json = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("PRIVATE-TOKEN", token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = send(request, url);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("Ошибка создания MR: HTTP " + response.statusCode() + " - " + response.body());
        }
        return objectMapper.readValue(response.body(), MergeRequest.class);
    }

    /**
     * Результат сравнения двух веток: количество коммитов + список заголовков
     * коммитов, совпавших с паттерном staging-merge.
     */
    public static class BranchCompareResult {
        public final int commitCount;
        public final List<String> stagingMergeMessages;

        public BranchCompareResult(int commitCount, List<String> stagingMergeMessages) {
            this.commitCount = commitCount;
            this.stagingMergeMessages = stagingMergeMessages;
        }
    }

    /**
     * Получить количество коммитов в sourceBranch относительно targetBranch.
     * Использует GitLab Compare API — возвращает длину массива commits.
     */
    public int getCommitCount(String projectId, String targetBranch, String sourceBranch) throws Exception {
        return compareBranches(projectId, targetBranch, sourceBranch, null).commitCount;
    }

    /**
     * Сравнить ветки и вернуть:
     * - количество коммитов
     * - заголовки коммитов, совпавших с паттерном (если задан)
     * Один вызов GitLab Compare API покрывает оба запроса.
     */
    public BranchCompareResult compareBranches(String projectId, String targetBranch,
                                               String sourceBranch, String stagingPattern) throws Exception {
        String url = baseUrl + "/api/v4/projects/" + projectId + "/repository/compare"
                + "?from=" + URLEncoder.encode(targetBranch, StandardCharsets.UTF_8)
                + "&to="   + URLEncoder.encode(sourceBranch, StandardCharsets.UTF_8);
        String body = get(url);
        Map<String, Object> resp = objectMapper.readValue(body, new TypeReference<>() {});
        Object commitsObj = resp.get("commits");
        if (!(commitsObj instanceof List<?> list)) {
            return new BranchCompareResult(0, List.of());
        }

        int count = list.size();
        List<String> stagingMerges = new ArrayList<>();

        if (stagingPattern != null && !stagingPattern.isBlank()) {
            Pattern pattern = Pattern.compile(stagingPattern);
            for (Object commit : list) {
                if (commit instanceof Map<?, ?> commitMap) {
                    Object title = commitMap.get("title");
                    if (title instanceof String titleStr && pattern.matcher(titleStr).find()) {
                        stagingMerges.add(titleStr);
                    }
                }
            }
        }

        return new BranchCompareResult(count, stagingMerges);
    }

    /**
     * Получить один мёрж-реквест по IID
     */
    public MergeRequest getMergeRequest(String projectId, Long iid) throws Exception {
        String url = baseUrl + "/api/v4/projects/" + projectId + "/merge_requests/" + iid;
        String body = get(url);
        return objectMapper.readValue(body, MergeRequest.class);
    }

    /**
     * Одобрить мёрж-реквест
     */
    public void approveMergeRequest(String projectId, Long iid) throws Exception {
        String url = baseUrl + "/api/v4/projects/" + projectId + "/merge_requests/" + iid + "/approve";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("PRIVATE-TOKEN", token)
                .POST(HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = send(request, url);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("Ошибка подтверждения MR: HTTP " + response.statusCode() + " - " + response.body());
        }
    }

    /**
     * Влить мёрж-реквест
     */
    public MergeRequest mergeMergeRequest(String projectId, Long iid) throws Exception {
        String url = baseUrl + "/api/v4/projects/" + projectId + "/merge_requests/" + iid + "/merge";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("PRIVATE-TOKEN", token)
                .PUT(HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = send(request, url);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("Ошибка слияния MR: HTTP " + response.statusCode() + " - " + response.body());
        }
        return objectMapper.readValue(response.body(), MergeRequest.class);
    }

    /**
     * Извлечь URL-закодированный идентификатор проекта из URL репозитория.
     * Пример: https://gitlab.com/group/subgroup/project → group%2Fsubgroup%2Fproject
     */
    public String extractProjectId(String repoUrl) {
        // Убираем протокол и хост
        String url = repoUrl.trim();
        if (url.endsWith(".git")) {
            url = url.substring(0, url.length() - 4);
        }

        // Определяем базовый URL (протокол + хост)
        String base = this.baseUrl.replaceAll("/$", "");
        String path;
        if (url.startsWith(base)) {
            path = url.substring(base.length());
        } else {
            // Попытка извлечь путь из произвольного URL
            URI uri = URI.create(url);
            path = uri.getPath();
        }

        // Удаляем ведущий слэш
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        // URL-кодируем слэши как %2F
        return path.replace("/", "%2F");
    }

    /**
     * Вспомогательный метод с повтором при ошибке
     */
    public static <T> T withRetry(Callable<T> action, int maxRetries) throws Exception {
        Exception lastException = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return action.call();
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxRetries) {
                    Thread.sleep(2000L * (attempt + 1));
                }
            }
        }
        throw lastException;
    }

    /**
     * Выполнить GET-запрос и вернуть тело ответа
     */
    private String get(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("PRIVATE-TOKEN", token)
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = send(request, url);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("HTTP ошибка " + response.statusCode() + " для " + url + ": " + response.body());
        }
        return response.body();
    }

    private HttpResponse<String> send(HttpRequest request, String url) throws Exception {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (SSLHandshakeException e) {
            throw new RuntimeException(
                    "SSL ошибка при обращении к " + url + ": " + e.getMessage() +
                            ". Добавьте сертификат GitLab в доверенные CA JRE или настройте truststore в config.yaml (gitlab.trustStorePath, gitlab.trustStorePassword, gitlab.trustStoreType).",
                    e
            );
        }
    }

    /**
     * Выполнить GET-запрос и десериализовать список
     */
    private <T> List<T> getList(String url, TypeReference<List<T>> typeRef) throws Exception {
        String body = get(url);
        return objectMapper.readValue(body, typeRef);
    }
}
