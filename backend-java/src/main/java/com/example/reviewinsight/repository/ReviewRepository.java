package com.example.reviewinsight.repository;

import com.example.reviewinsight.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByDataset_Id(Long datasetId, Pageable pageable);

    long countByDataset_Id(Long datasetId);
}
