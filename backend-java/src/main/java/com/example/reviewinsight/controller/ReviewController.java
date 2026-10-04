package com.example.reviewinsight.controller;

import com.example.reviewinsight.dto.ReviewView;
import com.example.reviewinsight.service.ReviewService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/datasets/{datasetId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public Page<ReviewView> page(
            @PathVariable Long datasetId,
            Pageable pageable
    ) {
        return reviewService.getReviews(datasetId, pageable);
    }
}
