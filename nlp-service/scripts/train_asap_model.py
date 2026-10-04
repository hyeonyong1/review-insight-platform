from __future__ import annotations

from pathlib import Path
import json
import sys

import joblib
import numpy as np
import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.pipeline import FeatureUnion, Pipeline
from sklearn.multiclass import OneVsRestClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import (
    f1_score,
    precision_score,
    recall_score,
    hamming_loss,
    classification_report,
)

BASE_DIR = Path(__file__).resolve().parents[1]
if str(BASE_DIR) not in sys.path:
    sys.path.insert(0, str(BASE_DIR))

from app.text_features import clean_text, jieba_tokenize
DATA_DIR = BASE_DIR / "data" / "asap"
MODEL_DIR = BASE_DIR / "models"
RESULT_DIR = BASE_DIR / "results"
MODEL_DIR.mkdir(parents=True, exist_ok=True)
RESULT_DIR.mkdir(parents=True, exist_ok=True)

MODEL_PATH = MODEL_DIR / "asap_negative_aspect_model.joblib"
META_PATH = MODEL_DIR / "model_meta.json"
METRICS_PATH = RESULT_DIR / "metrics.json"
REPORT_PATH = RESULT_DIR / "classification_report.txt"

IGNORE_COLUMNS = {"index", "id", "star", "rating", "score"}
TEXT_CANDIDATES = ["reviewbody", "review", "content", "text"]


def detect_text_column(df: pd.DataFrame) -> str:
    lower_map = {str(c).lower(): c for c in df.columns}
    for candidate in TEXT_CANDIDATES:
        if candidate in lower_map:
            return lower_map[candidate]
    object_cols = [c for c in df.columns if df[c].dtype == "object"]
    if not object_cols:
        raise ValueError("Could not detect review text column.")
    return max(object_cols, key=lambda c: df[c].astype(str).str.len().mean())


def detect_label_columns(df: pd.DataFrame, text_column: str) -> list[str]:
    labels = []
    for col in df.columns:
        if col == text_column or str(col).lower() in IGNORE_COLUMNS:
            continue
        numeric = pd.to_numeric(df[col], errors="coerce").dropna()
        if numeric.empty:
            continue
        values = set(numeric.astype(int).unique().tolist())
        if values and values.issubset({-2, -1, 0, 1}):
            labels.append(str(col))
    if not labels:
        raise ValueError("No ASAP aspect label columns were detected.")
    return labels


def build_pipeline() -> Pipeline:
    features = FeatureUnion([
        ("word", TfidfVectorizer(
            tokenizer=jieba_tokenize,
            token_pattern=None,
            ngram_range=(1, 2),
            min_df=2,
            max_df=0.98,
            sublinear_tf=True,
            max_features=25_000,
        )),
        ("char", TfidfVectorizer(
            analyzer="char",
            ngram_range=(2, 4),
            min_df=2,
            max_df=0.995,
            sublinear_tf=True,
            max_features=25_000,
        )),
    ])
    clf = OneVsRestClassifier(
        LogisticRegression(
            C=2.0,
            class_weight="balanced",
            solver="liblinear",
            max_iter=500,
            random_state=42,
        ),
        n_jobs=-1,
    )
    return Pipeline([("tfidf", features), ("clf", clf)])


def load(path: Path) -> pd.DataFrame:
    return pd.read_csv(path, encoding="utf-8-sig")


def to_negative_targets(df: pd.DataFrame, labels: list[str]) -> np.ndarray:
    # ASAP polarity: 1 positive, 0 neutral, -1 negative, -2 not mentioned.
    return np.column_stack([(pd.to_numeric(df[c], errors="coerce").fillna(-2).astype(int) == -1).astype(int) for c in labels])


def main() -> int:
    train_path = DATA_DIR / "train.csv"
    dev_path = DATA_DIR / "dev.csv"
    test_path = DATA_DIR / "test.csv"
    if not train_path.exists() or not dev_path.exists():
        print("ASAP data not found. Run: python scripts/download_asap.py")
        return 1

    print("[1/6] reading data")
    train = load(train_path)
    dev = load(dev_path)
    test = load(test_path) if test_path.exists() else None

    text_column = detect_text_column(train)
    label_columns = detect_label_columns(train, text_column)
    print(f"text column: {text_column}")
    print(f"aspect columns ({len(label_columns)}): {label_columns}")

    X_train = train[text_column].fillna("").map(clean_text).tolist()
    X_dev = dev[text_column].fillna("").map(clean_text).tolist()
    y_train = to_negative_targets(train, label_columns)
    y_dev = to_negative_targets(dev, label_columns)

    positive_support = y_train.sum(axis=0)
    usable_idx = [i for i, n in enumerate(positive_support) if int(n) >= 5]
    if len(usable_idx) != len(label_columns):
        dropped = [label_columns[i] for i in range(len(label_columns)) if i not in usable_idx]
        print(f"warning: dropping labels with <5 negative training examples: {dropped}")
        label_columns = [label_columns[i] for i in usable_idx]
        y_train = y_train[:, usable_idx]
        y_dev = y_dev[:, usable_idx]

    print("[2/6] building TF-IDF + Logistic Regression pipeline")
    model = build_pipeline()

    print(f"[3/6] training on {len(X_train):,} reviews")
    model.fit(X_train, y_train)

    print(f"[4/6] evaluating on {len(X_dev):,} validation reviews")
    pred = model.predict(X_dev)
    metrics = {
        "dataset": "Meituan-Dianping ASAP",
        "task": "negative aspect multi-label classification",
        "train_rows": len(X_train),
        "dev_rows": len(X_dev),
        "label_count": len(label_columns),
        "micro_precision": float(precision_score(y_dev, pred, average="micro", zero_division=0)),
        "micro_recall": float(recall_score(y_dev, pred, average="micro", zero_division=0)),
        "micro_f1": float(f1_score(y_dev, pred, average="micro", zero_division=0)),
        "macro_f1": float(f1_score(y_dev, pred, average="macro", zero_division=0)),
        "hamming_loss": float(hamming_loss(y_dev, pred)),
    }

    report = classification_report(
        y_dev,
        pred,
        target_names=label_columns,
        zero_division=0,
    )
    print(report)
    print(json.dumps(metrics, ensure_ascii=False, indent=2))

    print("[5/6] saving model and metrics")
    joblib.dump(model, MODEL_PATH, compress=3)
    meta = {
        "model_name": "tfidf-logreg-asap-v1",
        "text_column": text_column,
        "label_columns": label_columns,
        "trained_rows": len(X_train),
        "validation_rows": len(X_dev),
        "target_rule": "1 if ASAP aspect polarity == -1 else 0",
        "dataset_source": "https://github.com/Meituan-Dianping/asap",
    }
    META_PATH.write_text(json.dumps(meta, ensure_ascii=False, indent=2), encoding="utf-8")
    METRICS_PATH.write_text(json.dumps(metrics, ensure_ascii=False, indent=2), encoding="utf-8")
    REPORT_PATH.write_text(report, encoding="utf-8")

    if test is not None:
        print(f"[6/6] test file found ({len(test):,} rows); kept for future final evaluation/analysis")
    else:
        print("[6/6] no test file, done")

    print(f"model: {MODEL_PATH}")
    print(f"metrics: {METRICS_PATH}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
