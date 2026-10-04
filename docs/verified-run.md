# Verified local run

This repository is a portfolio project, so the README distinguishes **implemented**, **locally verified**, and **future-work** items instead of treating every planned feature as completed.

## What was actually verified

On 2026-10-04 the following workflow was run successfully on Windows 11:

1. Started the FastAPI NLP service on `127.0.0.1:8001`.
2. Started the Spring Boot backend on `127.0.0.1:8080`.
3. Uploaded `sample-data/reviews.csv` through the Java API.
4. Verified H2 persistence and paginated review queries.
5. Created an asynchronous `AnalysisTask` and observed `PENDING -> RUNNING`.
6. Debugged Java-to-Python HTTP integration and fixed a FastAPI `422 body missing` error.
7. Completed Java -> Python HTTP calls using Java 21 `HttpClient`, HTTP/1.1 and explicit UTF-8 JSON bytes.
8. Trained the Phase-2 ML model on the official ASAP train split.
9. Evaluated it on the official dev split and saved real metrics.
10. Restarted FastAPI and verified `/model-info` reported `tfidf-logreg-asap-v1`.
11. Verified `POST /analyze` returned HTTP 200 with the trained model active.

## Verified ML metrics

| Metric | Value |
| --- | ---: |
| Train rows | 36,850 |
| Validation rows | 4,940 |
| Aspect labels | 18 |
| Micro Precision | 0.4694 |
| Micro Recall | 0.5526 |
| Micro F1 | 0.5076 |
| Macro F1 | 0.4456 |
| Hamming Loss | 0.0450 |

The **4,940-row test split was not used for the resume metric claim**. It remains reserved for a future final evaluation.

## Known limitations

- This is a deliberately interpretable baseline, not a state-of-the-art NLP system.
- The current classifier uses the default decision behavior of One-vs-Rest Logistic Regression; per-label threshold calibration has not been implemented.
- Some long-tail aspects still produce false positives/false negatives.
- H2 was used for the verified local end-to-end flow; MySQL configuration is provided but was not the verified primary run.
- Redis is present in `docker-compose.yml` as a planned infrastructure component, but the application logic does **not** currently depend on Redis. Do not claim Redis integration in a resume yet.
- The repository does not commit the 34+ MB ASAP CSV files or the generated model binary. Training scripts reproduce them locally.

## Why the 422 bug matters

The most useful engineering lesson in the project came from a real integration failure. FastAPI returned:

```text
422 Unprocessable Entity: body missing
```

The Java side was creating the correct JSON string, but the request body was not arriving as expected. The final client implementation:

- serializes the Java record with Jackson `ObjectMapper`;
- sends with Java 21 `HttpClient`;
- fixes the protocol to HTTP/1.1;
- sends explicit UTF-8 bytes;
- sets `Content-Type: application/json; charset=UTF-8`.

That is a real example of cross-service debugging and is safe to discuss in an interview.
