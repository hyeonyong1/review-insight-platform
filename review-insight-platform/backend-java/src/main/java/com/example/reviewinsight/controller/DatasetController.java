package com.example.reviewinsight.controller;

import com.example.reviewinsight.entity.ReviewDataset;
import com.example.reviewinsight.service.DatasetService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/datasets")
public class DatasetController {

    private final DatasetService datasetService;

    public DatasetController(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public Map<String, Object> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String name
    ) {
        ReviewDataset dataset = datasetService.importCsv(file, name);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("datasetId", dataset.getId());
        result.put("name", dataset.getName());
        result.put("reviewCount", dataset.getReviewCount());
        return result;
    }

    @GetMapping
    public List<ReviewDataset> list() {
        return datasetService.listDatasets();
    }

    @GetMapping("/{id}")
    public ReviewDataset get(@PathVariable Long id) {
        return datasetService.getDataset(id);
    }
}
