# nav-xsd-parser-tool-cli

## Mire való ez a modul?

A `nav-xsd-parser-tool-cli` a projekt általános parancssori felülete. Olyan fejlesztői és automatizálási feladatokra használható, amelyekhez nincs szükség a webes alkalmazás elindítására.

A modul négy fő műveletet tesz elérhetővé:

- egy XML állományhoz tartozó sémacsomag felderítése;
- XML XSD-validációja;
- minimális, üres XML előállítása dokumentumtípus alapján;
- nyomtatható HTML készítése XML-ből.

A modul maga nem valósítja meg újra ezeket a feldolgozásokat. A tényleges munkát elsősorban a `nav-xsd-parser-tool-processing` és a `nav-xsd-parser-tool-print` modul szolgáltatásaira bízza.

## Belépési pont

A futtatható alkalmazás fő belépési pontja:

```text
hu.gov.nav.xsdparsertool.cli.NavXsdParserToolCliApplication
```

A Picocli gyökérparancs neve:

```text
nav-xsd-parser-tool
```

A modul Maven konfigurációja a `maven-shade-plugin` segítségével függőségeket is tartalmazó futtatható JAR-t készít, amelynek manifestje a fenti főosztályra mutat.

A gyökérparancs alparancs nélkül használati súgót jelenít meg.

## Elérhető szolgáltatások és parancsok

### `inspect`

Az `InspectCommand` egy XML állományt megvizsgál, és a processing modul segítségével feloldja a hozzá tartozó sémacsomagot.

A konzolra kiírja:

- a felismert dokumentumtípust;
- az elsődleges XSD fájlt;
- a feloldott UIModel fájlt;
- a page-schema fájlt.

Példa:

```bash
java -jar nav-xsd-parser-tool-cli/target/nav-xsd-parser-tool-cli.jar inspect \
  --xml /path/to/document.xml \
  --schema-dir /path/to/schema-root
```

A programozói háttérszolgáltatás:

```java
XmlProcessingService service = new DefaultXmlProcessingService();
ProcessingResult result = service.inspect(xmlFile, schemaDir);
```

### `validate`

A `ValidateCommand` XSD szerint validálja az XML-t. A megfelelő séma kiválasztását és a validációt a processing modul végzi.

Példa:

```bash
java -jar nav-xsd-parser-tool-cli/target/nav-xsd-parser-tool-cli.jar validate \
  --xml /path/to/document.xml \
  --schema-dir /path/to/schema-root
```

Sikeres validáció esetén a parancs `0` kilépési kódot ad vissza. Ha XSD-validációs hibák vannak, a konzolon felsorolja azok súlyosságát, üzenetét és feloldott XML-útvonalát, majd `1` kilépési kódot ad vissza.

A háttérben használt hívás:

```java
ValidationResult result = service.validate(xmlFile, schemaDir);
```

### `generate`

A `GenerateCommand` dokumentumtípus alapján minimális XML dokumentumot állít elő.

Példa:

```bash
java -jar nav-xsd-parser-tool-cli/target/nav-xsd-parser-tool-cli.jar generate \
  --document-type PMT25 \
  --schema-dir /path/to/schema-root \
  --out /path/to/output.xml
```

A tényleges feldolgozást ez a szolgáltatáshívás végzi:

```java
ExportResult result = service.generateEmptyXml(documentType, schemaDir, outputFile);
```

A parancs sikeres export esetén `0`, sikertelen export esetén `5` kilépési kódot ad vissza.

### `print-html`

A `PrintHtmlCommand` nyomtatható HTML-t készít az XML-ből a print modul segítségével.

Alap példa:

```bash
java -jar nav-xsd-parser-tool-cli/target/nav-xsd-parser-tool-cli.jar print-html \
  --xml /path/to/document.xml \
  --schema-dir /path/to/schema-root \
  --out /path/to/document.html
```

További kapcsolók:

| Paraméter | Jelentés |
|---|---|
| `--general-xsd-dir` | Opcionális általános XSD könyvtár. |
| `--ui-model-dir` | Opcionális UIModel gyökérkönyvtár. |
| `--ui-model` | Kézzel megadott UIModel fájl, amely felülírhatja az automatikus feloldást. |
| `--show-field-ids` | Megjeleníti a mezők technikai azonosítóit a nyomtatási nézetben. |
| `--only-filled-fields` | Csak a kitöltött mezőket rendereli. |
| `--out` | A létrehozandó HTML fájl. Kötelező. |

A parancs a `PrintOptions` objektumba gyűjti a nyomtatási beállításokat, majd meghívja:

```java
String html = service.generateHtml(
        xmlFile,
        schemaDir,
        generalXsdDir,
        uiModelDir,
        options
);
```

A célkönyvtárat szükség esetén létrehozza, a HTML-t pedig UTF-8 kódolással, a projekt biztonságos fájlműveleti segédjével írja ki.

## Hogyan lehet meghívni a modult?

### Parancssorból

Build után a modul futtatható JAR-ja a `target` könyvtárban található. A súgó:

```bash
java -jar nav-xsd-parser-tool-cli/target/nav-xsd-parser-tool-cli.jar --help
```

Egy adott alparancs saját súgója például:

```bash
java -jar nav-xsd-parser-tool-cli/target/nav-xsd-parser-tool-cli.jar validate --help
```

### Java kódból

A CLI osztályok közvetlenül is példányosíthatók, de üzleti vagy alkalmazáslogikából célszerű inkább az általuk használt szolgáltatási interfészeket meghívni:

```java
XmlProcessingService processingService = new DefaultXmlProcessingService();
ValidationResult result = processingService.validate(xmlFile, schemaDir);
```

Nyomtatáshoz:

```java
XmlPrintService printService = new DefaultXmlPrintService();
String html = printService.generateHtml(
        xmlFile,
        schemaDir,
        generalXsdDir,
        uiModelDir,
        options
);
```

Így a CLI csak adapter marad a parancssori argumentumok és a projekt szolgáltatásai között.

## Fő osztályok

| Osztály | Szerep |
|---|---|
| `NavXsdParserToolCliApplication` | Picocli gyökérparancs és JVM belépési pont. |
| `InspectCommand` | XML és sémacsomag felderítése. |
| `ValidateCommand` | XML XSD-validációja. |
| `GenerateCommand` | Minimális XML generálása dokumentumtípus alapján. |
| `PrintHtmlCommand` | Nyomtatható HTML létrehozása. |

## Kapcsolódó modulok

A modul közvetlen projektfüggőségei:

- `nav-xsd-parser-tool-processing` – XML-felderítés, validáció és XML-generálás;
- `nav-xsd-parser-tool-print` – nyomtatható HTML előállítása.

Fontos külső függőség:

- Picocli – parancssori parancsok, opciók és súgó kezelése.

## Felelősségi határ

A `nav-xsd-parser-tool-cli` feladata a parancssori interfész biztosítása és a felhasználói argumentumok továbbítása a megfelelő szolgáltatáshoz.

Nem ennek a modulnak a feladata:

- az XSD-k tényleges elemzése;
- a Schema Registry implementálása;
- a UIModel feldolgozása;
- az XML-validáció algoritmusának megvalósítása;
- a nyomtatási HTML renderelési logikája;
- a webes REST API kiszolgálása;
- a NAV M2M beküldés kezelése.
