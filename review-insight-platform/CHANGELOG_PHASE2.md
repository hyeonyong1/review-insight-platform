# Phase 2 changes

Compared with the MVP you already ran successfully, this package adds:

1. Real ML training pipeline based on the official ASAP dataset.
2. Automatic dataset download script.
3. Jieba word-level TF-IDF + character TF-IDF feature union.
4. One-vs-Rest Logistic Regression for negative-aspect multi-label classification.
5. Micro/Macro F1, Precision, Recall and Hamming Loss evaluation.
6. Saved model + model metadata + evaluation report.
7. FastAPI auto-loads the trained model and falls back to rules before training.
8. `/model-info` and `/reload-model` endpoints.
9. Java result now records the actual model name/mode instead of hard-coding `rule-baseline-v1`.
10. The Java→Python HTTP/1.1 UTF-8 fix that solved the real 422 bug is included.
11. Windows helper scripts for the current Java/Maven environment.

## What still requires a local run

The repository deliberately does **not** include the 46k-review dataset or a pre-trained model. They are generated locally because:

- large source data should not be committed into a portfolio repository;
- resume metrics must come from the model actually trained on the official data, not fabricated numbers;
- the resulting model file can be large and is machine-generated.

Run once:

```bat
cd nlp-service
scripts\phase2_setup.bat
```

After it finishes, send the contents of `nlp-service/results/metrics.json` back for the final resume wording.
