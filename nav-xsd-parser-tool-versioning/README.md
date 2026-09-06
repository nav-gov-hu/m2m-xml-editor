# nav-xsd-parser-tool-versioning

Lokális, külső szolgáltatás nélküli verzióelemző modul. A Git állapotból és diffből meghatározza a következő SemVer verziót, majd build metadata fájlokat generál.

## Fő működés

- A legutóbbi `vX.Y.Z` vagy `X.Y.Z` Git taget használja alapként.
- Ha nincs release tag, a `versioning.properties` `default.base.version` értéke az alapverzió, a diff alapja pedig `HEAD`, így a lokális változásokat vizsgálja.
- A committed, tracked working-tree és untracked változásokat is figyelembe veszi.
- A döntés prioritása: `MAJOR > MINOR > PATCH > NONE`.
- Kézi felülbírálás: `--override=major|minor|patch|none|auto`.

## Tipikus szabályok

- MAJOR: breaking commit jelölés, REST mapping/public API eltávolítás, destruktív Flyway SQL.
- MINOR: új Maven modul, új REST endpoint, új produkciós Java komponens, új kompatibilis Flyway migráció vagy új konfigurációs kulcs.
- PATCH: hibajavítás, installer/UI/refaktor, teszt vagy dokumentáció.

## Kimenet

Alapértelmezésben `target/generated-version` alatt:

- `build-version.properties`
- `build-version.env`
- `version-report.txt`

A verziózott buildhez használd a projektgyökérben a `build-versioned.cmd` vagy `build-versioned.sh` wrappert.

## Kiadott verzió alapvonal

A `versioning.properties` `released.base.version` kulcsa a már kiadott legkisebb megengedett alapverzió. Jelenleg `1.11.40`. A verzióelemző ennél régebbi Git taget nem használ baseline-ként. Ha nincs `1.11.40` vagy újabb SemVer tag, a working tree változásait a `HEAD`-hez képest elemzi, de a verziószám számítása `1.11.40`-től indul.

## Fejlesztői Maven-verzió és `spring-boot:run`

A lokális fejlesztői Maven-futtatás és az automatikusan számított release-verzió két külön szerepet kap.

- A projektgyökér `.mvn/maven.config` fájlja egy stabil fejlesztői `revision` értéket ad a Mavennek már a projektmodell felépítése előtt. Emiatt a `nav-xsd-parser-tool-web` modulból indított `mvn spring-boot:run` esetén sem kell kézzel `-Drevision=...` paramétert megadni.
- A release-verziót továbbra is a `VersioningTool` számolja. A `build-versioned.cmd`, `build-versioned.sh` és a Windows installer buildje a kiszámított verziót parancssori `-Drevision=<release>` értékkel felülírja. A fejlesztői `maven.config` tehát nem release-verzióforrás.
- A root POM `flatten-maven-plugin` integrációja `resolveCiFriendliesOnly` módban a telepített vagy publikált POM-okban konkrét verzióra oldja fel a `${revision}` hivatkozást. Így a lokális Maven repositoryból vagy Nexusból visszaolvasott modul POM-ja nem próbál `${revision}` nevű parent artifactot keresni.

Ha a javítás előtti verzióból már került hibás, feloldatlan `${revision}`-t tartalmazó SNAPSHOT POM a lokális Maven repositoryba, a frissített forrásból egyszer végre kell hajtani egy root szintű `mvn clean install -DskipTests` futtatást. Ez a régi lokális artifact descriptorokat felülírja a flattenelt POM-okkal. Ezt követően a normál fejlesztői indításnál nincs kézi verziókarbantartás.

## Friss checkout fejlesztői bootstrap (R14M)

Teljesen új fejlesztői gépen ne kelljen kézzel Maven-verziót megadni vagy POM-ot szerkeszteni. A projektgyökér `bootstrap-dev.cmd` / `bootstrap-dev.sh` scriptje automatikusan kiszámítja a fejlesztői revisiont és telepíti a reactort. A `run-web-dev.cmd` / `run-web-dev.sh` ugyanezt a verziószámítást használva előbb felépíti a web modul saját projektfüggőségeit, majd `spring-boot:run` paranccsal elindítja az alkalmazást. A web modulból a `run-dev.cmd` / `run-dev.sh` kényelmi wrapper használható.

## Verzió bootstrap scriptek

A projektgyökér `version.cmd` és `version.sh` scriptje Maven indítása előtt közvetlenül a JDK `javac`/`java` parancsaival fordítja és futtatja a `VersioningTool` osztályt. Így az automatikusan kiszámított `VERSION_RELEASE` már a Maven projektmodell felépítése előtt átadható `-Drevision` paraméterként.

Windows alatt a `.cmd` és `.bat` állományok UTF-8 BOM nélkül kerülnek a repositoryba. A BOM használata a `cmd.exe` számára az első `@echo off` parancsot hibás karakterekkel egészítené ki.
