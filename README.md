# M2M XML EDITOR

Az M2M XML EDITOR egy Java alapú alkalmazás NAV M2M XML állományok kezelésére. A program célja, hogy az XML-eket ne csak nyers szövegként lehessen megnyitni, hanem az XSD és UIModel leírók alapján kitöltő- és ellenőrző felületen is lehessen velük dolgozni.

A rendszer Windows asztali alkalmazásként telepíthető, amennyiben a telepítőt előállítod, de fejlesztői környezetből Spring Boot alkalmazásként is futtatható.
Amennyiben lefordítod a programot és windows telepítőt készítesz elkészítettem számodra egy alap felhasználói kézikönyvet amit bátran módosíthatsz magad részére.
Ezt ítt éred el:

https://github.com/nav-gov-hu/m2m-xml-editor/blob/main/developer_docs/M2M_XML_EDITOR_Felhaszn%C3%A1l%C3%B3i_K%C3%A9zikonyv_1.0.pdf

## Mire használható?

A program főbb feladatai:

- XML állományok feltöltése, nyilvántartása és megnyitása;
- az XML-hez tartozó XSD és UIModel automatikus feloldása;
- XML megjelenítése és szerkesztése űrlapnézetben;
- XSD és XPath alapú ellenőrzések futtatása;
- hibás XML-ek javítása az űrlapnézetben;
- nagy méretű és több űrlapot tartalmazó XML-ek kezelése;
- XML mentése, új verzió készítése és változások követése;
- HTML/PDF nyomtatási nézet készítése;
- támogatott esetben NAV M2M beküldés és státuszkövetés;
- űrlapsablonok és kapcsolódó XSD/UIModel/XPath erőforrások kezelése;
- rendszer-, adatbázis-, naplózási és egyéb technikai beállítások adminisztrációja.

A program az XML szerkezetét nem kézzel felépített képernyőkből állítja elő. Az űrlapnézet az adott dokumentumhoz tartozó XSD és UIModel alapján készül, ezért több különböző NAV dokumentumtípus kezelésére alkalmas.


## Fordítás:
Gyökérben add ki az alábbi parancsot:

Windows:
bootstrap-dev.cmd

Linux:
bootstrap-dev.sh

## Indítás parancssorból

Windows:
run-web-dev.cmd

Linux:
run-web-dev.sh

## Windows installer készítés
.\installer\windows\build-installer.bat

## Követelmények

### Telepített Windows verzió

A Windows telepítő tartalmazza az alkalmazás futtatásához szükséges csomagot. A felhasználónak normál esetben nem kell Mavenből buildelnie a programot.

A telepítő két módot kínál:

- **Egyszerű telepítés** – helyi használatra ajánlott, H2 adatbázissal és alapértelmezett könyvtárakkal;
- **Haladó telepítés** – az adatbázis, könyvtárak, alkalmazásmód és egyéb technikai paraméterek külön megadhatók.

### Fejlesztői környezet

Forrásból történő futtatáshoz szükséges:

- Java 17;
- Maven 3.8 vagy újabb;
- valamelyik támogatott adatbázis, vagy a beépített H2.

Támogatott adatbázistípusok:

- H2;
- MySQL;
- PostgreSQL;
- Oracle.

## Windows telepítés

Indítsd el az M2M XML EDITOR telepítőjét. A telepítő rendszergazdai és normál felhasználói módban is használható.

- **Minden felhasználónak / rendszergazdai telepítés:** az alkalmazás gépszinten települ, az írható adatok alaphelye `C:\ProgramData\M2M-XML-EDITOR`.
- **Csak az aktuális felhasználónak:** nem szükséges rendszergazdai jogosultság; az alkalmazás és az írható adatok a felhasználói profil alatt kerülnek elhelyezésre, az adatkönyvtár alaphelye `%LOCALAPPDATA%\M2M-XML-EDITOR`.

Ezután válaszd ki az **Egyszerű** vagy **Haladó** telepítést.

### Egyszerű telepítés

Az Egyszerű mód helyi használatra készült. Alapértelmezésben:

- STANDALONE alkalmazásmódot használ;
- H2 adatbázist használ;
- az alkalmazás adatait gépszintű telepítésnél a `C:\ProgramData\M2M-XML-EDITOR`, felhasználói telepítésnél a `%LOCALAPPDATA%\M2M-XML-EDITOR` könyvtár alatt tartja;
- a webes felület a 8080-as porton indul;
- az alkalmazás induláskor meg tudja nyitni az alapértelmezett böngészőt.

Ez a mód akkor célszerű, ha a programot egy munkaállomáson, külön adatbázis-szerver nélkül szeretnéd használni.

### Haladó telepítés

Haladó módban többek között megadható:

- STANDALONE vagy MULTI_USER működés;
- az adatbázis típusa és kapcsolati adatai;
- az XML állományok könyvtára;
- az XSD, UIModel, XPath, common XSD és Rule-XSL könyvtárak;
- a HTTP port;
- a nagy XML kezelésének küszöbe;
- a session időkorlát;
- az asztali integráció beállításai.

A telepítő a kiválasztott értékekből létrehozza a szükséges külső konfigurációs fájlokat.

## Első indítás

Az alkalmazás első indulásakor a kezdeti beállítási folyamatot végig kell futtatni.

A rendszer STANDALONE és MULTI_USER módban is felhasználói bejelentkezést használ. Az első beállítás során létrejön a kezdő adminisztrátor, amellyel később be lehet jelentkezni.

A két működési mód közötti fő különbség a jogosultságkezelés:

- **STANDALONE**: elsősorban egyfelhasználós, helyi használatra készült;
- **MULTI_USER**: több felhasználó és részletesebb szerepkör-alapú jogosultságkezelés használható.

Sikeres indítás után a webes felület alapértelmezett címe:

```text
http://localhost:8080/
```

Ha más portot állítottál be, a 8080 helyett azt kell használni.

## Bejelentkezés

A bejelentkező oldal közvetlenül is elérhető:

```text
http://localhost:8080/login.html
```

A kezdeti adminisztrátori felhasználót az első beállítás során kell megadni.

MULTI_USER módban további felhasználók és jogosultságok az adminisztrációs felületen kezelhetők.

## A program használata röviden

### XML állomány megnyitása

Az **Űrlapállományok** oldalon lehet XML fájlt feltölteni vagy a szerveren már elérhető XML-eket megnyitni.

A program a dokumentum alapján megpróbálja meghatározni a hozzá tartozó sémát és UIModelt. Ha a szükséges erőforrások rendelkezésre állnak, az XML űrlapnézetben is megjeleníthető.

### Űrlapnézet

Az űrlapnézet az XSD és UIModel alapján épül fel. Itt a mezők szerkeszthetők, és a módosítások az XML megfelelő eleméhez kerülnek.

Az XSD szerint hibás, de szerkezetileg jól formált XML is megnyitható javításra. A hibás mezők jelölést kapnak, így az eltérések az űrlapon javíthatók.

### Validálás

A program két fő ellenőrzést támogat:

- **XSD validáció** – az XML megfelel-e a hozzá tartozó sémának;
- **XPath/XSLT validáció** – az adott dokumentumtípus üzleti szabályainak ellenőrzése.

A validáció eredménye a felületen megtekinthető, és ahol lehetséges, a hiba közvetlenül az érintett mezőhöz kapcsolódik.

### Mentés és verziózás

A megnyitott XML menthető, illetve új verzió is készíthető belőle. Az új verzióhoz külön fájlnév és megjegyzés adható meg.

A rendszer nyilvántartja a módosításokat és támogatja a verziók közötti eltérések megtekintését.

### Nagy XML-ek és multiform dokumentumok

Nagy állományoknál a program nem próbálja minden esetben egyszerre memóriába tölteni és megjeleníteni a teljes dokumentumot.

A több űrlapot tartalmazó XML-eknél a főlap és a melléklapok külön kezelhetők. A melléklapok listából kereshetők és lapozhatók.

### NAV M2M beküldés

Ha az M2M kapcsolat be van állítva, a megfelelő állapotú XML dokumentumok beküldhetők a NAV M2M szolgáltatás felé. A rendszer kezeli a kapcsolódó csatolmányokat és a beküldés státuszának lekérdezését is.

Az M2M funkció használatához a szükséges végpontokat és hitelesítési adatokat külön konfigurálni kell.

## Konfigurációs fájlok

A program működésének nagy része külső konfigurációból állítható. A konfigurációs könyvtár a telepítési módtól függ:

```text
Minden felhasználónak: C:\ProgramData\M2M-XML-EDITOR\config
Csak az aktuális felhasználónak: %LOCALAPPDATA%\M2M-XML-EDITOR\config
```

### Fő konfiguráció

A Windows telepítő által létrehozott fő konfigurációs fájl az adatkönyvtár `config` alkönyvtárában található:

```text
<adatkönyvtár>\config\nav-xsd-parser-tool-paths.properties
```

Ebben találhatók többek között:

- a HTTP port;
- a security mód;
- a kiválasztott adatbázistípus;
- az XML könyvtárak;
- az XSD/UIModel/XPath könyvtárak;
- a logolás alapbeállításai;
- a nagy XML paraméterei;
- az asztali integráció beállításai.

### Bootstrap konfiguráció

Az első beállítási folyamat létrehozhat egy bootstrap konfigurációt is a kiválasztott alkalmazás-adatkönyvtárban:

```text
<adatkönyvtár>\config\application-bootstrap.properties
```

A pontos hely ugyanazt az adatgyökeret követi, mint a telepítés többi írható állománya. Gépszintű telepítésnél ez a ProgramData, felhasználói telepítésnél a LOCALAPPDATA alatti M2M-XML-EDITOR könyvtár.

Ez az indításhoz szükséges alapbeállításokat, például az adatkönyvtár és az adatbázis bootstrap adatait tartalmazhatja.

### Adatbázis konfiguráció

Az adatbázis-specifikus beállítások itt találhatók:

```text
<adatkönyvtár>\config\database
```

A fájl neve a kiválasztott adatbázistól függ:

```text
H2.properties
MYSQL.properties
POSTGRESQL.properties
ORACLE.properties
```

Például MySQL használatakor:

```text
<adatkönyvtár>\config\database\MYSQL.properties
```

Itt található a JDBC URL, a felhasználónév, a jelszó és az adott adatbázishoz tartozó Flyway konfiguráció.

### XML index konfiguráció

Az XML keresési/indexelési mezők konfigurációja alapértelmezésben:

```text
<adatkönyvtár>\config\xml-index-config.xml
```

### Fejlesztői konfiguráció

A repository-ban található mintakonfigurációk:

```text
config\nav-xsd-parser-tool-paths.properties
config\database\H2.properties
config\database\MYSQL.properties
config\database\POSTGRESQL.properties
config\database\ORACLE.properties
```

A Spring Boot belső bootstrap konfigurációja:

```text
nav-xsd-parser-tool-web\src\main\resources\application.properties
```

A belső `application.properties` csak az indításhoz szükséges alapértékeket tartalmazza. Telepített környezetben a tényleges működési beállításokat lehetőleg a külső konfigurációban kell kezelni.

## Alapértelmezett adatkönyvtár

Windows telepítésnél az adatkönyvtár a telepítési módtól függ:

```text
Minden felhasználónak: C:\ProgramData\M2M-XML-EDITOR
Csak az aktuális felhasználónak: %LOCALAPPDATA%\M2M-XML-EDITOR
```

A pontos könyvtárstruktúra a telepítés és a konfiguráció függvényében változhat, de jellemzően itt találhatók:

```text
<adatkönyvtár>\
├── config\
├── database\
├── data\
├── logs\
├── repo\
└── backup\
```

Fontosabb tartalmak:

- `config` – külső konfigurációs fájlok;
- `data` – XML-ek, adatbázis és egyéb futásidejű adatok;
- `logs` – alkalmazásnaplók;
- `repo` – XSD, UIModel, XPath és kapcsolódó technikai erőforrások;
- `backup` – mentések és biztonsági másolatok.

## Naplófájl

A fájlos napló az aktuális adatkönyvtár alatt található:

```text
<adatkönyvtár>\logs\app.log
```

Ha az alkalmazás nem indul el, vagy egy művelet hibával leáll, ezt a fájlt érdemes először ellenőrizni.

A logolási szint a konfigurációban állítható, például:

```properties
app.log.level=INFO
```

Általános használatra az `INFO` érték javasolt. Hibakereséshez ideiglenesen használható `DEBUG`, de tartósan nem célszerű szükségtelenül részletes naplózást bekapcsolni.

## Adatbázis

Az aktuális adatbázistípus a konfigurációban adható meg:

```properties
nav.xsdparsertool.database.type=H2
```

Lehetséges értékek:

```text
H2
MYSQL
POSTGRESQL
ORACLE
```

H2 esetén nincs szükség külön adatbázis-szerverre. MySQL, PostgreSQL vagy Oracle használatakor a kapcsolat adatait a megfelelő `config\database\*.properties` fájlban kell megadni.

Az adatbázis-sémát az alkalmazás Flyway migrációkkal kezeli.

## XSD, UIModel és XPath erőforrások

Az űrlapok helyes felismeréséhez és megjelenítéséhez szükséges erőforrások helye konfigurálható.

A fontosabb beállítások:

```properties
nav.xsdparsertool.paths.schema-dir=...
nav.xsdparsertool.paths.ui-model-dir=...
nav.xsdparsertool.paths.common-xsd-dir=...
nav.xsdparsertool.xpath-validator.rule-root-dir=...
nav.xsdparsertool.xpath-validator.xsl-root-dir=...
```

Ha egy XML megnyitható, de az űrlap vagy valamelyik validáció nem működik, érdemes ellenőrizni, hogy ezek az útvonalak a megfelelő repository-kra mutatnak-e.

## GitHub űrlapsablon-katalógus

A program képes a konfigurált GitHub szervezetből elérhető űrlapsablon-release-ek nyilvántartására és letöltésére.

A lokálisan már nyilvántartott katalógus GitHub token nélkül is használható. Külső GitHub ellenőrzéshez és privát/internal repository-k eléréséhez tokenre lehet szükség.

A tokeneket és más hitelesítési adatokat ne írd forráskódba és ne commitold repository-ba.

## Fejlesztői build

A verziózott teljes build ajánlott indítása a repository gyökeréből:

```bash
./build-versioned.sh
```

Windows alatt:

```bat
build-versioned.cmd
```

A wrapper a `nav-xsd-parser-tool-versioning` modullal előbb meghatározza a következő SemVer verziót, majd ezt és a build időpontját adja át a Maven buildnek. Kézi felülbírálás például `VERSION_BUMP_OVERRIDE=patch`. A közvetlen `mvn clean package` továbbra is használható fejlesztői buildre, ilyenkor a POM alapértelmezett `revision` értéke érvényesül.

Csak a webalkalmazás és annak függőségei:

```bash
mvn -pl nav-xsd-parser-tool-web -am clean package
```

A webalkalmazás build után a `nav-xsd-parser-tool-web/target` könyvtárba kerül.

## Fejlesztői indítás

Build után a web modul JAR-ja indítható:

```bash
java -jar nav-xsd-parser-tool-web/target/nav-xsd-parser-tool-web.jar
```

Fejlesztés közben érdemes a repository `config` könyvtárában lévő konfigurációs mintákból kiindulni.

Az alkalmazás a következő külső konfigurációs helyeket is támogatja:

```text
<adatkönyvtár>/config/nav-xsd-parser-tool-paths.properties
<adatkönyvtár>/config/application-bootstrap.properties
<adatkönyvtár>/config/nav-xsd-parser-tool-paths.properties
```

## Maven modulok röviden

A projekt több Maven modulból áll. A legtöbb felhasználónak ezekkel nem kell foglalkoznia, fejlesztésnél azonban hasznos tudni a fő felelősségeket:

- `nav-xsd-parser-tool-versioning` – lokális Git diff alapú SemVer döntés és build metadata;
- `nav-xsd-parser-tool-core` – közös modellek és alapfunkciók;
- `nav-xsd-parser-tool-schema-registry` – XSD/UIModel/XPath erőforrások feloldása;
- `nav-xsd-parser-tool-uimodel` – UIModel feldolgozás;
- `nav-xsd-parser-tool-processing` – XML feldolgozási folyamatok;
- `nav-xsd-parser-tool-print` – nyomtatási funkciók;
- `nav-xsd-parser-tool-web` – Spring Boot webalkalmazás és frontend;
- `nav-xsd-parser-tool-cli` – parancssori funkciók;
- `nav-xsd-parser-tool-xpath-cli` – XPath validációs parancssori eszköz.

## Gyakori problémák

### Nem nyílik meg a webes felület

Ellenőrizd:

1. fut-e az alkalmazás;
2. melyik HTTP port van beállítva;
3. van-e hiba a `logs\app.log` fájlban.

Alapértelmezett cím:

```text
http://localhost:8080/
```

### Nem sikerül az adatbázis-kapcsolat

Ellenőrizd a kiválasztott adatbázistípushoz tartozó fájlt:

```text
<adatkönyvtár>\config\database\<DBTYPE>.properties
```

Különösen a következő értékeket:

```properties
spring.datasource.url=...
spring.datasource.username=...
spring.datasource.password=...
```

### Az XML felismerhető, de az űrlap nem megfelelő

Ellenőrizd az XSD és UIModel könyvtárakat, valamint hogy az adott dokumentumtípushoz a megfelelő verziójú erőforrások vannak-e telepítve.

### Az XPath ellenőrzés nem indul

Ellenőrizd az XPath rule és Rule-XSL könyvtárak konfigurációját.

### Hol találom a hibát?

Elsőként az alkalmazás naplóját nézd meg:

```text
<adatkönyvtár>\M2M-XML-EDITOR\logs\app.log
```

## Biztonsági megjegyzések

- Jelszót, API kulcsot, GitHub tokent vagy M2M hitelesítési adatot ne commitolj a forráskódba.
- Telepített rendszerben az érzékeny értékeket a külső konfigurációban vagy az alkalmazás erre szolgáló konfigurációs felületén kezeld.
- A `config` és `data` könyvtárakról érdemes rendszeres biztonsági mentést készíteni.
- MULTI_USER használatnál a felhasználók jogosultságait a szükséges minimumra érdemes korlátozni.

## További dokumentáció

A részletes fejlesztői és technikai dokumentáció a repository `docs` könyvtárában található.

A README szándékosan csak a rendszer használatához, telepítéséhez, indításához és alapvető fejlesztői eligazodáshoz szükséges információkat tartalmazza. Az egyes funkciók részletes implementációs leírása külön dokumentációban található.

## Licenc

A projekt licencére vonatkozó információ a repository gyökerében található `LICENSE` és `README-LICENC.md` fájlban olvasható.
