package com.example.reviewinsight.service;

import com.example.reviewinsight.dto.KeywordCount;
import com.example.reviewinsight.dto.PythonAnalyzeRequest;
import com.example.reviewinsight.dto.PythonAnalyzeResponse;
import com.example.reviewinsight.dto.PythonReviewItem;
import com.example.reviewinsight.entity.AnalysisTask;
import com.example.reviewinsight.entity.Review;
import com.example.reviewinsight.entity.TaskStatus;
import com.example.reviewinsight.repository.AnalysisTaskRepository;
import com.example.reviewinsight.repository.ReviewRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalysisWorker {

    private static final int ANALYZE_BATCH_SIZE = 500;

    private final AnalysisTaskRepository taskRepository;
    private final ReviewRepository reviewRepository;
    private final NlpClient nlpClient;
    private final ObjectMapper objectMapper;

    public AnalysisWorker(
            AnalysisTaskRepository taskRepository,
            ReviewRepository reviewRepository,
            NlpClient nlpClient,
            ObjectMapper objectMapper
    ) {
        this.taskRepository = taskRepository;
        this.reviewRepository = reviewRepository;
        this.nlpClient = nlpClient;
        this.objectMapper = objectMapper;
    }

    @Async
    public void runTask(Long taskId) {
        AnalysisTask task = taskRepository.findById(taskId)
                .orElseThrow();

        task.setStatus(TaskStatus.RUNNING);
        task.setStartedAt(LocalDateTime.now());
        taskRepository.save(task);

        try {
            Map<String, Integer> issueCounts = new HashMap<>();
            Map<String, Integer> keywordCounts = new HashMap<>();
            List<Map<String, Object>> samples = new ArrayList<>();

            int pageNumber = 0;
            long total = 0;
            String modelName = "unknown";
            String modelMode = "unknown";

            while (true) {
                Page<Review> page = reviewRepository.findByDataset_Id(
                        task.getDatasetId(),
                        PageRequest.of(pageNumber, ANALYZE_BATCH_SIZE)
                );

                if (page.isEmpty()) {
                    break;
                }

                List<PythonReviewItem> items = page.getContent()
                        .stream()
                        .map(review -> new PythonReviewItem(review.getId(), review.getText()))
                        .toList();

                PythonAnalyzeResponse response = nlpClient.analyze(
                        new PythonAnalyzeRequest(items)
                );

                total += response.total();
                if (response.model() != null) {
                    modelName = response.model();
                }
                if (response.modelMode() != null) {
                    modelMode = response.modelMode();
                }

                if (response.issueCounts() != null) {
                    response.issueCounts().forEach(
                            (label, count) -> issueCounts.merge(label, count, Integer::sum)
                    );
                }

                if (response.topKeywords() != null) {
                    for (KeywordCount keyword : response.topKeywords()) {
                        keywordCounts.merge(
                                keyword.word(),
                                keyword.count(),
                                Integer::sum
                        );
                    }
                }

                if (response.samples() != null && samples.size() < 10) {
                    response.samples().stream()
                            .limit(10 - samples.size())
                            .forEach(sample -> {
                                Map<String, Object> item = new LinkedHashMap<>();
                                item.put("id", sample.id());
                                item.put("text", sample.text());
                                item.put("issues", sample.issues());
                                samples.add(item);
                            });
                }

                if (!page.hasNext()) {
                    break;
                }

                pageNumber++;
            }

            List<Map<String, Object>> topKeywords = keywordCounts.entrySet()
                    .stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(15)
                    .map(entry -> {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("word", entry.getKey());
                        item.put("count", entry.getValue());
                        return item;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> finalResult = new LinkedHashMap<>();
            finalResult.put("total", total);
            finalResult.put("issueCounts", issueCounts);
            finalResult.put("topKeywords", topKeywords);
            finalResult.put("samples", samples);
            finalResult.put("analyzeBatchSize", ANALYZE_BATCH_SIZE);
            finalResult.put("model", modelName);
            finalResult.put("modelMode", modelMode);

            task.setResultJson(objectMapper.writeValueAsString(finalResult));
            task.setStatus(TaskStatus.SUCCESS);
            task.setFinishedAt(LocalDateTime.now());
            taskRepository.save(task);

        } catch (Exception e) {
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage(e.getMessage());
            task.setFinishedAt(LocalDateTime.now());
            taskRepository.save(task);
        }
    }
}
