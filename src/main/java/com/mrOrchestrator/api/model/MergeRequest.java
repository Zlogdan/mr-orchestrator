package com.mrOrchestrator.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Модель мёрж-реквеста GitLab
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MergeRequest {

    private Long iid;
    private String state;
    private String title;

    @JsonProperty("source_branch")
    private String sourceBranch;

    @JsonProperty("target_branch")
    private String targetBranch;

    @JsonProperty("merge_status")
    private String mergeStatus;

    @JsonProperty("detailed_merge_status")
    private String detailedMergeStatus;

    @JsonProperty("web_url")
    private String webUrl;

    public Long getIid() {
        return iid;
    }

    public void setIid(Long iid) {
        this.iid = iid;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSourceBranch() {
        return sourceBranch;
    }

    public void setSourceBranch(String sourceBranch) {
        this.sourceBranch = sourceBranch;
    }

    public String getTargetBranch() {
        return targetBranch;
    }

    public void setTargetBranch(String targetBranch) {
        this.targetBranch = targetBranch;
    }

    public String getMergeStatus() {
        return mergeStatus;
    }

    public void setMergeStatus(String mergeStatus) {
        this.mergeStatus = mergeStatus;
    }

    public String getDetailedMergeStatus() {
        return detailedMergeStatus;
    }

    public void setDetailedMergeStatus(String detailedMergeStatus) {
        this.detailedMergeStatus = detailedMergeStatus;
    }

    public String getWebUrl() {
        return webUrl;
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }

    @Override
    public String toString() {
        return "MergeRequest{iid=" + iid + ", state=" + state + ", sourceBranch=" + sourceBranch + "}";
    }
}
