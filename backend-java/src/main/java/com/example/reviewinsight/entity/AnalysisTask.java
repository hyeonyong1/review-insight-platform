package com.example.reviewinsight.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "analysis_tasks")
public class AnalysisTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long datasetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @Lob
    private String resultJson;

    @Lob
    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    @PrePersist
    public void prePersist() {
        if (status == null) {
            status = TaskStatus.PENDING;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getDatasetId() {
        return datasetId;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getResultJson() {
        return resultJson;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setDatasetId(Long datasetId) {
        this.datasetId = datasetId;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public void setResultJson(String resultJson) {
        this.resultJson = resultJson;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }
}
