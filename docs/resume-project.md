# Resume-ready project wording

## Recommended title

**智能用户评论分析平台｜后端开发 / NLP 服务化**

## Recommended technical stack

**Spring Boot、Spring Data JPA、H2/MySQL、Java 21 HttpClient、FastAPI、Python、Jieba、TF-IDF、Logistic Regression**

Do **not** currently list Redis as an implemented application feature. It is only reserved in infrastructure configuration.

## Version A — 后端 / 软件开发岗位（推荐）

- 设计并实现评论数据集上传、评论分页查询、分析任务创建与结果查询等 RESTful API，基于 Spring Boot + JPA 建立数据集、评论与分析任务模型，并使用 `PENDING / RUNNING / SUCCESS / FAILED` 状态机管理耗时分析任务。
- 将文本分析能力拆分为独立 FastAPI 服务，Java 端按 **500 条/批**读取评论并通过 HTTP 调用 Python 推理接口，聚合多批分类结果，避免万级文本一次性请求带来的超时与内存压力。
- 基于美团点评 ASAP 公开数据构建 **18 维负面问题多标签分类**基线，融合 Jieba 词级 TF-IDF 与字符级 TF-IDF，使用 One-vs-Rest Logistic Regression 在 36,850 条训练数据上训练；验证集 **Micro-F1 0.508、Macro-F1 0.446**。
- 在跨服务联调中定位 FastAPI `422 body missing` 问题，改用 Java 21 `HttpClient` 显式序列化 JSON、固定 HTTP/1.1 并以 UTF-8 字节发送请求体，完成 Java/Python 服务通信。

## Version B — 数据 / 数字化岗位

- 基于 46,730 条真实餐饮评论构建细粒度用户问题分析流程，将 18 个评价维度转换为负面问题多标签任务，完成中文文本清洗、Jieba 分词、词级/字符级 TF-IDF 特征工程及模型评估。
- 使用 One-vs-Rest Logistic Regression 进行多标签分类，在 36,850 条训练集与 4,940 条验证集上获得 **Micro-F1 0.508、Macro-F1 0.446**，并输出 Precision、Recall、Hamming Loss 等指标用于误差分析。
- 将模型封装为 FastAPI 服务并接入 Spring Boot 业务后端，实现 CSV 评论导入、异步分析任务、分页查询与结果统计，形成从数据输入到模型服务再到结果查询的完整链路。

## 30-second answer when asked “你做了什么？”

这个项目不是单纯训练一个文本模型。我把评论分析做成了一个前后端服务化的后端系统：Spring Boot 管数据集、评论和异步任务，Python FastAPI 管 NLP 模型；Java 每次按 500 条评论分批调用 Python，再把结果聚合保存。算法部分用美团 ASAP 数据，把 18 个评价维度改成负面问题检测，用词级和字符级 TF-IDF 配合 One-vs-Rest 逻辑回归。项目里还真实遇到过 Java 调 Python 时 FastAPI 422 请求体为空的问题，最后通过 Java 21 HttpClient、HTTP/1.1 和 UTF-8 JSON 显式发送解决了。

## Claims to avoid

Unless you implement and verify them later, do not say:

- “使用 Redis 缓存任务状态”
- “Docker Compose 一键部署全部服务”
- “使用 BTM/LDA 做了线上主题挖掘”
- “模型达到 90%+ 准确率”
- “独立设计了 ASAP 标签体系”
- “测试集 Micro-F1 0.508” — 0.508 is currently a **validation/dev** result, not the held-out test result.
