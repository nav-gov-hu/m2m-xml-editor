@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0update-datasource-config.ps1" %*
set EXITCODE=%ERRORLEVEL%
if not "%EXITCODE%"=="0" (
  echo.
  echo A datasource konfiguracio frissitese sikertelen. Hibakod: %EXITCODE%
)
exit /b %EXITCODE%
