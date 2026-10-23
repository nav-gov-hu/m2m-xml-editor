@echo off
REM Resolve the release for both the normal Windows build and installer build.
REM Called scripts receive VERSION_RELEASE, VERSION_TIMESTAMP and FULL_RELEASE.
setlocal EnableExtensions DisableDelayedExpansion
set "PROJECT_DIR=%~dp0"
set "RELEASE_FILE=%PROJECT_DIR%RELEASE"
set "GENERATED_ENV=%PROJECT_DIR%target\generated-version\build-version.env"
set "VERSION_RELEASE="
set "VERSION_TIMESTAMP="
set "FULL_RELEASE="

if exist "%PROJECT_DIR%.git" goto :with_git

echo Git metadata not found. Reading release from RELEASE file.
if not exist "%RELEASE_FILE%" (
    echo HIBA: Git nelkuli buildhez hianyzik a RELEASE fajl.
    exit /b 1
)
set "RELEASE_LINE="
set /p "RELEASE_LINE="<"%RELEASE_FILE%"
if not defined RELEASE_LINE (
    echo HIBA: A RELEASE fajl ures.
    exit /b 1
)
REM Expected: major.minor.patch-YYYYMMDD-HHMMSS, optionally followed by -RCn.
for /f "tokens=1-4 delims=-" %%A in ("%RELEASE_LINE%") do (
    set "VERSION_RELEASE=%%A"
    set "BUILD_DATE=%%B"
    set "BUILD_TIME=%%C"
    set "RELEASE_SUFFIX=%%D"
)
if not defined VERSION_RELEASE goto :invalid_release
if not defined BUILD_DATE goto :invalid_release
if not defined BUILD_TIME goto :invalid_release
set "VERSION_TIMESTAMP=%BUILD_DATE%-%BUILD_TIME%"
set "FULL_RELEASE=%VERSION_RELEASE%-%VERSION_TIMESTAMP%"
if defined RELEASE_SUFFIX set "FULL_RELEASE=%FULL_RELEASE%-%RELEASE_SUFFIX%"
if not "%FULL_RELEASE%"=="%RELEASE_LINE%" goto :invalid_release
REM Enforce basic semantic version and timestamp shape, not merely token count.
echo(%VERSION_RELEASE%| findstr /R "^[0-9][0-9]*\.[0-9][0-9]*\.[0-9][0-9]*$" >nul || goto :invalid_release
echo(%BUILD_DATE%| findstr /R "^[0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]$" >nul || goto :invalid_release
echo(%BUILD_TIME%| findstr /R "^[0-9][0-9][0-9][0-9][0-9][0-9]$" >nul || goto :invalid_release
if defined RELEASE_SUFFIX (
    echo(%RELEASE_SUFFIX%| findstr /R "^RC[0-9][0-9]*$" >nul || goto :invalid_release
)
if defined VERSION_BUMP_OVERRIDE (
    echo HIBA: VERSION_BUMP_OVERRIDE csak Git-repozitoriumban hasznalhato.
    exit /b 1
)
goto :success

:with_git
where git >nul 2>nul
if errorlevel 1 (
    echo HIBA: Git metadata van, de a git parancs nem erheto el.
    exit /b 1
)
set "VERSION_ARGS="
if defined VERSION_BUMP_OVERRIDE set "VERSION_ARGS=--override=%VERSION_BUMP_OVERRIDE%"
call "%PROJECT_DIR%version.cmd" %VERSION_ARGS%
if errorlevel 1 exit /b 1
if not exist "%GENERATED_ENV%" (
    echo HIBA: A verzioelemzo nem hozta letre a build-version.env fajlt.
    exit /b 1
)
for /f "usebackq tokens=1,* delims==" %%A in ("%GENERATED_ENV%") do (
    if "%%A"=="VERSION_RELEASE" set "VERSION_RELEASE=%%B"
    if "%%A"=="VERSION_TIMESTAMP" set "VERSION_TIMESTAMP=%%B"
)
if not defined VERSION_RELEASE goto :invalid_generated
if not defined VERSION_TIMESTAMP goto :invalid_generated
set "FULL_RELEASE=%VERSION_RELEASE%-%VERSION_TIMESTAMP%"
>"%RELEASE_FILE%" echo %FULL_RELEASE%
if errorlevel 1 (
    echo HIBA: A RELEASE fajl irasa sikertelen.
    exit /b 1
)
goto :success

:invalid_generated
echo HIBA: A generalt verzioadatok hianyosak.
exit /b 1
:invalid_release
echo HIBA: Ervenytelen RELEASE formatum: %RELEASE_LINE%
echo Elvart: 4.0.0-20261023-184500 vagy 4.0.0-20261023-184500-RC1
exit /b 1
:success
endlocal & set "VERSION_RELEASE=%VERSION_RELEASE%" & set "VERSION_TIMESTAMP=%VERSION_TIMESTAMP%" & set "FULL_RELEASE=%FULL_RELEASE%"
exit /b 0
