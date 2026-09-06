# nav-xsd-parser-tool-page-schema

## A modul célja

A `nav-xsd-parser-tool-page-schema` modul a külön lapleíró sémafájlok (`.schema`) feldolgozásának helyét és szolgáltatási szerződését biztosítja az M2M XML EDITOR-ban.

A rendszer az XML-hez tartozó XSD alapján először felépíti a dokumentum szerkezeti modelljét (`DocumentDefinition`). A page-schema modul arra ad bővítési pontot, hogy ezt a modellt egy külön lapleíró állományból származó megjelenítési vagy elrendezési információkkal később ki lehessen egészíteni.

**Fontos:** a modul jelenleg nem tartalmaz tényleges lapleíró-séma parsert. Az alapértelmezett implementáció szándékosan nem módosítja a dokumentumdefiníciót. A modul elsősorban jól elkülönített kiterjesztési pontot biztosít a feldolgozási pipeline számára.

## A modul fő felelősségei

A modul jelenlegi feladatai:

- egységes szolgáltatási interfészt adni a page-schema feldolgozáshoz;
- elkülöníteni a lapleíró séma kezelését az XSD- és UIModel-feldolgozástól;
- no-op implementációt biztosítani olyan futási módhoz, ahol nincs aktív page-schema feldolgozás;
- lehetővé tenni, hogy később valódi parser implementáció illeszthető legyen a processing pipeline-ba annak átszervezése nélkül.

## Belépési pont

A modul programozói belépési pontja:

```java
hu.gov.nav.xsdparsertool.pageschema.service.PageSchemaParserService
```

A szolgáltatás egyetlen művelete:

```java
void applyPageSchema(DocumentDefinition definition, Path pageSchemaFile)
```

A metódus egy már létrehozott `DocumentDefinition` objektumot és a hozzá tartozó lapleíró sémafájl útvonalát kapja meg.

## Alapértelmezett implementáció

Az alapértelmezett implementáció:

```java
hu.gov.nav.xsdparsertool.pageschema.service.NoOpPageSchemaParserService
```

Ez egy **no-op**, vagyis módosítást nem végző implementáció. Meghívható, de:

- nem olvassa be a `.schema` fájlt;
- nem módosítja a `DocumentDefinition` objektumot;
- nem állít elő külön eredményt.

Ez tudatos viselkedés: a processing modul így egységesen kezelheti az opcionális page-schema lépést akkor is, ha még nincs aktív parser.

## Hogyan hívható?

A szolgáltatás közvetlen Java kódból a `PageSchemaParserService` interfészen keresztül hívható.

Példa a jelenlegi no-op implementáció használatára:

```java
PageSchemaParserService pageSchemaParser = new NoOpPageSchemaParserService();
pageSchemaParser.applyPageSchema(documentDefinition, pageSchemaFile);
```

A hívás után a `documentDefinition` változatlan marad.

Normál alkalmazásműködésben a modult nem közvetlenül a frontend vagy REST kliens hívja. A `nav-xsd-parser-tool-processing` modul használja a szolgáltatást az XML feldolgozási folyamat részeként.

## Helye az XML-feldolgozási folyamatban

A `DefaultXmlProcessingService` feldolgozási sorrendje a releváns részen:

1. az XML alapadatainak felismerése;
2. a megfelelő `SchemaBundle` feloldása;
3. az XSD feldolgozása és a `DocumentDefinition` létrehozása;
4. opcionálisan a UIModel alkalmazása;
5. ha a `SchemaBundle` tartalmaz page-schema fájlt, a `PageSchemaParserService.applyPageSchema(...)` meghívása.

Egyszerűsítve:

```text
XML
  ↓
Schema Registry
  ↓
XSD parser
  ↓
DocumentDefinition
  ↓
UIModel (opcionális)
  ↓
Page schema (opcionális)
```

A `nav-xsd-parser-tool-schema-registry` modul keresi meg a kapcsolódó `.schema` állományt, és annak elérési útját a `SchemaBundle.pageSchemaFile` mezőben adja tovább.

## A modul szolgáltatásai

### `PageSchemaParserService.applyPageSchema(...)`

A page-schema feldolgozás egységes szerződése. A bemenete:

- `DocumentDefinition` – a már felépített dokumentummodell;
- `Path pageSchemaFile` – a lapleíró sémafájl elérési útja.

A metódus nem ad vissza külön objektumot. Egy tényleges parser implementáció a kapott `DocumentDefinition` objektumot egészítheti ki.

### `NoOpPageSchemaParserService`

Biztonságos alapértelmezett implementáció, amely semmilyen változtatást nem hajt végre. Akkor használható, ha a page-schema feldolgozás opcionális vagy nincs implementálva.

## Kapcsolódó modulok

### `nav-xsd-parser-tool-core`

A page-schema modul innen használja a `DocumentDefinition` modellt.

### `nav-xsd-parser-tool-schema-registry`

A schema registry keresi meg az XML/XSD csomaghoz tartozó `.schema` fájlt, és a `SchemaBundle` részeként adja tovább annak útvonalát.

### `nav-xsd-parser-tool-processing`

Ez a modul hívja meg a `PageSchemaParserService` szolgáltatást a dokumentum feldolgozása közben.

## Függőségek

A modul közvetlen projektfüggősége:

```text
nav-xsd-parser-tool-core
```

A modul önmagában nem Spring Boot alkalmazás, nincs saját REST végpontja és nincs saját futtatható belépési osztálya.

## Bővítés

Valódi lapleíró-séma támogatás bevezetésekor célszerű új `PageSchemaParserService` implementációt készíteni, amely:

1. beolvassa a kapott `.schema` fájlt;
2. értelmezi az abban tárolt metaadatokat;
3. ezeket a teljes dokumentumkontextus alapján a megfelelő definíciós elemekhez rendeli;
4. a meglévő `DocumentDefinition` struktúrát egészíti ki.

A `PageSchemaParserService` szerződés miatt ehhez a processing modul feldolgozási sorrendjét nem szükséges megváltoztatni.
