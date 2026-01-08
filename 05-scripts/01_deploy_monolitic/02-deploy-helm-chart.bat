@echo off
REM Helm chart deployment script for monolitic stack (Windows)

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
if "%RELEASE_NAME%"=="" set RELEASE_NAME=monolitic-stack
if "%NAMESPACE%"=="" set NAMESPACE=default
if "%DEPLOY_ACTION%"=="" set DEPLOY_ACTION=install-or-upgrade

REM Project root directory
set PROJECT_ROOT=%SCRIPT_DIR%..\..
set CHART_DIR=%PROJECT_ROOT%\01-infrastructure\03-stack-monolitic

REM Registry image
set REGISTRY_IMAGE=%REGISTRY_HOST%:%REGISTRY_PORT%\%IMAGE_NAME%

REM Check for remote kubeconfig in same directory
set REMOTE_KUBECONFIG=%SCRIPT_DIR%remote-kubeconfig.yaml

REM Validate and set deployment mode
if "%DEPLOY_MODE%"=="" (
    echo Error: DEPLOY_MODE must be set to 'remote' or 'local'
    echo Please set DEPLOY_MODE in .env file or as environment variable
    exit /b 1
)

if not "%DEPLOY_MODE%"=="remote" if not "%DEPLOY_MODE%"=="local" (
    echo Error: DEPLOY_MODE must be 'remote' or 'local'
    exit /b 1
)

REM Set kubeconfig path based on mode
if "%DEPLOY_MODE%"=="remote" (
    if not exist "%REMOTE_KUBECONFIG%" (
        echo Error: DEPLOY_MODE=remote but remote-kubeconfig.yaml not found
        echo Please run 00-fetch-kubeconfig.sh first
        exit /b 1
    )
    set KUBECONFIG_PATH=%REMOTE_KUBECONFIG%
) else (
    set KUBECONFIG_PATH=
)

echo ==========================================
echo Helm Chart Deployment
echo ==========================================
echo Mode: %DEPLOY_MODE%
echo Action: %DEPLOY_ACTION%
echo Release: %RELEASE_NAME%
echo Namespace: %NAMESPACE%
echo Registry Image: %REGISTRY_IMAGE%:%IMAGE_TAG%
if "%DEPLOY_MODE%"=="remote" (
    echo Kubeconfig: %KUBECONFIG_PATH%
)
echo Chart Directory: %CHART_DIR%
echo ==========================================

REM Check kubectl
where kubectl >nul 2>&1
if errorlevel 1 (
    echo Error: kubectl not found
    exit /b 1
)

REM Check helm
where helm >nul 2>&1
if errorlevel 1 (
    echo Error: helm not found
    exit /b 1
)

REM Check chart directory
if not exist "%CHART_DIR%" (
    echo Error: Chart directory not found: %CHART_DIR%
    exit /b 1
)

REM Update helm dependencies
echo.
echo 1. Updating Helm dependencies...
cd /d "%CHART_DIR%"
helm dependency update

REM Check kubectl
where kubectl >nul 2>&1
if errorlevel 1 (
    echo Error: kubectl not found
    exit /b 1
)

REM Determine action
if "%DEPLOY_ACTION%"=="install-or-upgrade" (
    REM Auto-detect: check if release exists
    if "%DEPLOY_MODE%"=="remote" (
        helm list -n %NAMESPACE% --kubeconfig="%KUBECONFIG_PATH%" | findstr /C:"%RELEASE_NAME%" >nul 2>&1
        if errorlevel 1 (
            set DEPLOY_ACTION=install
        ) else (
            set DEPLOY_ACTION=upgrade
        )
    ) else (
        helm list -n %NAMESPACE% | findstr /C:"%RELEASE_NAME%" >nul 2>&1
        if errorlevel 1 (
            set DEPLOY_ACTION=install
        ) else (
            set DEPLOY_ACTION=upgrade
        )
    )
)

REM Validate action
if not "%DEPLOY_ACTION%"=="install" if not "%DEPLOY_ACTION%"=="upgrade" if not "%DEPLOY_ACTION%"=="delete" (
    echo Error: DEPLOY_ACTION must be 'install', 'upgrade', or 'delete'
    exit /b 1
)

if "%DEPLOY_ACTION%"=="delete" (
    REM Delete release
    echo.
    echo 2. Deleting release...
    if "%DEPLOY_MODE%"=="remote" (
        helm uninstall %RELEASE_NAME% --namespace %NAMESPACE% --kubeconfig="%KUBECONFIG_PATH%" 2>nul || echo Release not found or already deleted
    ) else (
        helm uninstall %RELEASE_NAME% --namespace %NAMESPACE% 2>nul || echo Release not found or already deleted
    )
    echo.
    echo Release '%RELEASE_NAME%' deleted
) else if "%DEPLOY_MODE%"=="remote" (
    REM Remote deployment: Use remote-kubeconfig.yaml from same directory
    echo.
    echo 2. Deploying to remote cluster using remote-kubeconfig.yaml...
    
    if "%DEPLOY_ACTION%"=="install" (
        echo    Installing new release...
        helm install %RELEASE_NAME% . ^
            --namespace %NAMESPACE% ^
            --create-namespace ^
            --kubeconfig="%KUBECONFIG_PATH%" ^
            --set app-monolitic.image.repository=%REGISTRY_IMAGE% ^
            --set app-monolitic.image.tag=%IMAGE_TAG% ^
            --set app-monolitic.image.pullPolicy=IfNotPresent
    ) else (
        echo    Upgrading existing release...
        helm upgrade %RELEASE_NAME% . ^
            --namespace %NAMESPACE% ^
            --kubeconfig="%KUBECONFIG_PATH%" ^
            --set app-monolitic.image.repository=%REGISTRY_IMAGE% ^
            --set app-monolitic.image.tag=%IMAGE_TAG% ^
            --set app-monolitic.image.pullPolicy=IfNotPresent
    )
    
    echo.
    echo 3. Checking deployment status...
    kubectl get pods -n %NAMESPACE% --kubeconfig="%KUBECONFIG_PATH%" -l app.kubernetes.io/instance=%RELEASE_NAME%
) else (
    REM Local deployment
    echo.
    echo 2. Deploying to local cluster...
    
    if "%DEPLOY_ACTION%"=="install" (
        echo    Installing new release...
        helm install %RELEASE_NAME% . ^
            --namespace %NAMESPACE% ^
            --create-namespace ^
            --set app-monolitic.image.repository=%REGISTRY_IMAGE% ^
            --set app-monolitic.image.tag=%IMAGE_TAG% ^
            --set app-monolitic.image.pullPolicy=IfNotPresent
    ) else (
        echo    Upgrading existing release...
        helm upgrade %RELEASE_NAME% . ^
            --namespace %NAMESPACE% ^
            --set app-monolitic.image.repository=%REGISTRY_IMAGE% ^
            --set app-monolitic.image.tag=%IMAGE_TAG% ^
            --set app-monolitic.image.pullPolicy=IfNotPresent
    )
    
    echo.
    echo 3. Checking deployment status...
    kubectl get pods -n %NAMESPACE% -l app.kubernetes.io/instance=%RELEASE_NAME%
)

echo.
echo ==========================================
echo Done!
echo Release: %RELEASE_NAME%
echo Namespace: %NAMESPACE%
echo ==========================================

endlocal

