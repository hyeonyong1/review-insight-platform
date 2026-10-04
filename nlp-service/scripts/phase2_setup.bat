@echo off
setlocal
cd /d "%~dp0.."

echo ==============================================
echo Review Insight Phase 2 - download + train

echo ==============================================

if not exist ".venv\Scripts\python.exe" (
  echo [1/4] Creating virtual environment...
  python -m venv .venv
)

echo [2/4] Installing dependencies...
call .venv\Scripts\activate.bat
python -m pip install --upgrade pip
pip install -r requirements.txt

if errorlevel 1 goto :error

echo [3/4] Downloading official ASAP dataset...
python scripts\download_asap.py
if errorlevel 1 goto :error

echo [4/4] Training TF-IDF + Logistic Regression model...
python scripts\train_asap_model.py
if errorlevel 1 goto :error

echo.
echo ==============================================
echo DONE. Model and metrics have been generated.
echo Start service with: uvicorn app.main:app --reload --port 8001

echo ==============================================
pause
exit /b 0

:error
echo.
echo Phase 2 setup failed. Read the error above.
pause
exit /b 1
