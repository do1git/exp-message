@echo off
REM Monolitic server build and push script (Windows)

setlocal enabledelayedexpansion

REM Script directory
set SCRIPT_DIR=%~dp0

REM Load .env file if exists
if exist "%SCRIPT_DIR%.env" (
    echo Loading environment file: %SCRIPT_DIR%.env
    for /f "usebackq eol=# tokens=1* delims==" %%a in ("%SCRIPT_DIR%.env") do (
        if not "%%a"=="" (
            set "%%a=%%b"
        )
    )
) else if exist "%SCRIPT_DIR%default.env" (
    echo Loading default environment file: %SCRIPT_DIR%default.env
    for /f "usebackq eol=# tokens=1* delims==" %%a in ("%SCRIPT_DIR%default.env") do (
        if not "%%a"=="" (
            set "%%a=%%b"
        )
    )
)

REM Set default values
if "%REGISTRY_HOST%"=="" set REGISTRY_HOST=localhost
if "%REGISTRY_PORT%"=="" set REGISTRY_PORT=5000
if "%IMAGE_NAME%"=="" set IMAGE_NAME=00-monolitic
if "%IMAGE_TAG%"=="" set IMAGE_TAG=latest

REM Project root directory
set PROJECT_ROOT=%SCRIPT_DIR%..\..
set MONOLITIC_DIR=%PROJECT_ROOT%\02-backend\00-monolitic

echo ==========================================
echo Monolitic Server Build and Push
echo ==========================================
echo Registry: %REGISTRY_HOST%:%REGISTRY_PORT%
echo Image: %IMAGE_NAME%:%IMAGE_TAG%
echo Directory: %MONOLITIC_DIR%
echo ==========================================

REM Check directory
if not exist "%MONOLITIC_DIR%" (
    echo Error: Monolitic directory not found: %MONOLITIC_DIR%
    exit /b 1
)

REM Check Dockerfile
if not exist "%MONOLITIC_DIR%\Dockerfile" (
    echo Error: Dockerfile not found: %MONOLITIC_DIR%\Dockerfile
    exit /b 1
)

REM Build image
echo.
echo 1. Building Docker image...
cd /d "%MONOLITIC_DIR%"
docker build -t %IMAGE_NAME%:%IMAGE_TAG% .

REM Tag for registry
set REGISTRY_IMAGE=%REGISTRY_HOST%:%REGISTRY_PORT%/%IMAGE_NAME%:%IMAGE_TAG%
echo.
echo 2. Tagging for registry: %REGISTRY_IMAGE%
docker tag %IMAGE_NAME%:%IMAGE_TAG% %REGISTRY_IMAGE%

REM Push to registry
echo.
echo 3. Pushing to registry...
docker push %REGISTRY_IMAGE%

echo.
echo ==========================================
echo Done!
echo Image: %REGISTRY_IMAGE%
echo ==========================================

endlocal

