@echo off
setlocal
cd /d "%~dp0.."
if not exist ".venv\Scripts\python.exe" (
  echo Virtual environment not found. Run scripts\phase2_setup.bat first.
  pause
  exit /b 1
)
call .venv\Scripts\activate.bat
uvicorn app.main:app --reload --port 8001
