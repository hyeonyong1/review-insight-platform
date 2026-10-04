# Phase 2: Real ML model

## Goal

Replace the keyword fallback with a real multi-label machine-learning model while keeping the Java/Python service boundary unchanged.

## Dataset

Official Meituan-Dianping ASAP dataset:

- 46,730 genuine Dianping restaurant reviews
- 18 manually annotated aspect categories
- polarity: `1 positive / 0 neutral / -1 negative / -2 not-mentioned`
- official split: 36,850 train / 4,940 dev / 4,940 test

The project transforms each aspect into a **negative issue detector**:

```text
ASAP aspect == -1  -> complaint label = 1
otherwise          -> complaint label = 0
```

This makes the output directly useful for a complaint-analysis backend.

## Model

```text
review text
  ├─ Jieba word TF-IDF (1-2 grams)
  └─ Chinese character TF-IDF (2-4 grams)
          ↓ FeatureUnion
One-vs-Rest Logistic Regression
          ↓
18 negative-aspect labels
```

`class_weight=balanced` is used because negative aspects are long-tail labels.

## Evaluation

The training script produces:

- Micro Precision
- Micro Recall
- Micro F1
- Macro F1
- Hamming Loss
- per-aspect classification report

No metric is hard-coded into the README. Use the generated `nlp-service/results/metrics.json` for resume claims.

## One-command Windows setup

From `nlp-service`:

```bat
scripts\phase2_setup.bat
```

It will:

1. reuse/create `.venv`
2. install dependencies
3. download official ASAP train/dev/test files
4. train and evaluate the model
5. save model + metrics

Then restart NLP:

```bat
scripts\start_nlp.bat
```

Check:

```text
GET http://127.0.0.1:8001/model-info
```

`available: true` means the ML model is active.
