# nav-xsd-parser-tool-uimodel

## A modul célja

A `nav-xsd-parser-tool-uimodel` modul az M2M XML EDITOR UIModel állományainak feldolgozásáért felel. A UIModel az XSD szerkezeti információit olyan megjelenítési adatokkal egészíti ki, amelyekből az alkalmazás közérthetőbb, vizuálisan rendezett űrlapot tud felépíteni.

A modul többek között kiolvassa a UIModelből a dokumentum címét, a szekciókat, a mezőcsoportokat, a mezők feliratait, típusát, maszkját, hosszkorlátját és egyes elrendezési tulajdonságait.

A modul önálló alkalmazást nem indít. Könyvtármodulként használható, szolgáltatásait elsősorban a feldolgozó és űrlapdefiníció-építő réteg hívja.

## Fő feladatai

A modul főbb felelősségei:

- XML formátumú UIModel állomány beolvasása;
- dokumentumszintű UIModel metaadatok feloldása;
- menü- és asszisztensszekciók feldolgozása;
- `FieldGroup` elemek és mezőazonosítók összegyűjtése;
- mezőfeliratok és layoutból örökölt alcímek feloldása;
- mezőtípus, maszk, maximális hossz és elrendezési szélesség kiolvasása;
- azonos mezőazonosító többszöri előfordulásakor a metaadatok összevonása;
- a UIModel metaadatainak alkalmazása az XSD-ből felépített `DocumentDefinition` modellre;
- biztonságosan konfigurált XML parser használata külső DTD-k és külső entitások tiltásával.

## Belépési pont

A modul elsődleges szolgáltatási szerződése:

```text
hu.gov.nav.xsdparsertool.uimodel.service.UiModelParserService
```

Az interfész egy műveletet definiál:

```java
void applyUiModel(DocumentDefinition definition, Path uiModelFile)
```

A tényleges XML-feldolgozást végző implementáció:

```text
hu.gov.nav.xsdparsertool.uimodel.service.XmlUiModelParserService
```

Ez az implementáció az interfész műveletén kívül közvetlen parser API-t is biztosít:

```java
UiModelMetadata parse(Path uiModelFile)
```

A modul tartalmaz egy szándékosan nem módosító implementációt is:

```text
hu.gov.nav.xsdparsertool.uimodel.service.NoOpUiModelParserService
```

Ez akkor használható, amikor a feldolgozási folyamatnak meg kell tartania a `UiModelParserService` szerződést, de UIModel-adatokat nem kell alkalmaznia.

A modulnak nincs saját `main` metódusa, REST controllere vagy Spring Boot indítási pontja.

## A modul szolgáltatásai

### UIModel feldolgozása metaadatmodellé

Az `XmlUiModelParserService.parse(...)` egy UIModel XML állományból `UiModelMetadata` objektumot készít.

Példa:

```java
XmlUiModelParserService parser = new XmlUiModelParserService();
UiModelMetadata metadata = parser.parse(uiModelFile);
```

A visszaadott modellből többek között elérhető:

```java
metadata.getDocumentId();
metadata.getTitle();
metadata.getInfo();
metadata.getVersion();
metadata.getType();
metadata.getSections();
metadata.getBlockGroupsById();
metadata.getFieldsById();
```

A `UiModelMetadata` három fontos belső modellt használ:

- `Section` – menü- vagy asszisztensszekció és a hozzá tartozó mezőcsoportok;
- `BlockGroup` – UIModel mezőcsoport és a hozzá tartozó mezőazonosítók;
- `FieldUi` – egy mező megjelenítési és beviteli metaadatai.

### UIModel alkalmazása XSD-alapú dokumentumdefinícióra

A `UiModelParserService.applyUiModel(...)` meglévő `DocumentDefinition` példányt egészít ki a UIModel adataival.

Példa:

```java
UiModelParserService parser = new XmlUiModelParserService();
parser.applyUiModel(documentDefinition, uiModelFile);
```

A tényleges implementáció a forráskód jelenlegi működése szerint módosíthatja:

- a dokumentum címét;
- a blokkok nevét és címét;
- a mezők UI-címkéjét és általános címkéjét;
- a mező adattípusát;
- a mező maszkját;
- a mező maximális hosszát.

Az alkalmazás a blokk- és mezőazonosítókat egyezteti az XSD-ből származó definíció és a UIModel között. Az eltérésekről naplóbejegyzés készül.

### No-op működés

A `NoOpUiModelParserService` ugyanazt a szolgáltatási interfészt valósítja meg, de a kapott `DocumentDefinition` példányt változatlanul hagyja.

Példa:

```java
UiModelParserService parser = new NoOpUiModelParserService();
parser.applyUiModel(documentDefinition, uiModelFile);
```

Ez a jelenlegi `DefaultXmlProcessingService` paraméter nélküli konstruktorának alapértelmezett UIModel szolgáltatása.

## Feldolgozási folyamat

A tényleges XML UIModel feldolgozás fő folyamata:

```text
UIModel XML
    ↓
XmlUiModelParserService.parse(...)
    ↓
UiModelMetadata
    ├── dokumentum metaadatok
    ├── Section lista
    ├── BlockGroup index
    └── FieldUi index
```

Ha a metaadatokat egy XSD-ből felépített definícióra is alkalmazni kell:

```text
XSD
 ↓
DocumentDefinition
 ↓
XmlUiModelParserService.applyUiModel(...)
 ↑
UIModel XML → UiModelMetadata
 ↓
UIModel adatokkal kiegészített DocumentDefinition
```

## Címkefeloldás

A parser nem minden esetben használja közvetlenül a `Field` elem saját `label` attribútumát.

A forráskód figyelembe veszi a környező `Layout` elemek `SUBTITLE` és `TITLEGROUP` feliratait is. Bizonyos technikai mezőcímkék, illetve sorszámozott táblázati címkék esetén az örökölt alcím lehet a használható megjelenítési címke.

Az azonos mezőazonosító többszöri előfordulásakor a parser a már összegyűjtött metaadatot kiegészítheti a később talált, használhatóbb értékekkel.

## Biztonságos XML-feldolgozás

Az `XmlUiModelParserService` DOM parsert használ, de a parser biztonsági beállításai tiltják többek között:

- a DOCTYPE deklarációt;
- a külső általános entitásokat;
- a külső paraméter-entitásokat;
- a külső DTD betöltést;
- a külső sémák elérését;
- az XInclude feldolgozást.

A közvetlen gyermekelemek keresésénél külön felső korlát is van, amely túl nagy közvetlen gyermeklista esetén megszakítja a feldolgozást.

## Hogyan használja a processing modul?

A `nav-xsd-parser-tool-processing` modul `DefaultXmlProcessingService` osztálya `UiModelParserService` függőséget fogad.

A feldolgozási folyamatban, ha a feloldott séma-csomaghoz UIModel állomány tartozik, az alábbi szolgáltatási műveletet hívja:

```java
uiModelParserService.applyUiModel(
        result.getDocumentDefinition(),
        bundle.getUiModelFile()
);
```

Fontos, hogy a `DefaultXmlProcessingService` paraméter nélküli konstruktorában jelenleg `NoOpUiModelParserService` kerül átadásra. A tényleges XML UIModel feldolgozáshoz `XmlUiModelParserService` példányt kell használni vagy olyan magasabb szintű komponenst kell hívni, amely ezt közvetlenül használja.

## Hogyan használja az űrlapdefiníció-építés?

A `nav-xsd-parser-tool-processing` modul `DefaultFormDefinitionBuilderService` osztálya közvetlenül létrehoz egy `XmlUiModelParserService` példányt, és UIModel metaadatokat olvas belőle az űrlapdefiníció felépítéséhez.

Ez a használati mód a közvetlen parser API-ra épül:

```java
UiModelMetadata metadata = uiModelParserService.parse(uiModelFile);
```

## További közvetlen használat

A web modul `ApiResponseMapper` osztálya szintén közvetlenül meghívja az `XmlUiModelParserService.parse(...)` műveletét, amikor UIModel metaadatokra van szüksége a válasz összeállításához.

A modul tehát kétféle módon használható:

1. `UiModelParserService` szerződésen keresztül egy dokumentumdefiníció kiegészítésére;
2. `XmlUiModelParserService.parse(...)` közvetlen hívásával a UIModel önálló metaadatmodelljének előállítására.

## Fő osztályok

### `UiModelParserService`

A modul publikus szolgáltatási szerződése. A már felépített dokumentumdefiníció UIModel-adatokkal történő kiegészítését definiálja.

### `XmlUiModelParserService`

A tényleges XML parser és UIModel-alkalmazó implementáció. Beolvassa az UIModel XML-t, felépíti a `UiModelMetadata` modellt, és szükség esetén az adatokat rávezeti egy `DocumentDefinition` példányra.

### `NoOpUiModelParserService`

Üres implementáció, amely nem változtatja meg a dokumentumdefiníciót.

### `UiModelMetadata`

A feldolgozott UIModel adatmodellje. Dokumentum-, szekció-, mezőcsoport- és mezőszintű metaadatokat tartalmaz.

## Függőségek

A modul Maven szinten közvetlenül az alábbi projektmodulra támaszkodik:

- `nav-xsd-parser-tool-core` – a `DocumentDefinition`, `BlockDefinition` és `FieldDefinition` közös modellek miatt.

A modul a JDK DOM XML API-ját használja az UIModel feldolgozásához.

A tesztekhez a `spring-boot-starter-test` függőség áll rendelkezésre.

## Kapcsolat más modulokkal

A modul tipikus helye az alkalmazásban:

```text
UIModel XML
    ↓
nav-xsd-parser-tool-uimodel
    ↓
UiModelMetadata / kiegészített DocumentDefinition
    ↓
nav-xsd-parser-tool-processing
    ↓
nav-xsd-parser-tool-web / nyomtatás / egyéb fogyasztók
```

A `uimodel` modul nem felel az XSD teljes feldolgozásáért, az XML értékek beolvasásáért vagy a webes űrlap kirajzolásáért. Feladata a UIModelből származó megjelenítési metaadatok értelmezése és átadása a további rétegek számára.

## Fejlesztői irányelv

A modul módosításakor különösen fontos megőrizni:

- a UIModel-first megjelenítési adatokat;
- a mezőazonosítók alapján történő metaadat-kapcsolást;
- a layoutból örökölt címkék feloldását;
- az azonos mezőazonosítóhoz tartozó metaadatok összevonási szabályait;
- a biztonságos XML parser beállításait.

A vizuális megjelenítés tényleges felépítése nem ennek a modulnak a feladata; a modul az ehhez szükséges UIModel metaadatokat szolgáltatja.
