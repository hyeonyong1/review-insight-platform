package com.example.reviewinsight.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PythonAnalyzeResponse(
        int total,
        @JsonProperty("issue_counts")
        Map<String, Integer> issueCounts,
        @JsonProperty("top_keywords")
        List<KeywordCount> topKeywords,
        List<AnalyzedSample> samples,
        String model,
        @JsonProperty("model_mode")
        String modelMode
) {
}
