package com.example.reviewinsight.service;

import com.example.reviewinsight.entity.Review;
import com.example.reviewinsight.entity.ReviewDataset;
import com.example.reviewinsight.repository.ReviewDatasetRepository;
import com.example.reviewinsight.repository.ReviewRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class DatasetService {

    private static final int BATCH_SIZE = 500;

    private final ReviewDatasetRepository datasetRepository;
    private final ReviewRepository reviewRepository;

    public DatasetService(
            ReviewDatasetRepository datasetRepository,
            ReviewRepository reviewRepository
    ) {
        this.datasetRepository = datasetRepository;
        this.reviewRepository = reviewRepository;
    }

    @Transactional
    public ReviewDataset importCsv(MultipartFile file, String datasetName) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("CSV file must not be empty.");
        }

        ReviewDataset dataset = new ReviewDataset();
        dataset.setName(
                datasetName == null || datasetName.isBlank()
                        ? file.getOriginalFilename()
                        : datasetName
        );
        dataset.setReviewCount(0);
        dataset = datasetRepository.save(dataset);

        long count = 0;
        List<Review> batch = new ArrayList<>(BATCH_SIZE);

        try (
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)
                );
                CSVParser parser = CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .build()
                        .parse(reader)
        ) {
            for (CSVRecord record : parser) {
                String text = record.get("text");

                if (text == null || text.isBlank()) {
                    continue;
                }

                Review review = new Review();
                review.setDataset(dataset);
                review.setText(text.trim());

                if (record.isMapped("review_id")) {
                    review.setExternalReviewId(record.get("review_id"));
                }

                batch.add(review);
                count++;

                if (batch.size() >= BATCH_SIZE) {
                    reviewRepository.saveAll(batch);
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                reviewRepository.saveAll(batch);
            }

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Failed to parse CSV. Required header: text. Optional header: review_id.",
                    e
            );
        }

        dataset.setReviewCount(count);
        return datasetRepository.save(dataset);
    }

    public ReviewDataset getDataset(Long id) {
        return datasetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dataset not found: " + id));
    }

    public List<ReviewDataset> listDatasets() {
        return datasetRepository.findAll();
    }
}
