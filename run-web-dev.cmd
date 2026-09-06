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
echo [1/2] Saját modulok telepitese a lokalis Maven repositoryba: %DEV_REVISION%
cd /d "%PROJECT_DIR%"
call mvn -U -pl :nav-xsd-parser-tool-web -am install -DskipTests -Drevision=%DEV_REVISION% -Dapp.release.version=%VERSION_RELEASE% -Dapp.build.timestamp=%VERSION_TIMESTAMP%
if errorlevel 1 exit /b 1

echo.
echo [2/2] Web alkalmazas inditasa Spring Boot-tal...
call mvn -f "%PROJECT_DIR%nav-xsd-parser-tool-web\pom.xml" spring-boot:run -Drevision=%DEV_REVISION% -Dapp.release.version=%VERSION_RELEASE% -Dapp.build.timestamp=%VERSION_TIMESTAMP% %*
exit /b %errorlevel%
