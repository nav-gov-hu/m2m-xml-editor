# nav-xsd-parser-tool-multiform

A `nav-xsd-parser-tool-multiform` egy önálló, Java 17 alapú könyvtár és parancssori modul a NAV multiform XSD-k feldolgozásához.

A modul elsődleges feladata, hogy egy olyan XSD-ből, amely egyetlen főlapot és egy ismétlődő melléklapot ír le, automatikusan felismerje a részbizonylatokat, azokat önállóan is validálhatóvá tegye, majd egy ZIP-ben érkező 1 főlap + N melléklap XML-ből előállítsa és validálja a teljes XML-dokumentumot.

## Fő funkciók

- a teljes NAV XSD automatikus elemzése;
- az `1..1` kardinalitású főlap felismerése;
- az `unbounded` ismétlődő melléklap felismerése;
- külön főlap- és melléklap-adapter XSD előállítása;
- önálló A/M XML-ek XSD-validálása az eredeti XSD típusaival;
- ZIP-ben lévő XML-ek osztályozása a gyökérelem alapján;
- pontosan 1 főlap és az XSD szerinti számú melléklap ellenőrzése;
- minden részbizonylat külön validálása;
- StAX streaming alapú összefűzés teljes DOM felépítése nélkül;
- az elkészült teljes XML újravalidálása az eredeti NAV XSD-vel;
- publikus Java API és dependency-free CLI.

## Modulfüggőségek

A `src/main` kód kizárólag Java 17 szabványos API-kat használ:

- JAXP / XSD (`javax.xml.validation`);
- StAX (`javax.xml.stream`);
- ZIP (`java.util.zip`);
- NIO (`java.nio.file`).

Nincs Spring-, adatbázis-, web- vagy más NAV XSD Parser Tool modulfüggőség. Emiatt a modul önálló JAR-ként is használható.

A JUnit kizárólag `test` scope-ban szerepel a `pom.xml`-ben.

A nagyobb multi-module projektben a javasolt függőségi irány:

```text
nav-xsd-parser-tool-processing
              |
              v
nav-xsd-parser-tool-multiform

nav-xsd-parser-tool-web
              |
              v
nav-xsd-parser-tool-processing
```

A `multiform` modul ne függjön vissza a `processing` vagy `web` modultól.

## Működési elv

Egy tipikus multiform séma dokumentumtípusa például:

```xml
<xs:complexType name="Doc_2608_Type">
    <xs:sequence>
        <xs:element name="Form_2608A"
                    type="tns:Form_2608A_Type"
                    minOccurs="1"
                    maxOccurs="1"/>

        <xs:element name="Form_2608M"
                    type="tns:Form_2608M_Type"
                    minOccurs="1"
                    maxOccurs="unbounded"/>
    </xs:sequence>
</xs:complexType>
```

A modul ebből automatikusan létrehozza a következő leírást:

```text
Document:  Doc_2608
Main:      Form_2608A -> Form_2608A_Type -> 1..1
Repeating: Form_2608M -> Form_2608M_Type -> 1..unbounded
```

A külön A- és M-lap XML-ek nem kapnak saját, kézzel karbantartott sémát. A modul rövid adapter-XSD-t generál, amely az eredeti NAV XSD-ben található típust importálja. Így nincs séma-duplikáció.

Például a melléklap adapterének lényege:

```xml
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
           xmlns:tns="https://soap.api.nav.gov.hu/definitions/model/2.0/2608/5.2">

    <xs:import namespace="https://soap.api.nav.gov.hu/definitions/model/2.0/2608/5.2"
               schemaLocation="NAV_2608.xsd"/>

    <xs:element name="Form_2608M"
                type="tns:Form_2608M_Type"/>
</xs:schema>
```

Az összefűzés folyamata:

```text
NAV_2608.xsd
      |
      v
MultiformSchemaAnalyzer
      |
      +--> MAIN adapter Schema
      +--> REPEATING adapter Schema

input.zip
      |
      +--> A XML ---------> külön validáció
      +--> M XML #1 ------> külön validáció
      +--> M XML #2 ------> külön validáció
      +--> ...
      |
      v
StAX StreamingAssembler
      |
      v
Doc_2608
      |
      v
eredeti NAV_2608.xsd validáció
      |
      v
output.xml
```

## ZIP bemeneti szerződés

A ZIP-ben lévő fájlnevek nem határozzák meg, hogy egy XML főlap vagy melléklap. A modul az XML gyökéreleme alapján osztályoz.

Például ez érvényes bemenet:

```text
input.zip
├── ceg.xml
├── dolgozo_000001.xml
├── dolgozo_000002.xml
└── dolgozo_000003.xml
```

ha a gyökérelemek rendre:

```text
ceg.xml              -> Form_2608A -> MAIN
dolgozo_000001.xml   -> Form_2608M -> REPEATING
dolgozo_000002.xml   -> Form_2608M -> REPEATING
dolgozo_000003.xml   -> Form_2608M -> REPEATING
```

A következő esetek hibát eredményeznek:

- nincs főlap;
- egynél több főlap található;
- az XSD `minOccurs` értékénél kevesebb melléklap van;
- ismeretlen XML gyökérelem található a ZIP-ben;
- bármelyik részbizonylat XSD-hibás;
- az összefűzött dokumentum nem felel meg a teljes eredeti XSD-nek.

A melléklapok összefűzési sorrendje a ZIP entry nevek lexikografikus sorrendje. Emiatt sorszámozott fájloknál nullával feltöltött elnevezés ajánlott:

```text
2608M_000001.xml
2608M_000002.xml
2608M_000003.xml
```

## Build

A modul Java 17-et igényel.

Maven csomag készítése:

```bash
mvn clean package
```

A létrejövő futtatható JAR:

```text
target/nav-xsd-parser-tool-multiform-1.0.0-SNAPSHOT.jar
```

## CLI használat

A példákban:

```bash
JAR=target/nav-xsd-parser-tool-multiform-1.0.0-SNAPSHOT.jar
XSD=example/2608/schema/NAV_2608.xsd
```

### XSD elemzése

```bash
java -jar "$JAR" analyze --xsd "$XSD"
```

Várható lényegi eredmény:

```text
Document:  {https://soap.api.nav.gov.hu/definitions/model/2.0/2608/5.2}Doc_2608
Main:      MAIN Form_2608A / Form_2608A_Type / 1..1
Repeating: REPEATING Form_2608M / Form_2608M_Type / 1..unbounded
```

### Főlap adapter-XSD előállítása

```bash
java -jar "$JAR" adapter \
  --xsd "$XSD" \
  --part MAIN \
  --output example/2608/generated/NAV_2608_A.xsd
```

### Melléklap adapter-XSD előállítása

```bash
java -jar "$JAR" adapter \
  --xsd "$XSD" \
  --part REPEATING \
  --output example/2608/generated/NAV_2608_M.xsd
```

### Egy önálló főlap validálása

```bash
java -jar "$JAR" validate \
  --xsd "$XSD" \
  --part MAIN \
  --xml example/2608/input/2608A.xml
```

Sikeres esetben:

```text
VALID
```

### Egy önálló melléklap validálása

```bash
java -jar "$JAR" validate \
  --xsd "$XSD" \
  --part REPEATING \
  --xml example/2608/input/2608M_000001.xml
```

### A teljes ZIP ellenőrzése összefűzés nélkül

```bash
java -jar "$JAR" validate-package \
  --xsd "$XSD" \
  --zip example/2608/input.zip
```

### A teljes XML előállítása

```bash
mkdir -p example/2608/generated

java -jar "$JAR" merge \
  --xsd "$XSD" \
  --zip example/2608/input.zip \
  --output example/2608/generated/2608-full.xml
```

A modul először külön validálja az A- és M-lapokat. Csak akkor indul az összefűzés, ha minden részbizonylat érvényes. Az összeállított XML ezután újra validálásra kerül az eredeti `NAV_2608.xsd`-vel.

## Java API használat

A legfontosabb publikus belépési pont a `MultiformService`.

```java
Path xsd = Path.of("example/2608/schema/NAV_2608.xsd");
MultiformService service = new MultiformService(xsd);

MultiformDescriptor descriptor = service.descriptor();

ValidationResult aResult = service.validatePart(
        Path.of("example/2608/input/2608A.xml"),
        PartKind.MAIN);

ValidationResult mResult = service.validatePart(
        Path.of("example/2608/input/2608M_000001.xml"),
        PartKind.REPEATING);

if (!aResult.valid() || !mResult.valid()) {
    throw new IllegalStateException("Hibás részbizonylat.");
}

Path output = service.merge(
        Path.of("example/2608/input.zip"),
        Path.of("example/2608/generated/2608-full.xml"));
```

A teljes futtatható Java példa itt található:

```text
example/JavaApiExample.java
```

## Példa könyvtár

A modulhoz mellékelt teljes példa:

```text
example/
├── JavaApiExample.java
└── 2608/
    ├── README.md
    ├── input.zip
    ├── input/
    │   ├── 2608A.xml
    │   ├── 2608M_000001.xml
    │   ├── 2608M_000002.xml
    │   └── 2608M_000003.xml
    ├── schema/
    │   ├── NAV_2608.xsd
    │   ├── common.xsd
    │   └── ...
    └── expected/
        └── 2608-full.xml
```

A példa szándékosan minimális, XSD-valid A/M részbizonylatokat használ, hogy a multiform mechanizmus legyen jól látható. Valós használatban ugyanide kerülnek az üzleti adatokkal kitöltött A- és M-lap XML-ek.

## Biztonsági és teljesítményjellemzők

- A feldolgozás nem építi fel a teljes multiform dokumentumot DOM-ban.
- A végső XML StAX streameléssel készül, ezért sok ezer melléklap esetén is kezelhető marad a memóriaigény.
- A ZIP-ből csak a szükséges XML entry-k kerülnek streamelve feldolgozásra.
- A XML parser külső entitás- és DTD-feloldása tiltott.
- A félkész kimenet ideiglenes fájlba készül.
- Csak sikeres teljes XSD-validáció után kerül a végleges célfájl helyére.

## Fontos osztályok

| Osztály | Feladat |
|---|---|
| `MultiformService` | Publikus Java facade. |
| `MultiformSchemaAnalyzer` | A teljes XSD-ből felismeri a dokumentumot, főlapot és melléklapot. |
| `MultiformDescriptor` | A felismert multiform struktúra leírása. |
| `AdapterSchemaGenerator` | MAIN/REPEATING adapter-XSD előállítása. |
| `PartValidator` | Önálló részbizonylat validálása. |
| `ZipPackageInspector` | ZIP-tartalom ellenőrzése és részbizonylatok osztályozása. |
| `StreamingAssembler` | A teljes dokumentum StAX streaming összefűzése. |
| `FullDocumentValidator` | Az elkészült teljes XML validálása az eredeti XSD-vel. |
| `MultiformCli` | Parancssori belépési pont. |

## További dokumentáció

- `docs/MULTIFORM_ARCHITECTURE.md` – architektúra és belső működés;
- `docs/PROJECT_INTEGRATION.md` – integráció a teljes NAV XSD Parser Tool projektbe;
- `example/2608/README.md` – a mellékelt konkrét 2608 példa futtatása.
