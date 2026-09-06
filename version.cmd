@echo off
chcp 65001 >nul
setlocal EnableExtensions EnableDelayedExpansion

REM =========================================================
REM NAV M2M Schema Explorer / M2M XML Editor
REM Automatikus verzioelemzo inditasa Windows alatt
REM =========================================================

set "PROJECT_DIR=%~dp0"
if "%PROJECT_DIR:~-1%"=="\" set "PROJECT_DIR=%PROJECT_DIR:~0,-1%"

set "VERSION_CLASS=hu.gov.nav.xsdparsertool.versioning.VersioningTool"

set "VERSION_SOURCE=%PROJECT_DIR%\nav-xsd-parser-tool-versioning\src\main\java\hu\gov\nav\xsdparsertool\versioning\VersioningTool.java"
set "VERSION_BUILD_DIR=%PROJECT_DIR%\target\versioning-bootstrap"

echo.
echo Verzioelemzes inditasa...
echo Projekt: %PROJECT_DIR%
echo.

if not exist "%VERSION_SOURCE%" (
    echo HIBA: A verzioelemzo forrasa nem talalhato:
    echo %VERSION_SOURCE%
    exit /b 1
)

where javac >nul 2>nul
if errorlevel 1 (
    echo HIBA: A javac nincs a PATH-ban.
    echo JDK 21 szukseges.
    exit /b 1
)

where java >nul 2>nul
if errorlevel 1 (
    echo HIBA: A java nincs a PATH-ban.
    echo JDK 21 szukseges.
    exit /b 1
)

if exist "%VERSION_BUILD_DIR%" (
    rmdir /s /q "%VERSION_BUILD_DIR%"
)

mkdir "%VERSION_BUILD_DIR%"
if errorlevel 1 (
    echo HIBA: Nem sikerult letrehozni a verzioelemzo build konyvtarat:
    echo %VERSION_BUILD_DIR%
    exit /b 1
)

javac -encoding UTF-8 -d "%VERSION_BUILD_DIR%" "%VERSION_SOURCE%"
if errorlevel 1 (
    echo HIBA: A verzioelemzo forditasa sikertelen.
    exit /b 1
)

java -cp "%VERSION_BUILD_DIR%" %VERSION_CLASS% --repo="%PROJECT_DIR%" %*
set "RC=%ERRORLEVEL%"

if not "%RC%"=="0" (
    echo HIBA: Az automatikus verzioelemzes sikertelen.
    exit /b %RC%
)

echo.
echo Verzioelemzes sikeresen befejezodott.
echo.

exit /b 0