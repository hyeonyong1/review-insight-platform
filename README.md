# Review Insight Platform｜智能用户评论分析平台

一个面向 **后端 / 软件开发 / 数字化技术岗** 的作品集项目：使用 Spring Boot 管理评论数据与异步分析任务，并将 NLP 模型封装为独立 FastAPI 服务，通过 HTTP 完成跨语言调用。

> 当前定位：**可运行的后端 MVP + 可复现的真实机器学习基线**。  
> 重点是工程链路完整、指标真实、边界清晰，不把未实现的 Redis / 复杂主题模型包装成已完成能力。

---

## 1. Architecture

```text
CSV 评论文件
   │
   ▼
Spring Boot Backend :8080
├── 数据集上传
├── 评论分页查询
├── AnalysisTask 状态机
├── JPA / H2（开发）
└── 每 500 条分批调用 Python
   │ HTTP/1.1 + JSON
   ▼
FastAPI NLP Service :8001
├── 模型加载 / 热重载
├── Jieba 词级 TF-IDF
├── 字符级 TF-IDF
└── One-vs-Rest Logistic Regression
   │
   ▼
18 维负面问题多标签结果
```

核心设计思想：

- **Java 管业务**：接口、数据库、分页、任务生命周期、结果聚合。
- **Python 管算法**：文本处理、模型训练、模型加载、推理。
- **HTTP 做服务边界**：两种语言只依赖 JSON 接口协议。
- **耗时任务异步化**：创建任务后立即返回 taskId，再查询状态与结果。
- **批量调用**：Java 每次最多发送 500 条评论，避免超大请求体。

---

## 2. Verified features

已经实际跑通：

- [x] CSV 评论批量导入
- [x] H2 数据持久化
- [x] 评论分页查询
- [x] `PENDING -> RUNNING -> SUCCESS / FAILED` 异步任务状态机
- [x] Java 分页读取并按 500 条/批调用 Python
- [x] Java 21 `HttpClient` 调用 FastAPI
- [x] Python FastAPI + Swagger API
- [x] 官方 ASAP 数据自动下载脚本
- [x] Jieba Word TF-IDF + Chinese Char TF-IDF
- [x] One-vs-Rest Logistic Regression 多标签分类
- [x] Precision / Recall / Micro-F1 / Macro-F1 / Hamming Loss 评估
- [x] 模型持久化与 `/model-info`、`/reload-model`
- [x] 中文业务标签映射
- [x] MySQL 运行配置预留

未作为“已完成能力”声明：

- Redis 业务缓存
- 消息队列
- BTM/LDA 在线主题模型
- 完整容器化 Java + Python 部署
- 前端管理台

---

## 3. Dataset & task

使用美团点评公开的 **ASAP** 中文餐饮评论数据集：

- 46,730 条真实用户评论
- 18 个细粒度评价维度
- 官方划分：36,850 train / 4,940 dev / 4,940 test
- 原始极性：`1 Positive / 0 Neutral / -1 Negative / -2 Not-Mentioned`

本项目将每个评价维度转换为一个“负面问题是否出现”的二分类标签：

```text
ASAP aspect == -1  -> complaint = 1
otherwise          -> complaint = 0
```

官方数据源：<https://github.com/Meituan-Dianping/asap>

---

## 4. ML baseline

```text
review text
  ├─ Jieba word TF-IDF (1–2 grams)
  └─ Chinese char TF-IDF (2–4 grams)
             ↓
        FeatureUnion
             ↓
One-vs-Rest Logistic Regression
             ↓
   18 negative-aspect labels
```

为了缓解负面标签长尾问题，Logistic Regression 使用：

```text
class_weight = balanced
```

### Verified validation result

以下是 2026-10-04 的真实本地训练结果，来自 **dev/validation split**，不是测试集：

| Metric | Value |
| --- | ---: |
| Train rows | 36,850 |
| Dev rows | 4,940 |
| Label count | 18 |
| Micro Precision | 0.4694 |
| Micro Recall | 0.5526 |
| **Micro F1** | **0.5076** |
| **Macro F1** | **0.4456** |
| Hamming Loss | 0.0450 |

完整记录：[`docs/verified_metrics.json`](docs/verified_metrics.json)

> 为什么不把 Hamming Loss 0.045 当主成绩？多标签任务中大部分标签对单条评论本来就是 0，Hamming Loss 很容易显得较低；Micro/Macro-F1 更适合一起判断模型实际效果。

---

## 5. Real integration bug solved

项目联调时真实遇到过：

```text
FastAPI -> 422 Unprocessable Entity -> body missing
```

Java 内存里已经能打印出正确 JSON，但 Python 端仍认为请求体为空。最终客户端改为：

- Jackson `ObjectMapper` 显式序列化请求；
- Java 21 `HttpClient`；
- 固定 HTTP/1.1；
- UTF-8 byte publisher；
- `Content-Type: application/json; charset=UTF-8`。

修复后 Java -> Python 返回 HTTP 200。

这部分是一个真实的跨服务接口联调案例，详见：[`docs/verified-run.md`](docs/verified-run.md)。

---

## 6. Project structure

```text
review-insight-platform/
├── backend-java/                    # Spring Boot business backend
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/example/reviewinsight/
│       │   ├── controller/
│       │   ├── dto/
│       │   ├── entity/
│       │   ├── repository/
│       │   └── service/
│       └── resources/
│           ├── application.yml
│           └── application-docker.yml
├── nlp-service/                     # FastAPI + ML
│   ├── app/
│   ├── scripts/
│   ├── data/asap/                   # generated locally; not committed
│   ├── models/                      # generated locally; not committed
│   └── results/                     # generated locally; not committed
├── sample-data/
├── scripts/
├── docs/
│   ├── architecture.md
│   ├── api-examples.md
│   ├── phase2.md
│   ├── verified-run.md
│   ├── verified_metrics.json
│   ├── resume-project.md
│   ├── interview-guide.md
│   └── github-upload.md
├── docker-compose.yml               # optional local MySQL
└── README.md
```

---

## 7. Quick start

### Python NLP service

Windows:

```bat
cd nlp-service
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
scripts\phase2_setup.bat
uvicorn app.main:app --reload --port 8001
```

Open:

```text
http://127.0.0.1:8001/docs
http://127.0.0.1:8001/model-info
```

### Java backend

Requirements:

- Java 17+
- Maven 3.9+

```bat
cd backend-java
mvn spring-boot:run
```

Backend:

```text
http://127.0.0.1:8080
```

H2 console:

```text
http://127.0.0.1:8080/h2-console
JDBC URL: jdbc:h2:mem:reviewdb
User: sa
Password: <empty>
```

---

## 8. End-to-end API example

From project root:

```bat
curl -X POST "http://127.0.0.1:8080/api/datasets/upload?name=demo" -F "file=@sample-data/reviews.csv"
```

```bat
curl "http://127.0.0.1:8080/api/datasets/1/reviews?page=0&size=5"
```

```bat
curl -X POST "http://127.0.0.1:8080/api/analysis-tasks?datasetId=1"
```

```bat
curl "http://127.0.0.1:8080/api/analysis-tasks/1"
```

```bat
curl "http://127.0.0.1:8080/api/analysis-tasks/1/result"
```

---

## 9. Resume & interview notes

- 简历项目描述：[`docs/resume-project.md`](docs/resume-project.md)
- 必须理解的知识点：[`docs/interview-guide.md`](docs/interview-guide.md)
- 已验证事实与局限：[`docs/verified-run.md`](docs/verified-run.md)

---

## 10. Known limitations / next steps

目前最值得继续做、但不影响本项目作为秋招作品集使用的方向：

1. 在验证集上为 18 个标签分别搜索最优阈值，减少过度预测。
2. 比较 TF-IDF baseline 与 MacBERT / Chinese-RoBERTa。
3. 将 H2 正式切换为 MySQL 做端到端持久化验证。
4. Python 服务增加超时、重试、熔断与监控。
5. 增加 Redis 的前提是先证明任务状态查询存在真实热点。
6. 增加 GitHub Actions 自动测试。

---

## License

Project code: MIT License.

ASAP dataset is an external dataset and follows the license/terms of its original repository; the CSV files are **not** vendored into this repository.
