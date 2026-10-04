from fastapi import FastAPI
from pydantic import BaseModel, Field

from .analyzer import analyze_reviews
from .model_utils import registry

app = FastAPI(
    title="Review Insight NLP Service",
    version="0.2.0",
    description="Multi-label negative-aspect review analyzer. Uses TF-IDF + Logistic Regression after training.",
)


class ReviewItem(BaseModel):
    id: int | None = None
    text: str


class AnalyzeRequest(BaseModel):
    reviews: list[ReviewItem] = Field(default_factory=list)


@app.get("/health")
def health():
    return {
        "status": "ok",
        "service": "review-insight-nlp",
        "model": registry.info()["name"],
        "ml_model_ready": registry.available(),
    }


@app.get("/model-info")
def model_info():
    return registry.info()


@app.post("/reload-model")
def reload_model():
    loaded = registry.load(force=True)
    return {"loaded": loaded, "model": registry.info()}


@app.post("/analyze")
def analyze(request: AnalyzeRequest):
    payload = [item.model_dump() for item in request.reviews]
    return analyze_reviews(payload)
