# Architecture

```text
┌─────────────────┐
│ Client / Postman│
└───────┬─────────┘
        │ HTTP
        ▼
┌─────────────────────────────┐
│ Spring Boot Backend         │
│                             │
│ DatasetController           │
│ ReviewController            │
│ AnalysisTaskController      │
│                             │
│ DatasetService              │
│ AnalysisTaskService         │
│ AnalysisWorker (@Async)     │
└───────┬──────────────┬──────┘
        │              │
        │ JPA          │ HTTP
        ▼              ▼
┌──────────────┐   ┌─────────────────┐
│ H2 / MySQL   │   │ Python NLP      │
│              │   │ FastAPI         │
│ datasets     │   │ /analyze        │
│ reviews      │   │ /health         │
│ tasks        │   └─────────────────┘
└──────────────┘
```

## 核心思想

Java 不负责“怎么算文本”。

Python 不负责“用户、数据、任务如何管理”。

两个服务通过清晰 HTTP 接口解耦。
