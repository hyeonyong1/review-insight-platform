# Interview guide — what you must understand

You do not need to memorize every line of code. You **do** need to be able to explain the following ten points clearly.

## 1. Why this is a backend project, not only an ML project

The system has two responsibilities:

- **Spring Boot**: API, data persistence, pagination, task lifecycle, orchestration.
- **FastAPI/Python**: text preprocessing, model loading and inference.

The boundary is HTTP/JSON. This is service decoupling: Java and Python can evolve independently as long as the API contract stays stable.

## 2. What the database tables represent

Core entities:

- `ReviewDataset`: one uploaded data batch.
- `Review`: one user review, linked to a dataset.
- `AnalysisTask`: one analysis job, including status, result JSON and error information.

This gives traceability: a result belongs to a task; a task belongs to a dataset.

## 3. Why analysis is asynchronous

Bad design:

```text
POST /analyze -> wait tens of seconds -> return result
```

Problems: long-held HTTP connection, timeout risk, poor user feedback, hard failure handling.

Current design:

```text
create task -> immediately return taskId
                ↓
       background worker
PENDING -> RUNNING -> SUCCESS / FAILED
```

The client polls task state and later asks for the result.

## 4. Why batch size is 500

Sending tens of thousands of reviews in one request creates a large body, higher memory usage, slower retries and bigger failure blast radius. The Java worker pages through reviews and sends at most 500 per request, then aggregates counts and samples.

500 is an engineering default, not a magic number. In production it would be benchmarked and made configurable.

## 5. What TF-IDF does

TF-IDF gives higher weight to terms that are important in one document but less common across the corpus.

This project combines:

- Jieba **word TF-IDF**, 1–2 grams;
- Chinese **character TF-IDF**, 2–4 grams.

The character branch helps with Chinese wording variation and words not perfectly segmented by Jieba.

## 6. Why Logistic Regression + One-vs-Rest

Each review can have several negative aspects at the same time, so it is a **multi-label** problem.

One-vs-Rest turns 18 labels into 18 binary classifiers:

```text
服务态度是不是负面？
价格水平是不是负面？
环境噪音是不是负面？
...
```

Logistic Regression is fast, interpretable and suitable as a strong classical baseline for sparse TF-IDF features.

## 7. Why `class_weight="balanced"`

Negative examples for some aspects are much rarer than non-negative/not-mentioned examples. Without balancing, the classifier may learn to predict 0 too often. `class_weight="balanced"` increases the loss contribution of minority-class samples.

It does not completely solve long-tail imbalance, which is why the current Macro-F1 is lower than Micro-F1.

## 8. How to read the metrics

Verified validation metrics:

- Micro Precision: 0.469
- Micro Recall: 0.553
- Micro F1: 0.508
- Macro F1: 0.446
- Hamming Loss: 0.045

**Micro-F1** pools decisions across all labels and is dominated more by frequent labels.

**Macro-F1** calculates F1 per label and averages them equally, so it exposes weak long-tail labels. Macro-F1 being lower is consistent with class imbalance.

**Hamming Loss** is the fraction of individual label decisions that are wrong. Because most labels are absent for a given review, a low Hamming Loss alone should not be advertised as proof of an excellent model.

## 9. The real 422 integration bug

Symptom:

```text
POST /analyze -> FastAPI 422 -> body missing
```

Debugging sequence:

1. Confirmed Java constructed the correct JSON.
2. Confirmed request reached Python.
3. Added explicit `Content-Type`, but the problem remained.
4. Replaced the ambiguous client path with Java 21 `HttpClient`.
5. Forced HTTP/1.1 and sent explicit UTF-8 byte body.
6. FastAPI returned HTTP 200.

What to say in an interview: the bug showed that “correct JSON in memory” does not guarantee “correct bytes on the wire”; service integration requires checking transport details as well as business payloads.

## 10. What you would improve next

A sensible answer:

1. Tune a decision threshold per aspect on the validation set to reduce over-prediction.
2. Compare the TF-IDF baseline with a Chinese pretrained model such as MacBERT/RoBERTa.
3. Replace H2 with MySQL for persistence.
4. Introduce Redis only after profiling high-frequency task-status reads.
5. Add retry/circuit-breaker handling for Python-service failures.
6. Containerize Java + Python + MySQL and add CI tests.

## Common interview questions

### “为什么不用 BERT？”

Because the first goal was to build a reproducible backend system with an explainable baseline. TF-IDF + Logistic Regression trains quickly, works naturally with sparse text features and lets the service architecture be validated before introducing a heavier model. A pretrained model is an appropriate next experiment, not a prerequisite for the backend design.

### “为什么不用同步请求？”

Analysis time grows with dataset size. Synchronous requests hold web threads and are more vulnerable to timeout. The task-state design separates request acceptance from execution.

### “如果 Python 服务挂了怎么办？”

Current version marks the task `FAILED` and stores the error. A production version would add bounded retries, timeout configuration, idempotency, circuit breaking and dead-letter/retry queues.

### “为什么结果存 JSON？”

The current result structure is aggregate-oriented and still evolving, so a JSON payload keeps the MVP simple. If downstream querying becomes complex, normalized result tables or JSON-capable database columns would be more appropriate.

### “为什么先 H2？”

H2 reduces environment setup cost and isolates application logic during the MVP. MySQL configuration is already reserved; production-like verification should move persistence to MySQL.
