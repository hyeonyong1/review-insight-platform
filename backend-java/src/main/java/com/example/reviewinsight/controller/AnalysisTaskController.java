package com.example.reviewinsight.controller;

import com.example.reviewinsight.entity.AnalysisTask;
import com.example.reviewinsight.service.AnalysisTaskService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analysis-tasks")
public class AnalysisTaskController {

    private final AnalysisTaskService analysisTaskService;

    public AnalysisTaskController(AnalysisTaskService analysisTaskService) {
        this.analysisTaskService = analysisTaskService;
    }

    @PostMapping
    public AnalysisTask create(@RequestParam Long datasetId) {
        return analysisTaskService.createTask(datasetId);
    }

    @GetMapping("/{taskId}")
    public AnalysisTask get(@PathVariable Long taskId) {
        return analysisTaskService.getTask(taskId);
    }

    @GetMapping("/{taskId}/result")
    public JsonNode result(@PathVariable Long taskId) {
        return analysisTaskService.getResult(taskId);
    }
}
