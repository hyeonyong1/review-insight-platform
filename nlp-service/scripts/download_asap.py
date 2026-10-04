from __future__ import annotations

from pathlib import Path
import sys
import requests

BASE_DIR = Path(__file__).resolve().parents[1]
DATA_DIR = BASE_DIR / "data" / "asap"
DATA_DIR.mkdir(parents=True, exist_ok=True)

BASE = "https://raw.githubusercontent.com/Meituan-Dianping/asap/refs/heads/master/data"
FILES = ["train.csv", "dev.csv", "test.csv"]


def download(name: str) -> Path:
    target = DATA_DIR / name
    if target.exists() and target.stat().st_size > 100_000:
        print(f"[skip] {name} already exists: {target}")
        return target
    url = f"{BASE}/{name}"
    print(f"[download] {url}")
    with requests.get(url, stream=True, timeout=60) as response:
        response.raise_for_status()
        total = int(response.headers.get("content-length", 0))
        current = 0
        with target.open("wb") as f:
            for chunk in response.iter_content(chunk_size=1024 * 1024):
                if not chunk:
                    continue
                f.write(chunk)
                current += len(chunk)
                if total:
                    print(f"  {current / 1024 / 1024:.1f}/{total / 1024 / 1024:.1f} MB", end="\r")
    print(f"\n[ok] {name}: {target.stat().st_size / 1024 / 1024:.1f} MB")
    return target


if __name__ == "__main__":
    try:
        for file_name in FILES:
            download(file_name)
    except Exception as exc:
        print(f"[error] download failed: {exc}")
        print("You can also manually download train.csv/dev.csv/test.csv from the official ASAP GitHub repo into nlp-service/data/asap/.")
        sys.exit(1)
