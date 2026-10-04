# Final portfolio cleanup

## Added

- `docs/verified-run.md`: separates verified facts from planned improvements.
- `docs/verified_metrics.json`: stores the real validation metrics from the 2026-10-04 local run.
- `docs/resume-project.md`: resume wording for backend/software and digital/data roles.
- `docs/interview-guide.md`: concepts and interview questions the project owner should understand.
- `docs/github-upload.md`: GitHub repository metadata and push instructions.

## Changed

- README rewritten around the verified backend architecture and actual ML metrics.
- ASAP label mapping updated to cover the official `Category#Aspect` names and return Chinese business labels.
- `docker-compose.yml` now contains only the MySQL service actually relevant to the current optional persistence profile; Redis was removed to avoid implying unimplemented cache integration.

## Verified

- Python source files compile.
- Chinese label mapping unit smoke test passes for representative ASAP labels.
- The core Java/Python workflow and ML metrics are documented from the user's real local run.
