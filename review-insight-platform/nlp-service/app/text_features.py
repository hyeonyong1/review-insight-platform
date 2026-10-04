from __future__ import annotations

import re

try:
    import jieba
except ImportError:
    jieba = None


def clean_text(text: str) -> str:
    text = str(text or "").replace("\\n", " ").replace("\n", " ")
    text = re.sub(r"\s+", " ", text)
    return text.strip()


def jieba_tokenize(text: str) -> list[str]:
    cleaned = clean_text(text)
    if jieba is not None:
        return [token.strip() for token in jieba.lcut(cleaned) if token.strip()]
    # Fallback only for development checks; requirements installs jieba in normal use.
    return [ch for ch in cleaned if not ch.isspace()]
