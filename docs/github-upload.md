# GitHub upload

The repository is already prepared to be pushed as a normal Git repository.

Recommended repository name:

```text
review-insight-platform
```

Recommended description:

```text
Spring Boot + FastAPI review analysis platform with async jobs and a reproducible TF-IDF multi-label NLP baseline.
```

Suggested topics:

```text
spring-boot, fastapi, nlp, tfidf, logistic-regression, multi-label-classification, java, python
```

## Files intentionally excluded by `.gitignore`

- `.venv/`
- Java `target/`
- ASAP CSV files
- generated `.joblib` model
- local generated metrics/results

The repository contains download/training scripts instead, so others can reproduce the model without committing large artifacts.

## Standard commands

```bash
git init
git add .
git commit -m "feat: build review insight platform with Spring Boot and FastAPI"
git branch -M main
git remote add origin <YOUR_GITHUB_REPOSITORY_URL>
git push -u origin main
```

Do not commit GitHub tokens or passwords into the repository.
