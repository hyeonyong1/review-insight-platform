package com.example.reviewinsight.service;

import com.example.reviewinsight.dto.ReviewView;
import com.example.reviewinsight.repository.ReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;

    public ReviewService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    public Page<ReviewView> getReviews(Long datasetId, Pageable pageable) {
        return reviewRepository.findByDataset_Id(datasetId, pageable)
                .map(review -> new ReviewView(
                        review.getId(),
                        review.getExternalReviewId(),
                        review.getText()
                ));
    }
}
