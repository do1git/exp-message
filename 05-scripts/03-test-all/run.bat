@echo off
REM PowerShell/cmd에서 run.sh를 bash로 실행
REM 사용: run.bat [--except-integration]

set "SCRIPT_DIR=%~dp0"
set "RUN_SH=%SCRIPT_DIR%run.sh"

REM bash 경로: PATH 우선, 없으면 Git 기본 경로
where bash >nul 2>&1
if %ERRORLEVEL% equ 0 (
  bash "%RUN_SH%" %*
) else (
  if exist "C:\Program Files\Git\bin\bash.exe" (
    "C:\Program Files\Git\bin\bash.exe" "%RUN_SH%" %*
  ) else if exist "C:\Program Files (x86)\Git\bin\bash.exe" (
    "C:\Program Files (x86)\Git\bin\bash.exe" "%RUN_SH%" %*
  ) else (
    echo Git Bash를 찾을 수 없습니다. PATH에 bash를 추가하거나 Git for Windows를 설치하세요.
    exit /b 1
  )
)
exit /b %ERRORLEVEL%
