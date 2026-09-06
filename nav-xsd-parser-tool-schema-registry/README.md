# nav-xsd-parser-tool-schema-registry

## A modul célja

A `nav-xsd-parser-tool-schema-registry` modul feladata annak meghatározása, hogy egy megnyitott NAV M2M XML dokumentumhoz **melyik XSD séma és mely kapcsolódó technikai erőforrások tartoznak**.

A modul nem az XML teljes feldolgozását vagy validálását végzi. A feladata a megfelelő séma-csomag feloldása. A visszaadott `SchemaBundle` alapján a magasabb szintű modulok már tudják, melyik XSD-t kell feldolgozni, mely kapcsolódó XSD-k szükségesek, illetve van-e a dokumentumhoz UIModel vagy oldalséma.

A modul önálló alkalmazást nem indít; könyvtármodulként használható.

## Fő feladatai

A modul főbb felelősségei:

- XSD állományok felderítése a konfigurált könyvtárakban;
- az XSD-k könnyűsúlyú metaadat-indexének felépítése;
- a `targetNamespace` kiolvasása;
- a globális gyökérelemek összegyűjtése;
- az `xs:import`, `xs:include` és `xs:redefine` sémahivatkozások összegyűjtése;
- az XML gyökéreleme, namespace-e és sémahelyei alapján a legjobb XSD kiválasztása;
- ismert dokumentumtípus alapján XSD feloldása;
- azonos tartalmi találat esetén a megfelelő release-verzió kiválasztása;
- a kapcsolódó XSD-k összegyűjtése;
- UIModel és `.schema` kísérőfájl keresése;
- a feloldási eredmény `SchemaBundle` objektummá összeállítása;
- a sémaindex memóriabeli cache-elése;
- aszinkron előtöltés és újraindexelés;
- az indexelés előrehaladási állapotának szolgáltatása.

## Belépési pont

A modul elsődleges programozói belépési pontja:

```text
hu.gov.nav.xsdparsertool.schemaregistry.service.SchemaRegistryService
```

A tényleges fájlrendszer-alapú implementáció:

```text
hu.gov.nav.xsdparsertool.schemaregistry.service.FileSystemSchemaRegistryService
```

A szolgáltatás két fő feloldási módot biztosít:

```java
SchemaBundle resolveByXmlProbe(...)
SchemaBundle resolveByDocumentType(...)
```

A modulnak nincs saját `main` metódusa vagy Spring Boot alkalmazásindító osztálya.

## A modul szolgáltatásai

### Séma feloldása XML elővizsgálati adatokból

A legteljesebb szolgáltatási hívás:

```java
SchemaBundle bundle = schemaRegistryService.resolveByXmlProbe(
        probeResult,
        schemaRootDir,
        generalXsdDir,
        uiModelDir
);
```

A `probeResult` típusa:

```text
hu.gov.nav.xsdparsertool.schemaregistry.model.XmlProbeResult
```

A modell a következő XML-szintű azonosító adatokat tartalmazza:

- gyökérelem neve;
- gyökérelem namespace URI-ja;
- `xsi:schemaLocation`;
- `xsi:noNamespaceSchemaLocation`.

A projektben ezeket az adatokat tipikusan a `nav-xsd-parser-tool-processing` modul `XmlProbeService` szolgáltatása állítja elő.

Egyszerűbb hívások is rendelkezésre állnak:

```java
SchemaBundle bundle = schemaRegistryService.resolveByXmlProbe(
        probeResult,
        schemaRootDir
);
```

vagy:

```java
SchemaBundle bundle = schemaRegistryService.resolveByXmlProbe(
        probeResult,
        schemaRootDir,
        generalXsdDir
);
```

### Séma feloldása dokumentumtípus alapján

Ha a dokumentumtípus már ismert, a registry közvetlenül is meghívható:

```java
SchemaBundle bundle = schemaRegistryService.resolveByDocumentType(
        documentType,
        schemaRootDir,
        generalXsdDir,
        uiModelDir
);
```

Például:

```java
SchemaBundle bundle = schemaRegistryService.resolveByDocumentType(
        "26HIPAK",
        schemaRootDir,
        commonXsdDir,
        uiModelDir
);
```

Ha a dokumentumtípushoz friss index után sem található megfelelő XSD, a szolgáltatás `IllegalArgumentException` kivételt dob.

## Mit tartalmaz a `SchemaBundle`?

A feloldás eredménye a `nav-xsd-parser-tool-core` modulban található:

```text
hu.gov.nav.xsdparsertool.core.model.bundle.SchemaBundle
```

A registry a következő fontos adatokat állítja be rajta:

- dokumentumtípus;
- dokumentumverzió;
- gyökérelem neve;
- target namespace;
- a kiválasztás indoklása;
- elsődleges XSD;
- kapcsolódó XSD állományok;
- UIModel állomány, ha található;
- oldalséma (`.schema`) állomány, ha található.

Tipikus felhasználás:

```java
Path primaryXsd = bundle.getPrimaryXsd();
List<Path> xsdFiles = bundle.getXsdFiles();
Path uiModelFile = bundle.getUiModelFile();
Path pageSchemaFile = bundle.getPageSchemaFile();
```

## Hogyan történik a séma kiválasztása?

XML elővizsgálatból történő feloldáskor a `FileSystemSchemaRegistryService` pontozza az XSD jelölteket.

A tényleges forráskód jelenlegi logikája figyelembe veszi:

1. az XML namespace-éhez tartozó `xsi:schemaLocation` értéket;
2. az `xsi:noNamespaceSchemaLocation` értéket;
3. a gyökérelem pontos egyezését;
4. a namespace egyezését;
5. a gyökérelem és namespace együttes egyezését.

A `schemaLocation` több namespace–sémahely párt is tartalmazhat. A registry nem egyszerűen az utolsó tokent választja ki, hanem az XML gyökérelemének namespace-éhez tartozó párt keresi meg.

Ha az elővizsgálati pontozás nem ad használható találatot, a szolgáltatás a gyökérelem nevét dokumentumtípusként használva megpróbálja a dokumentumtípus-alapú feloldást.

## Dokumentumtípus-alapú feloldás

Dokumentumtípus alapján a registry elsősorban:

- az XSD fájlnevét;
- a globális gyökérelemek neveit

hasonlítja össze a keresett dokumentumtípussal.

A kereséshez a technikai elválasztó karakterek normalizálásra kerülnek, így a kötőjel, aláhúzás és pont nem akadályozza meg az azonosítást.

Ha a cache-ben nincs megfelelő találat, a szolgáltatás egyszer szinkron módon újraolvassa az XSD állományokat, majd megismétli a keresést.

## Főverzió és release-patch verzió

A modul külön kezeli a dokumentum verzióját és a repository release-verzióját.

A dokumentumverzió meghatározásának sorrendje:

1. XSD `targetNamespace` utolsó verziószegmense;
2. az XSD-t tartalmazó verziókönyvtár neve;
3. fájlnévből kinyerhető verzió mint fallback.

Ez azért fontos, mert a dokumentumtípus nevében szereplő szám nem feltétlenül verziószám.

Azonos tartalmi találat esetén a registry a repository release-verziót is összehasonlítja. Így például azonos főverzión belül a magasabb patch release előnyt élvezhet.

## UIModel feloldása

A registry a kiválasztott XSD mellett megpróbálja megkeresni a dokumentumhoz tartozó UIModel állományt is.

A támogatott fájlnévminták között szerepelnek például:

```text
*.uimodel.xml
uimodel.xml
uimodel_*.xml
```

A keresés figyelembe veszi a dokumentumtípust és a dokumentum főverzióját. Több, azonos főverzióhoz tartozó UIModel esetén a forráskód release-verzió összehasonlítást használ, és a nagyobb release-patch verziót részesíti előnyben.

A keresési sorrend a kiválasztott XSD környezetéből indul, majd szükség esetén a külön megadott UIModel gyökérkönyvtárra és a séma gyökérkönyvtárára is kiterjed.

## Kapcsolódó XSD-k

Az `XsdFileDescriptor` az XSD közvetlen:

- `xs:import`;
- `xs:include`;
- `xs:redefine`

hivatkozásainak `schemaLocation` értékeit is tárolja.

A registry ezekből összeállítja a `SchemaBundle.xsdFiles` listát. Először a relatív elérési utat próbálja feloldani, majd szükség esetén az indexelt XSD-k között azonos fájlnév alapján keres.

## XSD index és cache

A `FileSystemSchemaRegistryService` nem olvassa újra minden feloldási hívásnál az összes XSD-t.

A leírókat egy memóriabeli cache-ben tárolja. A cache kulcsa a:

```text
schemaRootDir + generalXsdDir
```

könyvtárkombinációból készül.

Az index egy `XsdFileDescriptor` objektumot tart fenn minden XSD-hez. A descriptor tartalmazza:

```text
XSD elérési út
    ├── targetNamespace
    ├── globális gyökérelemek
    └── kapcsolódó schemaLocation értékek
```

## Aszinkron előtöltés

A `FileSystemSchemaRegistryService` létrehozható előre megadott könyvtárakkal:

```java
FileSystemSchemaRegistryService registry =
        new FileSystemSchemaRegistryService(schemaRootDir, generalXsdDir);
```

Ebben az esetben a konstruktor automatikusan aszinkron indexépítést indít.

Az előtöltés külön is elindítható:

```java
registry.preloadAsync();
```

Újraindexelés:

```java
registry.reloadAsync(schemaRootDir, generalXsdDir);
```

A `reloadAsync(...)` eltávolítja az adott könyvtárkombináció meglévő cache-bejegyzését, majd háttérben újraépíti az indexet.

## Indexelési állapot

Az aktuális állapot lekérdezhető:

```java
SchemaRegistryStatus status = registry.getStatus();
```

A státusz többek között tartalmazza:

- folyamatban van-e az indexelés;
- használatra kész-e a registry;
- az aktuális feldolgozási fázist;
- a feldolgozott fájlok számát;
- a teljes fájlszámot;
- a százalékos előrehaladást;
- a cache-bejegyzések számát;
- az aktív séma- és common XSD könyvtárat.

## Biztonságos XSD metaadat-beolvasás

A registry az XSD metaadatok kiolvasásához DOM parsert használ, de a parser külső erőforrások elérését tiltó beállításokkal működik.

A forráskód többek között tiltja:

- a DOCTYPE deklarációt;
- a külső általános entitásokat;
- a külső paraméter-entitásokat;
- a külső DTD-k betöltését;
- az XInclude használatát;
- a külső DTD és külső schema hozzáférést.

A registry ebben a lépésben nem az XSD teljes üzleti modelljét építi fel, csak a séma kiválasztásához szükséges metaadatokat olvassa ki.

## Hogyan használja a processing modul?

A `nav-xsd-parser-tool-processing` modul `DefaultXmlProcessingService` osztálya `SchemaRegistryService` függőséget használ.

Az `inspect(...)` folyamatban a lépések:

```text
XML fájl
   ↓
XmlProbeService.probe(...)
   ↓
XmlProbeResult
   ↓
SchemaRegistryService.resolveByXmlProbe(...)
   ↓
SchemaBundle
   ↓
XSD parser / UIModel / PageSchema feldolgozás
```

A tényleges hívás:

```java
XmlProbeResult probe = xmlProbeService.probe(xmlFile);
SchemaBundle bundle = schemaRegistryService.resolveByXmlProbe(
        probe,
        schemaRootDir,
        generalXsdDir,
        uiModelDir
);
```

## Hogyan használja az M2M submitter modul?

A `nav-xsd-parser-tool-m2m-submitter` modul `XmlBizonylatMetadataExtractor` komponense először a schema registry segítségével próbálja meghatározni a bizonylat típusát és verzióját.

A registryből kapott `SchemaBundle` alapján használja többek között:

```java
bundle.getDocumentType();
bundle.getDocumentVersion();
bundle.getTargetNamespace();
bundle.getPrimaryXsd();
bundle.getMatchReason();
```

Ha a registry alapú feloldás nem sikerül, az M2M submitter saját fallback logikát is használhat; ez már nem ennek a modulnak a felelőssége.

## Hogyan érhető el a webes alkalmazásból?

A REST réteg nem ebben a modulban, hanem a `nav-xsd-parser-tool-web` modulban található.

A `SchemaRegistryController` a `FileSystemSchemaRegistryService` állapot- és újratöltési műveleteit teszi elérhetővé.

Aktuális állapot:

```text
GET /api/schema-registry/status
```

Újraindexelés indítása:

```text
POST /api/schema-registry/reload
```

A REST controller a web modul konfigurációjából veszi a séma- és common XSD könyvtárakat, majd a registry `reloadAsync(...)` metódusát hívja.

## Fő osztályok

### `SchemaRegistryService`

A modul elsődleges szolgáltatási szerződése. XML-elővizsgálatból vagy dokumentumtípusból old fel `SchemaBundle` objektumot.

### `FileSystemSchemaRegistryService`

A szolgáltatás fájlrendszer-alapú implementációja. Felépíti és cache-eli az XSD metaadat-indexet, pontozza a jelölteket, valamint összeállítja a teljes séma-csomagot.

### `XmlProbeResult`

Az XML gyors elővizsgálatának eredménye. A schema registry számára szükséges gyökérelem-, namespace- és sémahelyadatokat hordozza.

### `XsdFileDescriptor`

Egy indexelt XSD állomány metaadatait tartalmazza.

### `SchemaRegistryStatus`

Az aszinkron indexelés és a cache aktuális állapotát hordozza.

## Függőségek

A modul Maven szinten közvetlenül az alábbi projektmodulra támaszkodik:

- `nav-xsd-parser-tool-core` – elsősorban a `SchemaBundle` és közös segédfunkciók miatt.

A modul a JDK DOM XML API-ját használja az XSD metaadatok biztonságos kiolvasásához.

A tesztekhez a `spring-boot-starter-test` függőség áll rendelkezésre.

## Kapcsolat más modulokkal

A modul tipikus helye az alkalmazás feldolgozási láncában:

```text
XML
 ↓
XmlProbeService
 ↓
XmlProbeResult
 ↓
nav-xsd-parser-tool-schema-registry
 ↓
SchemaBundle
 ├── primary XSD
 ├── kapcsolódó XSD-k
 ├── UIModel
 └── PageSchema
 ↓
nav-xsd-parser-tool-processing
 ↓
web / nyomtatás / validáció / M2M funkciók
```

A registry **nem** felel:

- az XML mezőértékeinek feldolgozásáért;
- az XSD teljes dokumentumdefinícióvá alakításáért;
- az XSD-validáció tényleges végrehajtásáért;
- a UIModel tartalmának értelmezéséért;
- a webes felület kirajzolásáért.

Feladata annak biztosítása, hogy a további rétegek a megfelelő dokumentumtípushoz és verzióhoz tartozó technikai erőforrásokat kapják meg.

## Fejlesztői irányelv

A schema registry módosításakor különösen fontos, hogy a feloldás továbbra is a dokumentum teljes azonosítási kontextusát használja. A gyökérelem neve önmagában nem minden esetben elegendő; a namespace és a schemaLocation is fontos azonosítási adat.

Multiform dokumentumoknál ugyanígy meg kell őrizni a dokumentumtípus és verzió helyes feloldását.

A dokumentum főverzióját és a repository release-patch verzióját külön fogalomként kell kezelni. Azonos főverzión belül több release is rendelkezésre állhat, ezért a release-kiválasztási logikát nem szabad egyszerű fájlnév-egyezésre visszabutítani.
