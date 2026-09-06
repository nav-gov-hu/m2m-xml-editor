# nav-xsd-parser-tool-xpath-cli

## Mire való ez a modul?

A `nav-xsd-parser-tool-xpath-cli` egy önálló parancssori segédprogram XSL/XSLT alapú XML-validáció futtatására. Akkor használható, amikor a validációt a webalkalmazás nélkül, közvetlenül parancssorból kell elindítani, például fejlesztői ellenőrzéshez, diagnosztikához vagy automatizált futtatáshoz.

A modul a Saxon-HE XSLT motorját használja. A megadott XSL/XSLT állományt lefuttatja az XML bemeneten, majd a transzformáció eredményéből megkeresi a `Hiba` vagy `hiba` elemeket és ezekből állítja elő a validációs hibalistát.

## Belépési pont

A futtatható alkalmazás fő belépési pontja:

```text
hu.gov.nav.xsdparsertool.xpathcli.NavXsdParserToolXPathCliApplication
```

A Maven build a `maven-shade-plugin` segítségével futtatható, függőségeket is tartalmazó JAR-t készít, amelynek manifestje ezt az osztályt adja meg főosztályként.

A gyökérparancs neve:

```text
nav-xsd-parser-tool-xpath-cli
```

A jelenleg elérhető fő alparancs:

```text
validate-xslt
```

## A modul fő szolgáltatásai

### `ValidateXsltCommand`

A `validate-xslt` parancs Picocli implementációja. Feladata:

- az XSL/XSLT és XML bemeneti fájl ellenőrzése;
- a validációhoz szükséges XSLT paraméterek összeállítása;
- az `XsltValidationService` meghívása;
- a nyers transzformációs eredmény opcionális fájlba írása;
- a validációs hibaüzenetek konzolos megjelenítése;
- a parancs kimenetelének megfelelő kilépési kód visszaadása.

### `XsltValidationService`

A tényleges XSLT feldolgozást végző szolgáltatás. Nem Spring bean, a parancs közvetlenül példányosítja.

A szolgáltatás:

1. létrehozza a Saxon processzort;
2. biztonságosan korlátozza az XSLT által elérhető külső erőforrásokat;
3. lefordítja a stylesheetet;
4. beállítja a validációs paramétereket;
5. lefuttatja a transzformációt;
6. XML szövegként megőrzi a teljes eredményt;
7. kinyeri a `Hiba` / `hiba` elemekből a hibaüzeneteket.

### `XsltValidationResult`

A validáció eredményét hordozó record. Két adatot tartalmaz:

- `rawOutputXml` – a teljes XSLT eredmény-XML;
- `errorMessages` – a feldolgozott validációs hibaüzenetek.

A `hasErrors()` metódussal egyszerűen eldönthető, hogy található-e legalább egy validációs hiba.

## Hogyan hívható parancssorból?

Build után a futtatható JAR a modul `target` könyvtárában jön létre.

A használati súgó például:

```bash
java -jar nav-xsd-parser-tool-xpath-cli/target/nav-xsd-parser-tool-xpath-cli.jar --help
```

XSLT-validáció példa:

```bash
java -jar nav-xsd-parser-tool-xpath-cli/target/nav-xsd-parser-tool-xpath-cli.jar validate-xslt \
  --xsl /path/to/validator.xsl \
  --xml /path/to/document.xml \
  --form-name PMT25 \
  --form-version 1.0.0 \
  --print-errors
```

A legfontosabb paraméterek:

| Paraméter | Jelentés |
|---|---|
| `--xsl` | A futtatandó XSL/XSLT fájl. Kötelező. |
| `--xml` | A validálandó XML fájl. Kötelező. |
| `--form-name` | Az XSLT `form-name` paramétere. Kötelező. |
| `--form-version` | Az XSLT `form-version` paramétere. Kötelező. |
| `--rules-root` | Az XSLT `rules-root` paramétere. Ha nincs megadva, az XML szülőkönyvtára lesz. |
| `--rules-dir` | Az XSLT `rules-dir` paramétere. |
| `--rules-file` | Az XSLT `rules-file` paramétere. Ha nincs megadva, az XML abszolút útvonala lesz. |
| `--result-file` | Opcionális fájl, ahová a teljes XSLT eredmény-XML menthető. |
| `--encoding` | A kimeneti XML karakterkódolása, alapértelmezésben UTF-8. |
| `--print-errors` | Ha igaz, a kinyert hibaüzenetek is megjelennek a konzolon. |

## Kilépési kódok

A `validate-xslt` parancs az alábbi kódokat használja:

- `0` – a transzformáció sikeresen lefutott és nem található validációs hiba;
- `2` – a transzformáció sikeresen lefutott, de legalább egy validációs hiba található;
- `1` – technikai XSLT- vagy fájlkezelési hiba történt.

Ez lehetővé teszi, hogy shell script vagy CI folyamat különbséget tegyen a technikai hiba és a tartalmi validációs hiba között.

## Programozói használat

A modul szolgáltatása közvetlenül Java kódból is meghívható:

```java
XsltValidationService service = new XsltValidationService();

XsltValidationResult result = service.validate(
        xslPath,
        xmlPath,
        rulesRoot,
        rulesDir,
        formName,
        formVersion,
        rulesFile,
        StandardCharsets.UTF_8
);

if (result.hasErrors()) {
    result.errorMessages().forEach(System.out::println);
}
```

A szolgáltatás nem igényel Spring alkalmazáskontextust.

## XSLT paraméterek

A szolgáltatás a következő stylesheet-paramétereket adhatja át:

```text
rules-root
rules-dir
form-name
form-version
rules-file
```

A `rules-root` és `rules-file` értékeknél a Windows `\\` könyvtárelválasztó `/` karakterre normalizálódik. `null`, üres vagy csak whitespace érték nem kerül be a Saxon paramétertérképbe.

## Külső XSLT erőforrások biztonsági korlátozása

A stylesheet által hivatkozott további XSLT erőforrásokat a modul korlátozott `URIResolver` segítségével oldja fel.

A tényleges forráskód szerint:

- csak `file:` URI-séma engedélyezett;
- a feloldott fájlnak a fő XSL fájl könyvtárán belül kell maradnia;
- hálózati vagy más URI-séma nem engedélyezett;
- a könyvtárból kifelé mutató normalizált útvonal elutasításra kerül.

Ez a korlátozás az XSLT `include`/`import` jellegű külső feloldására vonatkozik.

## Hibaüzenetek felismerése

A transzformáció eredményében a szolgáltatás az alábbi fallback sorrendben keres hibaelemeket:

1. namespace-független `Hiba`;
2. egyszerű `Hiba`;
3. namespace-független `hiba`;
4. egyszerű `hiba`.

Egy megtalált hibaelem szövege az alábbi prioritással készül:

1. `hibaszoveg` attribútum;
2. `message` attribútum;
3. az elem teljes szöveges tartalma.

Az eredmény-XML feldolgozásához használt parser tiltja a DOCTYPE és a külső entitások feldolgozását.

## Kapcsolódó modulok

A modul közvetlen projektfüggősége:

- `nav-xsd-parser-tool-core` – biztonságos és kivételvédett fájlműveleti segédek.

Külső fő függőségei:

- Picocli – parancssori interfész;
- Saxon-HE – XSLT végrehajtás;
- SLF4J + Logback – naplózás.

A `nav-xsd-parser-tool-xpath-cli` önálló diagnosztikai/parancssori eszköz. Nem a webes XPath-validáció REST rétegének implementációja, bár hasonló XSL/XPath alapú ellenőrzési feladatra szolgál.

## Felelősségi határ

A modul feladata a parancssori XSLT-validáció futtatása és az eredmény feldolgozása. Nem feladata:

- a webes felület kiszolgálása;
- az XML állományok adatbázisban történő nyilvántartása;
- XSD sémafeloldás;
- űrlapnézet építése;
- XML szerkesztés vagy mentés;
- NAV M2M beküldés.
