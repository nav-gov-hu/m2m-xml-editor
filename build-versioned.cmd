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

echo.
echo Maven release version: %VERSION_RELEASE%
echo Build timestamp: %VERSION_TIMESTAMP%
echo.
cd /d "%PROJECT_DIR%"
call mvn clean package -Drevision=%VERSION_RELEASE% -Dapp.release.version=%VERSION_RELEASE% -Dapp.build.timestamp=%VERSION_TIMESTAMP% %*
exit /b %errorlevel%
