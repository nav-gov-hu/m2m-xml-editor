# nav-xsd-parser-tool-processing

## Mire való ez a modul?

A `nav-xsd-parser-tool-processing` az M2M XML EDITOR központi XML-feldolgozó modulja. Összeköti az XML felismerését, a megfelelő XSD/UIModel erőforrások feloldását, az XSD-alapú dokumentumdefiníció felépítését, az űrlapmodell előállítását, az XSD-validációt és az XML fa-nézethez szükséges adatmodell létrehozását.

A modul nem webes végpontokat biztosít, hanem Java szolgáltatásokat ad a `web`, `cli`, `print` és más modulok számára.

## Fő belépési pont

A modul legfontosabb programozói belépési pontja:

```java
XmlProcessingService
```

Alapértelmezett implementációja:

```java
DefaultXmlProcessingService
```

Egyszerű használat:

```java
XmlProcessingService service = new DefaultXmlProcessingService();
ProcessingResult result = service.inspect(xmlFile, schemaRootDir);
```

## Fő szolgáltatások

### XML feldolgozás

Az `inspect(...)` metódusok feloldják az XML-hez tartozó séma-csomagot és felépítik a dokumentumdefiníciót.

A feldolgozás fő lépései:

1. XML alapmetaadatok kiolvasása `XmlProbeService` segítségével;
2. `SchemaBundle` feloldása a Schema Registryből;
3. XSD strukturális modell felépítése;
4. UIModel alkalmazása, ha rendelkezésre áll;
5. Page Schema alkalmazása, ha rendelkezésre áll.

Példa:

```java
ProcessingResult result = service.inspect(
        xmlFile,
        schemaRootDir,
        generalXsdDir,
        uiModelDir
);
```

### XSD-validáció

A `validate(...)` metódusok ellenőrzik a szükséges fájlokat és könyvtárakat, feloldják a sémát, majd lefuttatják az XSD-validációt.

```java
ValidationResult validation = service.validate(
        xmlFile,
        schemaRootDir,
        generalXsdDir,
        uiModelDir
);
```

A validációs hibákhoz a modul lehetőség szerint pontos, előfordulási indexeket is tartalmazó XML-útvonalat rendel. Ez teszi lehetővé, hogy a webes felület az érintett mezőhöz tudjon navigálni.

### Minimális XML generálása

A `generateEmptyXml(...)` szolgáltatás dokumentumtípus és XSD alapján minimális XML állományt készít.

```java
ExportResult result = service.generateEmptyXml(
        documentType,
        schemaRootDir,
        outputFile
);
```

## Űrlapdefiníció építése

Belépési pont:

```java
FormDefinitionBuilderService
```

Alapértelmezett implementáció:

```java
DefaultFormDefinitionBuilderService
```

Használat:

```java
FormDefinitionBuilderService builder = new DefaultFormDefinitionBuilderService();
FormDefinition form = builder.build(documentDefinition, schemaBundle);
```

A builder **UIModel-first** módon működik. Ha UIModel rendelkezésre áll, annak vizuális szerkezetét használja. Ha nincs megfelelő UIModel, az XSD-ből felépített `DocumentDefinition` alapján strukturális fallback űrlapot készít.

## XML értékek betöltése az űrlapba

Belépési pont:

```java
FormDataBuilderService
```

Alapértelmezett implementáció:

```java
DefaultFormDataBuilderService
```

Használat:

```java
FormDataBuilderService builder = new DefaultFormDataBuilderService();
FormData data = builder.build(formDefinition, xmlFile);
```

A szolgáltatás a mezők XML-útvonalai alapján kiolvassa az értékeket, és az ismétlődő sorok külön példányait is kezeli.

## XML fa-nézet

Belépési pont:

```java
XmlViewBuilderService
```

Alapértelmezett implementáció:

```java
DefaultXmlViewBuilderService
```

Használat:

```java
XmlDocumentView view = new DefaultXmlViewBuilderService().build(xmlFile);
```

A faelemek útvonala előfordulási indexeket is tartalmaz, például:

```text
/Root/Item[1]/Field[1]
/Root/Item[2]/Field[1]
```

## XML gyors azonosítása

A `XmlProbeService` a sémafeloldás előtt kiolvassa:

- a gyökérelem nevét;
- a namespace-t;
- az `xsi:schemaLocation` értékét;
- az `xsi:noNamespaceSchemaLocation` értékét.

```java
XmlProbeResult probe = new XmlProbeService().probe(xmlFile);
```

## XSD-validáció belső szolgáltatásai

### `XsdValidationService`

Ha már rendelkezésre áll egy `SchemaBundle`, közvetlenül is hívható:

```java
ValidationResult result = new XsdValidationService().validate(xmlFile, schemaBundle);
```

### `MultiPathResourceResolver`

A dokumentumspecifikus és az általános XSD könyvtárakból oldja fel az XSD `include` és `import` hivatkozásokat.

### `SimpleLsInput`

A lokálisan feloldott XSD erőforrást `LSInput` formában adja át a Java XML Schema validátornak.

## Hol használják a modult?

- `nav-xsd-parser-tool-web` – XML megnyitás, űrlapépítés, validáció és XML-nézet;
- `nav-xsd-parser-tool-cli` – `inspect`, `validate` és generálási parancsok;
- `nav-xsd-parser-tool-print` – űrlapdefiníció és mezőértékek előállítása nyomtatáshoz;
- `nav-xsd-parser-tool-m2m-submitter` – XML probe használata bizonylatmetaadatok felismeréséhez.

## Modulfüggőségek

A modul fő projektfüggőségei:

- `nav-xsd-parser-tool-core`;
- `nav-xsd-parser-tool-schema-registry`;
- `nav-xsd-parser-tool-xsd`;
- `nav-xsd-parser-tool-uimodel`;
- `nav-xsd-parser-tool-page-schema`.

## Felelősségi határ

A modul a feldolgozási pipeline-ért és a közös feldolgozási modellek előállításáért felel. Nem tartozik ide a REST API, a HTML frontend, az XML-ek adatbázis-nyilvántartása, a NAV M2M hálózati beküldés vagy a GitHub release-ek kezelése.
