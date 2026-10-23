@echo off
setlocal EnableExtensions DisableDelayedExpansion
set "PROJECT_DIR=%~dp0"
call "%PROJECT_DIR%resolve-build-version.cmd"
if errorlevel 1 exit /b 1

echo.
echo Maven release version: %VERSION_RELEASE%
echo Full release: %FULL_RELEASE%
echo Build timestamp: %VERSION_TIMESTAMP%
echo.
cd /d "%PROJECT_DIR%"
call mvn clean package -Drevision=%VERSION_RELEASE% -Dapp.release.version=%VERSION_RELEASE% -Dapp.build.timestamp=%VERSION_TIMESTAMP% -Dapp.full.release=%FULL_RELEASE% %*
exit /b %errorlevel%
