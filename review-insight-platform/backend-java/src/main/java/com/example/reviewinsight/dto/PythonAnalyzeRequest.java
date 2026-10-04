package com.example.reviewinsight.dto;

import java.util.List;

public record PythonAnalyzeRequest(
        List<PythonReviewItem> reviews
) {
}
