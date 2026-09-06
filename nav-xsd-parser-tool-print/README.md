# nav-xsd-parser-tool-print

## A modul célja

A `nav-xsd-parser-tool-print` modul az M2M XML EDITOR nyomtatási funkcióit valósítja meg. Feladata, hogy egy NAV M2M XML állományból az XML-hez tartozó XSD- és UIModel-információk felhasználásával nyomtatásra optimalizált HTML-, illetve PDF-dokumentumot állítson elő.

A modul nem egyszerűen az XML nyers tartalmát írja ki. A feldolgozási réteg segítségével felépíti az űrlap szerkezetét és az XML-ből származó űrlapadatokat, majd ezekből készíti el az A4-es megjelenítésre optimalizált nyomtatási képet.

A modul önálló Spring Boot alkalmazást nem indít. Könyvtármodulként használható, szolgáltatásait más modulok – jelenleg elsősorban a webes és a parancssori modul – hívják meg.

## Fő feladatai

A modul főbb felelősségei:

- XML dokumentum feldolgozása nyomtatási célra;
- a dokumentumhoz tartozó XSD és UIModel információk felhasználása;
- az űrlap szerkezeti definíciójának felépítése;
- az XML mezőértékeinek űrlapadat-modellé alakítása;
- nyomtatásra optimalizált HTML előállítása;
- a HTML PDF-dokumentummá alakítása;
- ismétlődő és táblázatos űrlapstruktúrák nyomtatási megjelenítése;
- dokumentum-metaadatok, például XML-típus, fájlútvonal, hash és időpontok megjelenítése;
- opcionális UIModel felülbírálás kezelése;
- a mezőazonosítók és az üres mezők megjelenítésének szabályozása.

## Belépési pont

A modul programozói belépési pontja az alábbi interfész:

```text
hu.gov.nav.xsdparsertool.print.service.XmlPrintService
```

Ez a modul publikus szolgáltatási szerződése. Két fő műveletet biztosít:

```java
String generateHtml(...)
byte[] generatePdf(...)
```

Az alapértelmezett implementáció:

```text
hu.gov.nav.xsdparsertool.print.service.DefaultXmlPrintService
```

A jelenlegi kódban a hívó modulok közvetlenül ezt az implementációt példányosítják:

```java
XmlPrintService service = new DefaultXmlPrintService();
```

A modulnak nincs saját `main` metódusa, REST controllere vagy önálló futtatási belépési pontja.

## A modul szolgáltatásai

### HTML-generálás

A `XmlPrintService.generateHtml(...)` egy teljes, önálló HTML-dokumentumot ad vissza `String` formában.

A híváshoz az alábbi adatok szükségesek:

- a feldolgozandó XML állomány;
- az űrlapsablon XSD-sémáinak gyökérkönyvtára;
- az általános XSD-k könyvtára, ha szükséges;
- a UIModel állományok könyvtára, ha szükséges;
- a nyomtatási opciók.

Példa közvetlen Java-hívásra:

```java
XmlPrintService service = new DefaultXmlPrintService();

PrintOptions options = new PrintOptions();
options.setShowFieldIds(false);
options.setOnlyFilledFields(false);

String html = service.generateHtml(
        xmlFile,
        schemaRootDir,
        generalXsdDir,
        uiModelDir,
        options
);
```

Ha a `PrintOptions` értéke `null`, az implementáció alapértelmezett beállításokat használ.

### PDF-generálás

A `XmlPrintService.generatePdf(...)` ugyanabból a bemenetből PDF-dokumentumot készít, és annak tartalmát `byte[]` formában adja vissza.

Példa:

```java
XmlPrintService service = new DefaultXmlPrintService();

byte[] pdf = service.generatePdf(
        xmlFile,
        schemaRootDir,
        generalXsdDir,
        uiModelDir,
        options
);
```

A PDF-generálás először elkészíti a nyomtatható HTML-t, majd azt az OpenHTMLToPDF könyvtár segítségével PDF-formátummá alakítja.

## Nyomtatási beállítások

A nyomtatási működés az alábbi osztállyal szabályozható:

```text
hu.gov.nav.xsdparsertool.print.model.PrintOptions
```

A jelenlegi beállítások:

| Beállítás | Jelentés |
|---|---|
| `showFieldIds` | Megjelenjenek-e a mezők technikai azonosítói. |
| `onlyFilledFields` | Csak a kitöltött mezők kerüljenek-e a kimenetbe. |
| `uiModelOverrideFile` | Opcionális UIModel állomány, amely felülírhatja az automatikusan feloldott UIModelt. |
| `appVersion` | A nyomtatási metaadatok között megjelenített alkalmazásverzió. |

## Feldolgozási folyamat

A nyomtatás fő folyamata:

```text
XML állomány
    ↓
XmlProcessingService
    ↓
ProcessingResult + SchemaBundle
    ↓
FormDefinitionBuilderService
    ↓
FormDefinition
    ↓
FormDataBuilderService
    ↓
FormData
    ↓
nyomtatási HTML
    ↓
OpenHTMLToPDF
    ↓
PDF
```

A `DefaultXmlPrintService` a `nav-xsd-parser-tool-processing` modul szolgáltatásaira támaszkodik az XML felismeréséhez, a sémaadatok feloldásához, valamint az űrlapdefiníció és az űrlapadat előállításához.

## Hogyan használja a webes alkalmazás?

A webes modulban a nyomtatási szolgáltatást az alábbi controller hívja:

```text
nav-xsd-parser-tool-web
└── hu.gov.nav.xsdparsertool.web.api.PrintController
```

A controller két REST végpontot biztosít:

```text
POST /api/print/html
POST /api/print/pdf
```

Mindkét végpont `multipart/form-data` kérést fogad. A webes réteg előkészíti és ellenőrzi a bemeneti fájlokat és útvonalakat, létrehozza a `PrintOptions` objektumot, majd meghívja az `XmlPrintService` megfelelő műveletét.

A HTML végpont `text/html`, a PDF végpont `application/pdf` választ ad.

## Hogyan használja a parancssori alkalmazás?

A CLI modul HTML-generáláshoz közvetlenül használja a nyomtatási szolgáltatást:

```text
nav-xsd-parser-tool-cli
└── hu.gov.nav.xsdparsertool.cli.command.PrintHtmlCommand
```

A parancs neve:

```text
print-html
```

A CLI parancs létrehoz egy `DefaultXmlPrintService` példányt, összeállítja a `PrintOptions` objektumot, meghívja a `generateHtml(...)` metódust, majd az eredményt a megadott kimeneti fájlba írja.

A fontosabb CLI paraméterek:

```text
--xml
--schema-dir
--general-xsd-dir
--ui-model-dir
--ui-model
--show-field-ids
--only-filled-fields
--out
```

## Fő osztályok

### `XmlPrintService`

A modul publikus szolgáltatási interfésze. A HTML- és PDF-generálás szerződését definiálja.

### `DefaultXmlPrintService`

Az `XmlPrintService` alapértelmezett implementációja. Elvégzi a feldolgozási pipeline meghívását, a HTML összeállítását és a PDF-generálást.

### `PrintOptions`

A nyomtatás opcionális beállításait tartalmazó modell.

## Függőségek

A modul Maven szinten közvetlenül az alábbi fontosabb komponensekre támaszkodik:

- `nav-xsd-parser-tool-processing` – XML feldolgozás, űrlapdefiníció és űrlapadat előállítása;
- `openhtmltopdf-pdfbox` – HTML-ből PDF előállítása;
- `slf4j-api` – naplózás.

A tesztekhez a `spring-boot-starter-test` függőség áll rendelkezésre.

## Kapcsolat más modulokkal

A modul tipikus helye az alkalmazásban:

```text
nav-xsd-parser-tool-web / nav-xsd-parser-tool-cli
                    ↓
       nav-xsd-parser-tool-print
                    ↓
    nav-xsd-parser-tool-processing
                    ↓
      közös modellek és sémakezelés
```

A `print` modul ezért elsősorban egy újrahasznosítható szolgáltatási réteg: nem kezeli a teljes HTTP-kérést, a felhasználói felületet vagy a CLI argumentumfeldolgozást, hanem a nyomtatási kimenet tényleges előállítására koncentrál.

## Fejlesztői irányelv

A modul módosításakor külön kell választani:

- a nyomtatási adatok előállítását;
- a HTML struktúráját;
- a nyomtatási CSS szabályokat;
- a PDF-konverziót;
- a hívó webes vagy CLI réteg feladatait.

A képernyős űrlapmegjelenítés és a nyomtatási megjelenítés nem ugyanaz a felelősség. A nyomtatási kimenet A4-es dokumentumra, oldaltörésekre és többoldalas táblázatokra optimalizált.
