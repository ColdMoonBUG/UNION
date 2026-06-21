@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

set "ROOT_DIR=%~dp0"
if "%ROOT_DIR:~-1%"=="\" set "ROOT_DIR=%ROOT_DIR:~0,-1%"

set "APP_NAME=jusic-serve"
set "LOG_DIR=%ROOT_DIR%\logs"
set "PID_FILE=%LOG_DIR%\%APP_NAME%.pid"

if not exist "%PID_FILE%" (
    echo 未找到 PID 文件: %PID_FILE%
    echo 如果服务已停止，可以忽略。
    exit /b 0
)

set /p PID=<"%PID_FILE%"
if "%PID%"=="" (
    del /f /q "%PID_FILE%" >nul 2>nul
    echo PID 文件为空，已清理。
    exit /b 0
)

powershell -NoProfile -Command "if (Get-Process -Id %PID% -ErrorAction SilentlyContinue) { exit 0 } else { exit 1 }"
if errorlevel 1 (
    del /f /q "%PID_FILE%" >nul 2>nul
    echo %APP_NAME% 未运行，已清理旧 PID 文件。
    exit /b 0
)

taskkill /PID %PID% /T /F >nul 2>nul
del /f /q "%PID_FILE%" >nul 2>nul
echo %APP_NAME% 已停止，PID=%PID%
endlocal
