@echo off
chcp 65001 >nul
setlocal EnableExtensions EnableDelayedExpansion

REM =========================================================
REM M2M XML EDITOR - Windows installer build
REM WiX nelkul: jpackage app-image + Inno Setup
REM =========================================================

set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%\..\.."
if errorlevel 1 (
    echo ERROR: Cannot resolve project directory.
    exit /b 1
)
set "PROJECT_DIR=%CD%"
popd

if not exist "%PROJECT_DIR%\version.cmd" (
    echo HIBA: Hianyzik a projekt gyokerbol a version.cmd.
    exit /b 1
)

set "VERSION_ARGS="
if defined VERSION_BUMP_OVERRIDE set "VERSION_ARGS=--override=%VERSION_BUMP_OVERRIDE%"
call "%PROJECT_DIR%\version.cmd" %VERSION_ARGS%
if errorlevel 1 (
    echo HIBA: Az automatikus verzioelemzes sikertelen.
    exit /b 1
)
for /f "usebackq tokens=1,* delims==" %%A in ("%PROJECT_DIR%\target\generated-version\build-version.env") do set "%%A=%%B"
if not defined VERSION_RELEASE (
    echo HIBA: A generalt release verzio nem olvashato.
    exit /b 1
)
set "APP_NUMERIC_VERSION=%VERSION_RELEASE%"
set "BUILD_TIMESTAMP=%VERSION_TIMESTAMP%"
set "APP_VERSION=%VERSION_RELEASE%-%VERSION_TIMESTAMP%"

set "BUILD_DIR=%PROJECT_DIR%\installer\windows\build"
set "STAGE_DIR=%BUILD_DIR%\stage"
set "INPUT_DIR=%STAGE_DIR%\input"
set "APP_IMAGE_DIR=%STAGE_DIR%\app-image"
set "DIST_DIR=%BUILD_DIR%\dist"

echo.
echo [1/9] Projekt gyoker:
echo %PROJECT_DIR%
echo.
echo Build verzio: %APP_VERSION%
echo JPackage verzio: %APP_NUMERIC_VERSION%
echo Build timestamp: %BUILD_TIMESTAMP%
echo.

if not exist "%PROJECT_DIR%\pom.xml" (
    echo HIBA: Nem talalhato pom.xml a projekt gyokerben:
    echo %PROJECT_DIR%
    exit /b 1
)

where mvn >nul 2>nul
if errorlevel 1 (
    echo HIBA: A Maven nincs a PATH-ban.
    exit /b 1
)

where jpackage >nul 2>nul
if errorlevel 1 (
    echo HIBA: A jpackage nincs a PATH-ban.
    echo Telepits JDK 21-et, es tedd a PATH-ba.
    exit /b 1
)

set "ISCC_EXE=%ProgramFiles(x86)%\Inno Setup 6\ISCC.exe"
if not exist "%ISCC_EXE%" set "ISCC_EXE=%ProgramFiles%\Inno Setup 6\ISCC.exe"
if not exist "%ISCC_EXE%" goto :no_iscc

set "CSC_EXE=%WINDIR%\Microsoft.NET\Framework64\v4.0.30319\csc.exe"
if not exist "%CSC_EXE%" set "CSC_EXE=%WINDIR%\Microsoft.NET\Framework\v4.0.30319\csc.exe"
if not exist "%CSC_EXE%" goto :no_csc

if exist "%BUILD_DIR%" rmdir /s /q "%BUILD_DIR%"
mkdir "%INPUT_DIR%"
mkdir "%APP_IMAGE_DIR%"
mkdir "%DIST_DIR%"

echo [2/9] Maven build
cd /d "%PROJECT_DIR%"
call mvn clean package -DskipTests -Drevision=%VERSION_RELEASE% -Dapp.release.version=%VERSION_RELEASE% -Dapp.build.timestamp=%VERSION_TIMESTAMP%
if errorlevel 1 (
    echo HIBA: Maven build sikertelen.
    exit /b 1
)

echo [3/9] Futtathato JAR keresese

set "APP_JAR="
for %%F in ("%PROJECT_DIR%\nav-xsd-parser-tool-web\target\*.jar") do (
    echo %%~nxF | find /I "sources" >nul
    if errorlevel 1 (
        echo %%~nxF | find /I "javadoc" >nul
        if errorlevel 1 (
            echo %%~nxF | find /I "original-" >nul
            if errorlevel 1 (
                set "APP_JAR=%%~fF"
                goto :jar_found
            )
        )
    )
)

:jar_found
if "%APP_JAR%"=="" (
    echo HIBA: Nem talaltam futtathato JAR-t itt:
    echo %PROJECT_DIR%\nav-xsd-parser-tool-web\target
    exit /b 1
)

echo Megtalalt JAR:
echo %APP_JAR%

echo [4/9] Bemeneti allomanyok elokeszitese
copy "%APP_JAR%" "%INPUT_DIR%\app.jar" >nul
if errorlevel 1 (
    echo HIBA: app.jar masolasa sikertelen.
    exit /b 1
)

if exist "%PROJECT_DIR%\config" (
    xcopy "%PROJECT_DIR%\config" "%INPUT_DIR%\config\" /E /I /Y >nul
)

if exist "%PROJECT_DIR%\README.md" (
    copy "%PROJECT_DIR%\README.md" "%INPUT_DIR%\README.md" >nul
)
if exist "%PROJECT_DIR%\LICENSE" copy "%PROJECT_DIR%\LICENSE" "%INPUT_DIR%\LICENSE" >nul
if exist "%PROJECT_DIR%\README-LICENC.md" copy "%PROJECT_DIR%\README-LICENC.md" "%INPUT_DIR%\README-LICENC.md" >nul
if exist "%PROJECT_DIR%\licenses" xcopy "%PROJECT_DIR%\licenses" "%INPUT_DIR%\licenses\" /E /I /Y >nul

echo [5/9] App-image letrehozasa jpackage-gel
jpackage ^
  --type app-image ^
  --name "M2M XML EDITOR" ^
  --dest "%APP_IMAGE_DIR%" ^
  --input "%INPUT_DIR%" ^
  --main-jar "app.jar" ^
  --main-class org.springframework.boot.loader.launch.JarLauncher ^
  --icon "%PROJECT_DIR%\installer\windows\app.ico" ^
  --java-options "-Dfile.encoding=UTF-8" ^
  --app-version "%APP_NUMERIC_VERSION%"
  
if errorlevel 1 (
    echo HIBA: jpackage sikertelen.
    echo Ha Spring Boot launch osztaly elter, probald ezt:
    echo   org.springframework.boot.loader.JarLauncher
    exit /b 1
)

set "APP_IMAGE_PATH=%APP_IMAGE_DIR%\M2M XML EDITOR"
if not exist "%APP_IMAGE_PATH%" (
    echo HIBA: Az app-image kimeneti mappa nem talalhato:
    echo %APP_IMAGE_PATH%
    exit /b 1
)

echo [6/9] Payload telepito EXE es kulon telepitesi adatfajl keszitese
"%ISCC_EXE%" ^
  "/DMyAppVersion=%APP_VERSION%" ^
  "/DMyNumericVersion=%APP_NUMERIC_VERSION%" ^
  "/DMyBuildTimestamp=%BUILD_TIMESTAMP%" ^
  "/DMySourceDir=%APP_IMAGE_PATH%" ^
  "/DMyOutputDir=%DIST_DIR%" ^
  "/DMyProjectDir=%PROJECT_DIR%" ^
  "%PROJECT_DIR%\installer\windows\m2m-xml-editor.iss"

if errorlevel 1 (
    echo HIBA: Inno Setup build sikertelen.
    exit /b 1
)

echo [7/9] Gyors Windows splash bootstrapper forditasa
set "PAYLOAD_EXE_NAME=M2M-XML-EDITOR-Payload-Setup-%APP_VERSION%.exe"
set "PAYLOAD_EXE=%DIST_DIR%\%PAYLOAD_EXE_NAME%"
if not exist "%PAYLOAD_EXE%" (
    echo HIBA: A payload telepito EXE nem jott letre:
    echo %PAYLOAD_EXE%
    exit /b 1
)

"%CSC_EXE%" /nologo /target:winexe /optimize+ /platform:anycpu ^
  /reference:System.dll ^
  /reference:System.Core.dll ^
  /reference:System.Drawing.dll ^
  /reference:System.Windows.Forms.dll ^
  /win32icon:"%PROJECT_DIR%\installer\windows\app.ico" ^
  /out:"%DIST_DIR%\M2M-XML-EDITOR-Setup-%APP_VERSION%.exe" ^
  "%PROJECT_DIR%\installer\windows\bootstrapper\M2MXmlEditorBootstrapper.cs"

if errorlevel 1 (
    echo HIBA: A gyors splash bootstrapper forditasa sikertelen.
    exit /b 1
)

echo [8/9] Telepito kimenetenek ellenorzese
set "SETUP_EXE=%DIST_DIR%\M2M-XML-EDITOR-Setup-%APP_VERSION%.exe"
if not exist "%SETUP_EXE%" (
    echo HIBA: A kulso splash bootstrapper EXE nem jott letre:
    echo %SETUP_EXE%
    exit /b 1
)

set "PAYLOAD_FOUND="
for %%F in ("%DIST_DIR%\M2M-XML-EDITOR-Payload-Setup-%APP_VERSION%-*.bin") do (
    if exist "%%~fF" set "PAYLOAD_FOUND=1"
)
if not defined PAYLOAD_FOUND (
    echo HIBA: A telepito nagy adatfajlja nem jott letre.
    echo A Setup EXE onmagaban nem terjesztheto.
    exit /b 1
)

> "%DIST_DIR%\TELEPITES-HU.txt" (
    echo M2M XML EDITOR telepitese
    echo.
    echo 1. Tartsa a bootstrapper EXE-t, a Payload Setup EXE-t es az összes BIN fájlt ugyanabban a mappában.
    echo 2. Indítsa el: M2M-XML-EDITOR-Setup-%APP_VERSION%.exe
    echo 3. Ne nevezze át es ne törölje a Payload Setup EXE-t vagy a BIN fájlokat.
    echo.
    echo A kis Windows bootstrapper azonnal splash ablakot mutat, majd elinditja a tenyleges telepitot.
)


echo [9/9] Kesz
echo.
echo Telepitocsomag helye:
echo %DIST_DIR%
echo.
echo FONTOS: a bootstrapper EXE, a Payload Setup EXE es az osszes BIN fajl egyutt terjesztendo.
echo A felhasznalo csak a M2M-XML-EDITOR-Setup-%APP_VERSION%.exe gyors bootstrappert inditja el.
echo.
exit /b 0

:no_iscc
echo HIBA: Nem talalhato az Inno Setup Compiler (ISCC.exe).
echo Varhato helyek:
echo   %ProgramFiles(x86)%\Inno Setup 6\ISCC.exe
echo   %ProgramFiles%\Inno Setup 6\ISCC.exe
exit /b 1
:no_csc
echo HIBA: Nem talalhato a .NET Framework C# fordito (csc.exe).
echo Varhato helyek:
echo   %WINDIR%\Microsoft.NET\Framework64\v4.0.30319\csc.exe
echo   %WINDIR%\Microsoft.NET\Framework\v4.0.30319\csc.exe
exit /b 1
