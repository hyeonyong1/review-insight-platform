package com.example.reviewinsight.dto;

import java.util.List;

public record AnalyzedSample(
        Long id,
        String text,
        List<String> issues
) {
}
