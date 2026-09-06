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

set "DEV_REVISION=%VERSION_RELEASE%-SNAPSHOT"
echo.
echo Fejlesztoi Maven revision: %DEV_REVISION%
echo Build timestamp: %VERSION_TIMESTAMP%
echo.
cd /d "%PROJECT_DIR%"
call mvn -U clean install -DskipTests -Drevision=%DEV_REVISION% -Dapp.release.version=%VERSION_RELEASE% -Dapp.build.timestamp=%VERSION_TIMESTAMP% %*
exit /b %errorlevel%
