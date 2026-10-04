package com.example.reviewinsight.service;

import com.example.reviewinsight.dto.PythonAnalyzeRequest;
import com.example.reviewinsight.dto.PythonAnalyzeResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Service
public class NlpClient {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String baseUrl;

    public NlpClient(
            ObjectMapper objectMapper,
            @Value("${nlp.base-url}") String baseUrl
    ) {
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    public PythonAnalyzeResponse analyze(PythonAnalyzeRequest request) {
        try {
            String jsonBody = objectMapper.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/analyze"))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(
                            jsonBody.getBytes(StandardCharsets.UTF_8)
                    ))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "NLP service error: HTTP " + response.statusCode() + " - " + response.body()
                );
            }

            return objectMapper.readValue(response.body(), PythonAnalyzeResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to call NLP service: " + e.getMessage(), e);
        }
    }
}
