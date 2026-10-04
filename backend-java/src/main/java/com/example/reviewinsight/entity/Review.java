package com.example.reviewinsight.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String externalReviewId;

    @Lob
    @Column(nullable = false)
    private String text;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dataset_id", nullable = false)
    private ReviewDataset dataset;

    public Long getId() {
        return id;
    }

    public String getExternalReviewId() {
        return externalReviewId;
    }

    public String getText() {
        return text;
    }

    public ReviewDataset getDataset() {
        return dataset;
    }

    public void setExternalReviewId(String externalReviewId) {
        this.externalReviewId = externalReviewId;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setDataset(ReviewDataset dataset) {
        this.dataset = dataset;
    }
}
