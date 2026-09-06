# nav-xsd-parser-tool-xsd

## A modul célja

A `nav-xsd-parser-tool-xsd` modul feladata, hogy a Schema Registry által kiválasztott XSD-séma-csomagból az alkalmazás által közvetlenül használható dokumentum- és meződefiníciót készítsen. Ez a réteg fordítja le az XSD technikai szerkezetét a `DocumentDefinition`, `BlockDefinition` és `FieldDefinition` modellekre.

A modul nem XML példányokat olvas és nem validál. A konkrét XML-adatok beolvasása és az XSD-validáció a feldolgozási réteg más szolgáltatásainak feladata.

## Belépési pont

A modul programozói belépési pontja a következő interfész:

```java
XsdParserService
```

A fő implementáció:

```java
BasicXsdParserService
```

Egy egyszerűsített, tényleges XSD-bejárást nem végző implementáció is rendelkezésre áll:

```java
StubXsdParserService
```

## Fő szolgáltatás

Az `XsdParserService` egyetlen szolgáltatási művelete:

```java
DocumentDefinition parse(SchemaBundle bundle)
```

A bemenet egy `SchemaBundle`, amelyet jellemzően a `nav-xsd-parser-tool-schema-registry` modul állít elő. Tartalmazhatja többek között az elsődleges XSD-t, a kapcsolódó XSD-fájlokat, a dokumentumtípust, a gyökérelem nevét és a target namespace-t.

A kimenet egy `DocumentDefinition`, amelyet a későbbi form- és XML-feldolgozási lépések használnak.

## Használat

Közvetlen Java-hívás például:

```java
XsdParserService parser = new BasicXsdParserService();
DocumentDefinition definition = parser.parse(schemaBundle);
```

A modul osztályai önmagukban nem Spring komponensek, ezért a példányosításról vagy bean-regisztrációról a hívó modul gondoskodik.

## Mit dolgoz fel a BasicXsdParserService?

A fő feldolgozási lépések:

1. Az elsődleges és kapcsolódó XSD-fájlokból közös index készül.
2. Az index globális `element`, `complexType` és `simpleType` definíciókat tartalmaz.
3. A parser megkeresi a dokumentum gyökérelemét.
4. A gyökér complex type szerkezetét rekurzívan bejárja.
5. A `FieldGroup_` elemekből blokkok, a mezőelemekből `FieldDefinition` objektumok készülnek.
6. Az XSD annotációkból címkék kerülnek feloldásra.
7. A simple type restriction alapján adattípus és enumértékek kerülnek a meződefinícióba.
8. A `minOccurs` és `maxOccurs` alapján a mező kötelezőségi és ismétlődési adatai is bekerülnek.

## Címkefeloldás

A mezőcímkék feloldása több forrást vizsgál. A fő prioritás:

1. az elem saját `annotation/documentation` értéke;
2. `ref` hivatkozás esetén a globális elem dokumentációja;
3. hivatkozott `simpleType` dokumentációja;
4. hivatkozott `complexType` dokumentációja;
5. inline `simpleType`;
6. inline `complexType`;
7. végül a technikai XML-névből képzett fallback címke.

A parser a címke végén redundánsan megjelenő technikai mezőnevet eltávolítja.

## Adattípusok és enumok

A parser a névvel hivatkozott vagy inline `simpleType/restriction` definícióból olvassa ki az alapadattípust. Ha a restriction `enumeration` elemeket tartalmaz, azok értékei bekerülnek a `FieldDefinition.enumValues` listába.

Ha nem oldható fel konkrét típus, a fallback adattípus `string`.

## Biztonságos XSD-beolvasás

Az XSD-fájlok DOM parserrel kerülnek feldolgozásra. A parser tiltja a külső entitásokat, a DOCTYPE használatát, a külső DTD- és sémahozzáférést, valamint az XInclude feldolgozást. Ez azért fontos, mert a sémafájl feldolgozása nem nyithat tetszőleges külső erőforrást.

## Kapcsolat más modulokkal

- `nav-xsd-parser-tool-core` – a `SchemaBundle`, `DocumentDefinition`, `BlockDefinition` és `FieldDefinition` modellek forrása.
- `nav-xsd-parser-tool-schema-registry` – kiválasztja és összeállítja a feldolgozandó séma-csomagot.
- `nav-xsd-parser-tool-processing` – az XSD parser eredményét felhasználva építi fel a konkrét dokumentum feldolgozási eredményeit és űrlapadatait.
- `nav-xsd-parser-tool-uimodel` – az XSD-ből létrejött definíciót UIModel-metaadatokkal gazdagíthatja.

## Fő osztályok

| Osztály | Szerep |
|---|---|
| `XsdParserService` | A modul szolgáltatási szerződése. |
| `BasicXsdParserService` | Az XSD-k tényleges bejárása és a dokumentumdefiníció felépítése. |
| `StubXsdParserService` | Minimális, XSD-struktúrát nem feldolgozó implementáció. |

## Felelősségi határ

A modul feladata az XSD szerkezet értelmezése és alkalmazásszintű modellre fordítása. Nem feladata a konkrét XML példány értékeinek betöltése, az XML módosítása, az XSD-validáció végrehajtása, a UI megjelenítése vagy a sémák repository-ból történő kiválasztása.
