from __future__ import annotations

from pathlib import Path
import json
import threading
from typing import Any

import joblib

BASE_DIR = Path(__file__).resolve().parents[1]
MODEL_DIR = BASE_DIR / "models"
MODEL_PATH = MODEL_DIR / "asap_negative_aspect_model.joblib"
META_PATH = MODEL_DIR / "model_meta.json"
METRICS_PATH = BASE_DIR / "results" / "metrics.json"

# ASAP official labels use names such as "Service#Hospitality".  The project
# also keeps the earlier snake_case aliases so old model metadata remains
# readable if the repository is upgraded in place.
LABEL_ZH = {
    # official ASAP headers
    "Location#Transportation": "位置-交通便利",
    "Location#Downtown": "位置-商圈距离",
    "Location#Easy to find": "位置-是否易找",
    "Service#Queue": "服务-排队等候",
    "Service#Hospitality": "服务-人员态度",
    "Service#Parking": "服务-停车便利",
    "Service#Timely": "服务-上菜速度",
    "Price#Level": "价格-价格水平",
    "Price#Cost effective": "价格-性价比",
    "Price#Cost-effective": "价格-性价比",
    "Price#Discount": "价格-折扣力度",
    "Ambience#Decoration": "环境-装修",
    "Ambience#Noise": "环境-噪音",
    "Ambience#Space": "环境-空间",
    "Ambience#Sanitary": "环境-卫生",
    "Food#Portion": "菜品-分量",
    "Food#Taste": "菜品-口味",
    "Food#Appearance": "菜品-外观",
    "Food#Recommend": "菜品-推荐度",
    # backwards-compatible aliases
    "location_traffic": "位置-交通便利",
    "location_distance_from_business_district": "位置-商圈距离",
    "location_easy_to_find": "位置-是否易找",
    "service_wait_time": "服务-排队等候",
    "service_waiters_attitude": "服务-人员态度",
    "service_parking_convenience": "服务-停车便利",
    "service_serving_speed": "服务-上菜速度",
    "price_level": "价格-价格水平",
    "price_cost_effective": "价格-性价比",
    "price_discount": "价格-折扣力度",
    "environment_decoration": "环境-装修",
    "environment_noise": "环境-噪音",
    "environment_space": "环境-空间",
    "environment_cleaness": "环境-卫生",
    "dish_portion": "菜品-分量",
    "dish_taste": "菜品-口味",
    "dish_look": "菜品-外观",
    "dish_recommendation": "菜品-推荐度",
}


def pretty_label(label: str) -> str:
    if label in LABEL_ZH:
        return LABEL_ZH[label]
    return label.replace("_", "-")


class ModelRegistry:
    """Thread-safe lazy loader for the trained sklearn pipeline."""

    def __init__(self) -> None:
        self._lock = threading.Lock()
        self._model: Any | None = None
        self._meta: dict[str, Any] = {}
        self._loaded_mtime: float | None = None

    def available(self) -> bool:
        return MODEL_PATH.exists() and META_PATH.exists()

    def load(self, force: bool = False) -> bool:
        if not self.available():
            return False

        mtime = MODEL_PATH.stat().st_mtime
        if not force and self._model is not None and self._loaded_mtime == mtime:
            return True

        with self._lock:
            if not force and self._model is not None and self._loaded_mtime == mtime:
                return True
            self._model = joblib.load(MODEL_PATH)
            self._meta = json.loads(META_PATH.read_text(encoding="utf-8"))
            self._loaded_mtime = mtime
        return True

    @property
    def model(self):
        if self._model is None:
            self.load()
        return self._model

    @property
    def meta(self) -> dict[str, Any]:
        if not self._meta:
            self.load()
        return self._meta

    def info(self) -> dict[str, Any]:
        if not self.available():
            return {
                "available": False,
                "name": "rule-baseline-v1",
                "type": "fallback rule baseline",
                "message": "Run scripts/phase2_setup.bat to download ASAP and train the ML model.",
            }
        self.load()
        metrics = {}
        if METRICS_PATH.exists():
            try:
                metrics = json.loads(METRICS_PATH.read_text(encoding="utf-8"))
            except Exception:
                metrics = {}
        return {
            "available": True,
            "name": self.meta.get("model_name", "tfidf-logreg-asap-v1"),
            "type": "TF-IDF + One-vs-Rest Logistic Regression",
            "text_column": self.meta.get("text_column"),
            "label_count": len(self.meta.get("label_columns", [])),
            "trained_rows": self.meta.get("trained_rows"),
            "metrics": {
                k: metrics.get(k)
                for k in ["micro_f1", "macro_f1", "micro_precision", "micro_recall", "hamming_loss"]
                if k in metrics
            },
        }


registry = ModelRegistry()
