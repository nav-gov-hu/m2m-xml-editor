# M2M XML EDITOR – Windows telepítő készítése

Ez a dokumentum a projekt aktuális Windows telepítő-build folyamatát írja le. A telepítő a következő lánccal készül:

```text
Maven
  -> Spring Boot futtatható JAR
  -> jpackage app-image
  -> Inno Setup payload telepítő
  -> C# bootstrapper / splash EXE
  -> terjeszthető Windows telepítőcsomag
```

A teljes folyamatot az alábbi script végzi:

```text
installer/windows/build-installer.bat
```

A `.iss` fájlt normál esetben **nem kell kézzel fordítani**, mert a szükséges verzió-, timestamp-, forrás- és kimeneti paramétereket a build script adja át neki.

---

## 1. Előfeltételek

A buildet Windows alatt kell futtatni. A következő eszközök szükségesek.

### Maven

A `mvn` parancsnak elérhetőnek kell lennie a `PATH` változóban.

Ellenőrzés:

```bat
mvn -version
```

### JDK jpackage támogatással

A build script a `jpackage` eszközt használja az alkalmazás Windows app-image előállításához. A projekt jelenlegi buildfolyamata JDK 21-es `jpackage` használatával számol.

Ellenőrzés:

```bat
java -version
jpackage --version
```

A `jpackage` parancsnak elérhetőnek kell lennie a `PATH` változóban.

### Inno Setup 6.2 vagy frisebb (7.x-el nincs kipróbálva)

Az Inno Setup Compiler szükséges a tényleges payload telepítő elkészítéséhez.

A script az `ISCC.exe` programot az alábbi helyeken keresi:

```text
C:\Program Files (x86)\Inno Setup 6\ISCC.exe
C:\Program Files\Inno Setup 6\ISCC.exe
```

### .NET Framework C# compiler

A külső splash/bootstrapper EXE fordításához a Windows `.NET Framework` `csc.exe` fordítója szükséges.

A script az alábbi helyeken keresi:

```text
%WINDIR%\Microsoft.NET\Framework64\v4.0.30319\csc.exe
%WINDIR%\Microsoft.NET\Framework\v4.0.30319\csc.exe
```

---

## 2. Build indítása

Nyiss egy `cmd.exe` ablakot, majd lépj a projekt gyökerébe.

Példa:

```bat
cd C:\work\nav-xsd-parser-tool
```

Ezután indítsd el:

```bat
installer\windows\build-installer.bat
```

A scriptet célszerű mindig a projekt aktuális, tiszta forrásából futtatni.

---

## 3. Automatikus verziószám

A build sorszámát ez a fájl tartja nyilván:

```text
installer/windows/build-number.txt
```

A script minden build indításakor eggyel növeli az értéket.

Például ha a fájl tartalma:

```text
22
```

akkor a következő build száma:

```text
23
```

A jelenlegi verzióképzés:

```text
<major>.<minor>.<build>-<yyyyMMdd-HHmmss>
```

Példa:

```text
1.11.23-20260815-223200
```

A build timestamp a helyi Windows rendszeridőből készül:

```powershell
Get-Date -Format yyyyMMdd-HHmmss
```

### Numerikus és megjelenített verzió

A `jpackage` és a Windows numerikus fájlverzió nem kaphat timestampet, ezért két verzió készül:

```text
APP_NUMERIC_VERSION = 1.11.23
APP_VERSION         = 1.11.23-20260815-223200
```

A numerikus változat kerül például a `jpackage --app-version` paraméterbe.

A teljes változat kerül a telepítő nevébe és a megjelenített verzióba.

> Fontos: a buildszám már a build indításakor növekszik. Ha a build később hibával leáll, a következő futás új buildszámot kap.

---

## 4. A build lépései

A `build-installer.bat` jelenleg 9 fő lépést hajt végre.

### 4.1 Projektgyökér és verzió meghatározása

A script meghatározza a projekt gyökérkönyvtárát, majd meghívja a gyökér `version.cmd` wrapperét. A verzióelemző a legutóbbi SemVer Git tag óta történt committed és lokális változások alapján MAJOR/MINOR/PATCH döntést hoz. Git metadata nélküli forráscsomag esetén konzervatív PATCH fallbacket használ. A kimenetből készül a numerikus alkalmazásverzió és a dátum/idő bélyeggel ellátott teljes telepítőverzió.

### 4.2 Maven build

A script automatikusan futtatja:

```bat
mvn clean package -DskipTests -Drevision=<semver> -Dapp.release.version=<semver> -Dapp.build.timestamp=<yyyyMMdd-HHmmss>
```

Például:

```bat
mvn clean package -DskipTests -Drevision=1.12.0 -Dapp.release.version=1.12.0 -Dapp.build.timestamp=20260820-120000
```

Ez a telepítő-build része, tehát a `build-installer.bat` futtatásakor a Maven build automatikusan megtörténik.

### 4.3 Futtatható Spring Boot JAR megkeresése

A script a következő könyvtárban keresi a futtatható JAR-t:

```text
nav-xsd-parser-tool-web\target\
```

Kizárja a következő fájlokat:

- `sources` JAR;
- `javadoc` JAR;
- `original-*` JAR.

A kiválasztott fájl a staging könyvtárba `app.jar` néven kerül.

### 4.4 Staging input előkészítése

A JAR mellett a szükséges projektfájlok is bekerülnek a staging könyvtárba, például:

- `config`;
- `README.md`;
- `LICENSE`;
- `README-LICENC.md`;
- `licenses`.

### 4.5 jpackage app-image

A script app-image típust készít:

```bat
jpackage ^
  --type app-image ^
  --name "M2M XML EDITOR" ^
  --input "..." ^
  --main-jar "app.jar" ^
  --main-class org.springframework.boot.loader.launch.JarLauncher ^
  --icon "installer\windows\app.ico" ^
  --java-options "-Dfile.encoding=UTF-8" ^
  --app-version "1.11.23"
```

A jpackage eredménye tartalmazza többek között:

```text
M2M XML EDITOR.exe
app\
runtime\
```

### 4.6 Inno Setup payload telepítő

A script meghívja az `ISCC.exe` fordítót, és paraméterként átadja az `.iss` fájlnak:

```text
MyAppVersion
MyNumericVersion
MyBuildTimestamp
MySourceDir
MyOutputDir
MyProjectDir
```

Az Inno script:

```text
installer/windows/m2m-xml-editor.iss
```

A `.iss` fájl szándékosan hibával leáll, ha a verzióparamétereket nem a build script adja át. Emiatt nem ajánlott a fájlt közvetlenül az Inno Setup IDE-ből lefordítani.

### 4.7 Külső bootstrapper fordítása

A script a következő forrást fordítja:

```text
installer/windows/bootstrapper/M2MXmlEditorBootstrapper.cs
```

A bootstrapper feladata, hogy azonnal megjelenítse a Windows splash képernyőt, majd elindítsa a tényleges Inno Setup payload telepítőt.

### 4.8 Kimenet ellenőrzése

A script ellenőrzi, hogy létrejött-e:

- a külső bootstrapper EXE;
- a payload Setup EXE;
- legalább egy Inno `.bin` adatfájl.

Ha valamelyik hiányzik, a build hibával leáll.

### 4.9 Telepítési útmutatók generálása

A `dist` könyvtárba automatikusan létrejön:

```text
TELEPITES-HU.txt
INSTALL-EN.txt
```

---

## 5. A kész telepítő helye

A build kimenete:

```text
installer\windows\build\dist\
```

Példa:

```text
M2M-XML-EDITOR-Setup-1.11.23-20260815-223200.exe
M2M-XML-EDITOR-Payload-Setup-1.11.23-20260815-223200.exe
M2M-XML-EDITOR-Payload-Setup-1.11.23-20260815-223200-1.bin
TELEPITES-HU.txt
INSTALL-EN.txt
```

## 6. Mit kell együtt terjeszteni?

A teljes `dist` csomagot együtt kell átadni.

Különösen fontos, hogy az alábbiak egy könyvtárban maradjanak:

```text
M2M-XML-EDITOR-Setup-<version>.exe
M2M-XML-EDITOR-Payload-Setup-<version>.exe
M2M-XML-EDITOR-Payload-Setup-<version>-*.bin
```

A felhasználó ezt indítja:

```text
M2M-XML-EDITOR-Setup-<version>.exe
```

A payload EXE-t és a `.bin` fájlokat nem szabad átnevezni vagy külön könyvtárba mozgatni.

---

## 7. Új telepítés és frissítés

Ugyanaz az Inno Setup telepítő kezeli az új telepítést és a meglévő rendszer frissítését.

### Új telepítés

Új telepítésnél a wizard létrehozza a szükséges konfigurációt és könyvtárstruktúrát.

A telepítés lehet:

- csak az aktuális felhasználónak;
- minden felhasználónak / gépszinten.

Per-user telepítés adatgyökere:

```text
%LOCALAPPDATA%\M2M-XML-EDITOR
```

Gépszintű telepítés adatgyökere:

```text
%PROGRAMDATA%\M2M-XML-EDITOR
```

### Frissítés

Frissítésnél a telepítő a meglévő konfigurációt megőrzi.

A fő szabályok:

- meglévő konfigurációs érték nem íródik felül;
- új, hiányzó konfigurációs kulcs hozzáfűzhető;
- új konfigurációs fájl csak akkor jön létre, ha még nem létezik;
- új szükséges könyvtár automatikusan létrejön;
- az alkalmazás `app` és `runtime` könyvtára tisztán cserélődik;
- a felhasználói adatok, XML-ek, adatbázis, backup, repo és konfiguráció megmaradnak;
- az adatbázis-séma frissítését az alkalmazás indulásakor a Flyway végzi.

A konfigurációs template-ek Inno oldalon `onlyifdoesntexist` védelemmel kerülnek telepítésre.

---

## 8. Első indítás és Setup

Friss, új rendszer esetén a telepítő konfigurációja:

```properties
nav.xsdparsertool.setup.completed=false
```

Az alkalmazás első induláskor a setup folyamatra irányít. A kötelező adatkönyvtár-, működési mód- és kezdő admin beállítások mellett opcionálisan megadhatók a külső integrációk hozzáférési adatai is:

- GitHub token;
- NAV M2M API-kulcs egyetlen mezőben, `userId-password-aláírókulcsElsőFele-nonce` formátumban;
- NAV M2M Client ID és Client Secret.

Az M2M API-kulcsot a rendszer a kötőjelek mentén négy részre bontja. A User ID a `nav.m2m.auth.username`, a jelszó a `nav.m2m.auth.password`, az aláírókulcs első része a `nav.m2m.signature.key-first-part`, a nonce pedig a `nav.m2m.signature.nonce` konfigurációhoz kerül. A Client ID és Client Secret csak együtt adható meg.

Az érzékeny értékek – a GitHub token, az M2M jelszó, az aláírókulcs első része, a nonce és a Client Secret – a rendszer meglévő titkosított secret-tárolójába kerülnek, és nem írhatók ki naplóba vagy vissza a setup ellenőrző oldalára.

A setup befejezése után:

```properties
nav.xsdparsertool.setup.completed=true
```

A setupnak a telepítő által kezelt fő runtime konfigurációs fájlt kell frissítenie:

```text
config\nav-xsd-parser-tool-paths.properties
```

Nem szabad második, teljes konfigurációt tartalmazó `application-bootstrap.properties` fájlt létrehozni installer-managed telepítéshez.

---

## 9. Fontos konfigurációs szabályok

Installer-managed rendszerben az elsődleges runtime konfiguráció:

```text
config\nav-xsd-parser-tool-paths.properties
```

A DB-specifikus fájl például H2 esetén:

```text
config\database\H2.properties
```

A fő konfiguráció a konkrét DB-fájlt importálja, például:

```properties
spring.config.import=optional:file:C:/Users/<user>/AppData/Local/M2M-XML-EDITOR/config/database/H2.properties
```

A telepítő nem használhat önmagára visszamutató vagy körkörös `spring.config.import` láncot.

---

## 10. Gyakori hibák

### `Maven nincs a PATH-ban`

Ellenőrizd:

```bat
mvn -version
```

### `jpackage nincs a PATH-ban`

Ellenőrizd a JDK telepítést és a `JAVA_HOME` / `PATH` beállítást:

```bat
jpackage --version
```

### `Nem található ISCC.exe`

Telepítsd az Inno Setup 6-ot a szabványos Program Files könyvtárba, vagy igazítsd a build script `ISCC_EXE` feloldását.

### `Nem található csc.exe`

Ellenőrizd, hogy a Windows .NET Framework telepítve van-e, és létezik-e a script által keresett `Framework` vagy `Framework64` könyvtár.

### Inno Setup: `Type mismatch`

Az Inno Pascal Script API egyes fájlkezelő függvényei `AnsiString` paramétert várnak. A projektben a `LoadStringFromFile` / `SaveStringToFile` körüli konverziókat ezért nem szabad egyszerűen `String` típusra visszaírni.

### A telepítő EXE elindul, de a payload nem található

A bootstrapper, payload EXE és `.bin` fájlok legyenek ugyanabban a könyvtárban, eredeti fájlnévvel.

### Build után nincs Setup EXE

Ellenőrizd a build kimenetének korábbi lépéseit. A script minden fő lépés után hibakódot vizsgál, ezért az első tényleges `HIBA:` üzenet általában megmutatja a gyökérokot.

---

## 11. Javasolt release ellenőrzés

Telepítő kiadása előtt legalább az alábbiakat érdemes ellenőrizni:

1. `build-installer.bat` hibamentesen lefut;
2. az EXE verziója és fájlneve tartalmazza az aktuális buildszámot és timestampet;
3. a bootstrapper EXE elindítja a payload telepítőt;
4. per-user friss telepítés `%LOCALAPPDATA%` alatt működik;
5. gépszintű telepítés `%PROGRAMDATA%` alatt működik;
6. első indításkor új telepítés esetén elindul a Setup;
7. setup után normál login / alkalmazás indul;
8. meglévő telepítés frissítése nem írja felül a konfigurációt;
9. új konfigurációs kulcs frissítéskor hozzáadódik;
10. új könyvtár frissítéskor létrejön;
11. meglévő XML-ek, adatbázis, backup és repository adatok megmaradnak;
12. uninstall / reinstall viselkedés külön tesztelve van;
13. a teljes `dist` csomag együtt kerül terjesztésre.

---

## 12. Rövid build parancs

Ha minden előfeltétel telepítve van, a normál telepítő-build egyetlen parancs:

```bat
installer\windows\build-installer.bat
```

A kész csomag innen vehető át:

```text
installer\windows\build\dist\
```
