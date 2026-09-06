# nav-xsd-parser-tool-core

## Mi ez a modul?

A `nav-xsd-parser-tool-core` a M2M XML EDITOR közös, technológiafüggetlen alapmodulja. Itt találhatók azok a domain modellek, enumok és biztonsági segédosztályok, amelyeket több más Maven modul is használ.

A modul **nem önálló alkalmazás**, nincs saját Spring Boot indítási pontja és nincs saját REST API-ja. A többi modul Java-függőségként használja.

## Fő felelősségek

A modul négy fő területet fog össze:

1. **Dokumentum- és séma-modellek** – a feloldott XSD-csomag, a dokumentumdefiníció és a konkrét XML-példány közös reprezentációi.
2. **Űrlapmodellek** – a tab/szekció/sor/mező felépítés, valamint a konkrét mezőértékek és ismétlődő sorpéldányok modelljei.
3. **Eredmény- és validációs modellek** – feldolgozási, export- és validációs eredmények, illetve strukturált hibák.
4. **Közös biztonsági segédek** – biztonságos XML parser beállítások, tulajdonosra korlátozott fájlműveletek és központi jogosultsági kifejezések.

## Mi a modul belépési pontja?

A modulnak nincs egyetlen futtatható belépési pontja. A hívó modul a feladatának megfelelő típust használja.

A legfontosabb programozói belépési pontok:

- `SchemaBundle` – a Schema Registry által feloldott séma-erőforrások átadására;
- `DocumentDefinition` és `FieldDefinition` – az XSD-ből felépített dokumentumszerkezethez;
- `FormDefinition` és `FormData` – az űrlapstruktúra és a konkrét XML-értékek kezeléséhez;
- `ProcessingResult`, `ValidationResult`, `ValidationIssue` – feldolgozási és validációs eredményekhez;
- `XmlDocumentView` és `XmlNodeView` – az XML-fa nézethez;
- `SecureXmlParserSupport` – DOM, StAX és transformer factory biztonságos konfigurálásához;
- `SecureFileOperations` – érzékeny fájlok és könyvtárak biztonságos létrehozásához és írásához;
- `AuthorizationRules` – egységes Spring Security metódus-jogosultsági szabályokhoz.

## Hogyan használják a többi modulok?

### Sémacsomag átadása

A `schema-registry` modul egy `SchemaBundle` objektumban adja tovább a feloldott erőforrásokat:

```java
SchemaBundle bundle = new SchemaBundle();
bundle.setDocumentType("26HIPAK");
bundle.setPrimaryXsd(primaryXsd);
bundle.setXsdFiles(xsdFiles);
bundle.setUiModelFile(uiModelFile);
```

A `processing` modul ezután ebből indul ki az XSD, UIModel és XML feldolgozásakor.

### Űrlapdefiníció és adatok

A megjelenítési szerkezetet a `FormDefinition`, a konkrét XML-értékeket pedig külön `FormData` objektum hordozza:

```java
FormDefinition definition = new FormDefinition();
definition.setId("26HIPAK");
definition.setTitle("Helyi iparűzési adó");

FormData data = new FormData();
data.getValuesByFieldId().put("Field_Example", formValue);
```

Ez a szétválasztás teszi lehetővé, hogy ugyanazt az űrlapdefiníciót több XML-példányhoz is fel lehessen használni.

### Validációs eredmény

```java
ValidationIssue issue = new ValidationIssue();
issue.setCode("XSD_VALIDATION_ERROR");
issue.setPath("/Doc[1]/Form[1]/Field[1]");
issue.setMessage("A mező értéke nem felel meg az XSD-nek.");
issue.setSeverity(Severity.ERROR);
```

A teljes XML-útvonal megőrzése különösen fontos multiform dokumentumoknál, ahol azonos technikai mezőnév több részbizonylatban is előfordulhat.

## Biztonságos XML-feldolgozás

A DOM parser factory-kat a központi segédosztállyal kell konfigurálni:

```java
DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
SecureXmlParserSupport.configureSecureDocumentBuilderFactory(factory);
```

StAX esetén:

```java
XMLInputFactory factory = XMLInputFactory.newFactory();
SecureXmlParserSupport.configureSecureXmlInputFactory(factory);
```

Transformer esetén:

```java
TransformerFactory factory = TransformerFactory.newInstance();
SecureXmlParserSupport.configureSecureTransformerFactory(factory);
```

A konfiguráció tiltja a külső DTD-ket, külső entitásokat és más külső XML-erőforrások automatikus betöltését.

## Biztonságos fájlműveletek

Érzékeny alkalmazásfájl létrehozásakor a `SecureFileOperations` használható:

```java
SecureFileOperations.createPrivateDirectories(targetDirectory);
SecureFileOperations.writePrivateString(
        targetFile,
        content,
        StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING);
```

A segéd POSIX fájlrendszeren owner-only jogosultságot használ. Más platformokon ACL-alapú, végső esetben `java.io.File` alapú best-effort korlátozást alkalmaz.

## Jogosultsági szabályok

A `AuthorizationRules` konstansai a web és szolgáltatási réteg `@PreAuthorize` annotációiban használhatók:

```java
@PreAuthorize(AuthorizationRules.OPERATOR_WRITE)
public void updateSomething() {
    // ...
}
```

A központi konstansok használata csökkenti annak kockázatát, hogy ugyanaz a jogosultsági szabály több helyen eltérően legyen megadva.

## Fő modellcsoportok

| Csomag | Szerep |
|---|---|
| `core.model.bundle` | Feloldott XSD/UIModel/page-schema csomag |
| `core.model.definition` | XSD-ből felépített dokumentum- és meződefiníció |
| `core.model.form` | Űrlapdefiníció és konkrét űrlapadatok |
| `core.model.instance` | Konkrét XML-példány strukturális fája |
| `core.model.processing` | Feldolgozási, validációs és exporteredmények |
| `core.model.validation` | Strukturált validációs problémák |
| `core.model.xmlview` | XML-fa megjelenítési modell |
| `core.enums` | Közös állapot- és súlyossági enumok |
| `core.security` | Közös jogosultsági kifejezések |
| `core.support` | Biztonságos fájl- és platformműveletek |
| `core.xml` | Biztonságos XML parser konfiguráció |

## Kapcsolódás a többi modulhoz

A `core` modul szándékosan nem tartalmaz webes DTO-kat, controllereket vagy Spring-specifikus üzleti logikát. A fő kapcsolatok:

- `schema-registry` → `SchemaBundle`;
- `xsd` → `DocumentDefinition`, `BlockDefinition`, `FieldDefinition`;
- `uimodel` és `processing` → űrlapmodellek és dokumentumdefiníciók;
- `print` → `FormDefinition` és `FormData`;
- `web` → feldolgozási modellek, jogosultsági szabályok és biztonsági segédek;
- `m2m-submitter` és más integrációs modulok → közös eredmény- és fájlkezelési típusok, ahol szükséges.

## Függőségek

A modul minimális külső függőséggel rendelkezik. A `pom.xml` alapján közvetlenül az `slf4j-api` függőséget deklarálja; a domain modellek önmagukban nem kötődnek Springhez vagy webes technológiához.

## Fejlesztési szabály

Új közös modell akkor kerüljön ebbe a modulba, ha azt több backend modul is használja, és nincs web-, adatbázis- vagy integrációspecifikus felelőssége. Webes request/response DTO, controller vagy konkrét infrastruktúra-implementáció ne kerüljön a `core` modulba.
