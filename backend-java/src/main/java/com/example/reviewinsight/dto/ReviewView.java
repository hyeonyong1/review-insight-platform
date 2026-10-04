package com.example.reviewinsight.dto;

public record ReviewView(
        Long id,
        String externalReviewId,
        String text
) {
}
