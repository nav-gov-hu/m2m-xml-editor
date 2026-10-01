@echo off
setlocal EnableExtensions
set "PROJECT_DIR=%~dp0"
set "VERSION_ARGS="
if defined VERSION_BUMP_OVERRIDE set "VERSION_ARGS=--override=%VERSION_BUMP_OVERRIDE%"

call "%PROJECT_DIR%version.cmd" %VERSION_ARGS%
if errorlevel 1 exit /b 1

for /f "usebackq tokens=1,* delims==" %%A in ("%PROJECT_DIR%target\generated-version\build-version.env") do set "%%A=%%B"
if not defined VERSION_RELEASE (
  echo HIBA: A generalt verzioszam nem olvashato.
  exit /b 1
)
if not defined VERSION_TIMESTAMP (
  echo HIBA: A generalt build idobelyeg nem olvashato.
  exit /b 1
)

set "FULL_RELEASE=%VERSION_RELEASE%-%VERSION_TIMESTAMP%"
>"%PROJECT_DIR%RELEASE" echo %FULL_RELEASE%
if errorlevel 1 (
  echo HIBA: A RELEASE allomany letrehozasa sikertelen.
  exit /b 1
)

echo.
echo Maven release version: %VERSION_RELEASE%
echo Full release: %FULL_RELEASE%
echo Build timestamp: %VERSION_TIMESTAMP%
echo.
cd /d "%PROJECT_DIR%"
call mvn clean package -Drevision=%VERSION_RELEASE% -Dapp.release.version=%VERSION_RELEASE% -Dapp.build.timestamp=%VERSION_TIMESTAMP% -Dapp.full.release=%FULL_RELEASE% %*
exit /b %errorlevel%
