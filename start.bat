@echo off
chcp 65001 >nul
setlocal

set "ROOT_DIR=%~dp0"
if "%ROOT_DIR:~-1%"=="\" set "ROOT_DIR=%ROOT_DIR:~0,-1%"

powershell -NoProfile -ExecutionPolicy Bypass -File "%ROOT_DIR%\start-console.ps1"
exit /b %errorlevel%
