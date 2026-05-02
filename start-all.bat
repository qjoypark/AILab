@echo off
setlocal

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-all.ps1" %*
set "EXIT_CODE=%ERRORLEVEL%"

if not "%EXIT_CODE%"=="0" (
  echo.
  echo Start failed. Check the messages above or logs under .\logs.
  pause
  exit /b %EXIT_CODE%
)

echo.
echo Press any key to close this window. Services will continue running in the background.
pause >nul
