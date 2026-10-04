package com.example.reviewinsight.service;

import com.example.reviewinsight.entity.AnalysisTask;
import com.example.reviewinsight.entity.TaskStatus;
import com.example.reviewinsight.repository.AnalysisTaskRepository;
import com.example.reviewinsight.repository.ReviewDatasetRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class AnalysisTaskService {

    private final AnalysisTaskRepository taskRepository;
    private final ReviewDatasetRepository datasetRepository;
    private final AnalysisWorker analysisWorker;
    private final ObjectMapper objectMapper;

    public AnalysisTaskService(
            AnalysisTaskRepository taskRepository,
            ReviewDatasetRepository datasetRepository,
            AnalysisWorker analysisWorker,
            ObjectMapper objectMapper
    ) {
        this.taskRepository = taskRepository;
        this.datasetRepository = datasetRepository;
        this.analysisWorker = analysisWorker;
        this.objectMapper = objectMapper;
    }

    public AnalysisTask createTask(Long datasetId) {
        if (!datasetRepository.existsById(datasetId)) {
            throw new IllegalArgumentException("Dataset not found: " + datasetId);
        }

        AnalysisTask task = new AnalysisTask();
        task.setDatasetId(datasetId);
        task.setStatus(TaskStatus.PENDING);
        task = taskRepository.save(task);

        analysisWorker.runTask(task.getId());
        return task;
    }

    public AnalysisTask getTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
    }

    public JsonNode getResult(Long taskId) {
        AnalysisTask task = getTask(taskId);

        if (task.getStatus() != TaskStatus.SUCCESS || task.getResultJson() == null) {
            throw new IllegalStateException(
                    "Task is not ready. Current status: " + task.getStatus()
            );
        }

        try {
            return objectMapper.readTree(task.getResultJson());
        } catch (Exception e) {
            throw new IllegalStateException("Invalid stored task result.", e);
        }
    }
}
