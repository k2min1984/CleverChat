@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\start-local.ps1" %*
set "CLEVERCHAT_EXIT=%ERRORLEVEL%"
if not "%CLEVERCHAT_EXIT%"=="0" (
  echo.
  echo Setup stopped. Read the message above and README.md, then retry.
  pause
)
exit /b %CLEVERCHAT_EXIT%
