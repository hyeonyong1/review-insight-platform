from __future__ import annotations

from collections import Counter
from typing import Iterable

from .model_utils import pretty_label, registry

# Phase-1 fallback. The service uses this only before the real model is trained.
ISSUE_KEYWORDS = {
    "商品质量": ["质量", "做工", "开线", "破洞", "掉色", "脱胶", "断裂", "异味", "坏了", "损坏", "瑕疵"],
    "服务售后": ["客服", "售后", "服务", "态度", "回复", "退货", "退款", "不理", "没人处理"],
    "物流包装": ["物流", "快递", "配送", "包装", "慢", "压扁", "破损", "发货"],
    "价格性价比": ["价格", "贵", "便宜", "性价比", "优惠", "折扣", "不值"],
    "货不对板": ["图片", "实物", "不一样", "不符", "宣传", "假货", "颜色不对", "尺寸不符", "货不对板"],
    "使用体验": ["体验", "不舒服", "难用", "异味", "噪音", "拥挤", "不好用", "不方便"],
}

POSITIVE_HINTS = {"很好", "不错", "很快", "满意", "划算", "方便", "热情", "干净", "好吃", "推荐", "到位"}
NEGATIVE_HINTS = {"差", "慢", "不", "坏", "破", "贵", "失望", "恶心", "难", "脏", "吵", "久", "一般"}


def _rule_detect(text: str) -> list[str]:
    """Improved transparent fallback; real project mode uses the trained model."""
    text = text or ""
    labels: list[str] = []
    has_negative = any(x in text for x in NEGATIVE_HINTS)
    has_positive = any(x in text for x in POSITIVE_HINTS)
    for label, keywords in ISSUE_KEYWORDS.items():
        if not any(keyword in text for keyword in keywords):
            continue
        # Obvious praise such as “物流很快”“质量不错” should not become a complaint.
        if has_positive and not has_negative:
            continue
        labels.append(label)
    return labels


def _ml_predict(texts: list[str]) -> list[list[str]]:
    registry.load()
    model = registry.model
    meta = registry.meta
    raw = model.predict(texts)
    label_columns = meta["label_columns"]
    output: list[list[str]] = []
    for row in raw:
        labels = [pretty_label(label_columns[i]) for i, value in enumerate(row) if int(value) == 1]
        output.append(labels)
    return output


def extract_keyword_counts(texts: Iterable[str], top_k: int = 15) -> list[dict]:
    """Fast keyword summary for the API response.

    The classification model is ML-based after training; this counter is intentionally
    lightweight because it runs online for every analysis request.
    """
    vocabulary = {keyword for keywords in ISSUE_KEYWORDS.values() for keyword in keywords}
    counts = Counter()
    for text in texts:
        for keyword in vocabulary:
            n = (text or "").count(keyword)
            if n:
                counts[keyword] += n
    return [{"word": word, "count": count} for word, count in counts.most_common(top_k)]


def analyze_reviews(reviews: list[dict]) -> dict:
    texts = [str(review.get("text", "")) for review in reviews]
    use_ml = registry.available()
    predicted = _ml_predict(texts) if use_ml else [_rule_detect(text) for text in texts]

    issue_counts = Counter()
    samples = []
    for review, text, issues in zip(reviews, texts, predicted):
        issue_counts.update(issues)
        if issues and len(samples) < 10:
            samples.append({"id": review.get("id"), "text": text, "issues": issues})

    info = registry.info()
    return {
        "total": len(reviews),
        "issue_counts": dict(issue_counts),
        "top_keywords": extract_keyword_counts(texts),
        "samples": samples,
        "model": info["name"],
        "model_mode": "ml" if use_ml else "fallback",
    }
