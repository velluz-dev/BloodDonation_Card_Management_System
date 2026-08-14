@REM ---------------------------------------------------------------------------
@REM Serves this folder over HTTP and opens it.
@REM
@REM Needed because browsers refuse to load JavaScript modules over file://,
@REM so double-clicking index.html shows an empty stage. jwebserver ships with
@REM the JDK (18 and newer), so there is nothing extra to install.
@REM ---------------------------------------------------------------------------
@echo off
setlocal

where jwebserver >nul 2>&1
if errorlevel 1 (
    echo jwebserver was not found on PATH.
    echo It ships with JDK 18 and newer - check that your JDK's bin directory is on PATH.
    echo.
    echo Alternatively, from this folder run:  python -m http.server 8000
    pause
    exit /b 1
)

echo Serving the donor card model at http://localhost:8000/
echo Press Ctrl+C to stop.
start "" http://localhost:8000/
jwebserver -p 8000 -d "%~dp0."
