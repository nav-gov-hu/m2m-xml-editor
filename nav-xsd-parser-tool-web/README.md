# nav-xsd-parser-tool-web

A `nav-xsd-parser-tool-web` az M2M XML EDITOR Spring Boot webalkalmazási modulja. Ez a modul adja az alkalmazás futtatható webes belépési pontját, a REST API-kat, a Spring Security integrációt, az adatbázis-hozzáférést, az XML-állománykezelést és a böngészőben futó vanilla JavaScript/ES module frontend nagy részét.

## Felelősség

A modul a felhasználói és HTTP-réteget kapcsolja össze az alacsonyabb szintű Maven modulok szolgáltatásaival. Nem célja az XSD-, UIModel-, nyomtatási vagy NAV M2M protokollspecifikus üzleti logika megkettőzése; ezeket a megfelelő `xsd`, `uimodel`, `processing`, `print`, `github-updater` és `m2m-submitter` modulok biztosítják.

A legfontosabb felelősségi területek:

- Spring Boot alkalmazásindítás és desktop-integráció;
- REST API és DTO réteg;
- XML állományok regisztrálása, megnyitása, mentése, verziózása, zárolása és archiválása;
- normál és nagy/multiform XML feldolgozási folyamatok;
- XSD- és XPath-validáció webes orchestrációja;
- felhasználó-, szerepkör-, partner- és partner-hozzáférés kezelés;
- rendszerkonfiguráció, setup, adatbázis- és hálózati diagnosztika;
- tanúsítvány-, titok- és proxykezelés;
- audit- és konzolnapló webes elérése;
- GitHub űrlapsablon-katalógus webes integrációja;
- statikus HTML/CSS/JavaScript frontend.

## Belépési pont

A futtatható Java belépési pont:

```text
hu.gov.nav.xsdparsertool.web.NavXsdParserToolWebApplication
```

A `main(String[] args)` Spring Boot alkalmazást indít, beköti a startup splash eseményeket, majd desktop módban inicializálja a desktop launcher integrációt.

A modul Spring komponenskeresése a `hu.gov.nav.xsdparsertool` és `hu.nav.m2m.submitter` csomagokra terjed ki. Ugyanez a két gyökér szerepel az entity- és JPA repository-szkennelésben is.

## Fő Java rétegek

A fő csomagok:

| Csomag | Szerep |
|---|---|
| `api` | általános editor, print, schema-registry, health és admin REST API |
| `xmlfile` | XML állománykezelés, lock/session, revízió, mentés, nagy XML és multiform |
| `xmlindex` | űrlapsablon- és form-part szintű XML indexkonfiguráció |
| `xpath` | XPath/XSLT validáció és futási állapot |
| `xsdvalidation` | streaming XSD-validáció és hibatárolás |
| `security` | autentikáció, felhasználók, szerepkörök és partner-hozzáférés |
| `partner` | partnernyilvántartás |
| `systemconfig` | futásidejű rendszerkonfiguráció |
| `setup` | első indítási konfiguráció és újraindítás |
| `certificate` | tanúsítványkezelés és hálózati teszt |
| `secret` | rendszer-titkok és master key kezelés |
| `audit` | auditnapló |
| `consolelog` | futási napló megtekintése és streamelése |
| `processing` | háttérfeldolgozási jobok |
| `githubproxy` / `network` | GitHub- és NAV-proxy beállítások összekötése a rendszerkonfigurációval |
| `database` | adatbázis-konfiguráció és állapotdiagnosztika |

## Fontos REST API-csoportok

A modul többek között az alábbi REST gyökérútvonalakat szolgáltatja:

```text
/api
/api/admin
/api/admin/audit-log
/api/admin/certificates
/api/admin/configuration
/api/admin/console-log
/api/admin/network
/api/jobs
/api/partners
/api/print
/api/schema-registry
/api/security
/api/setup
/api/users
/api/users/{userId}/partner-access
/api/xml-files
/api/xml-files/{xmlFileId}/large-multiform
/api/xml-index-config
/api/xpath-validator
/api/xsd-validation
```

A konkrét request/response szerződéshez a controller és DTO Javadoc az elsődleges forrás.

## XML állománykezelés

Az XML-kezelés fő szolgáltatásai a `xmlfile/service` csomagban találhatók. Ezek együtt kezelik az állomány-életciklust, a felhasználói sessiont, a szerkesztési lockot, a mentést, a revíziókat és a nagy XML-es speciális folyamatokat.

Fontos invariánsok:

- a módosíthatóságot backend oldalon is ellenőrizni kell;
- `SUBMITTED_OK` állapotú XML végállapotú és nem módosítható;
- a frontend read-only állapota nem helyettesíti a szerveroldali védelmet;
- multiform dokumentumban a mezőket és hibákat teljes, indexelt XML-útvonal alapján kell kötni;
- XSD-hibás, de well-formed XML szerkeszthető maradhat;
- mentéskor a projekt által elfogadott pretty-print formátumot kell megőrizni.

## Frontend architektúra

A frontend vanilla JavaScript ES modulokra bontva működik a következő könyvtárban:

```text
src/main/resources/static/js
```

A fő modulcsoportok:

| Könyvtár | Felelősség |
|---|---|
| `core` | API-kliens, alkalmazásállapot, közös UI shell és téma |
| `runtime` | alkalmazás-runtime és modulok közötti bridge/context |
| `form` | űrlaprenderelés, navigáció, toolbar, shortcut, multiform |
| `xml` | XML szerkesztés, snapshot, session és mentési workflow |
| `validation` | XSD/XPath validáció, drawer és hibanavigáció |
| `m2m` | M2M státusz, csatolmány, progress, kommunikációs trace és beküldési UI |
| `pages` | oldal-specifikus admin és szerkesztő logika |
| `schema` | schema-registry felület |
| `security` | biztonsági fejléc és felhasználói UI-kiegészítések |
| `print` | nyomtatási műveletek |
| `admin`, `home` | admin- és kezdőoldali működés |

A JavaScript fájlok modul-szintű JSDocot, a névvel rendelkező függvények és nem triviális helper függvények pedig JSDoc leírást tartalmaznak. A JSDoc célja a kliensoldali üzleti és állapotkezelési szabályok forrásközeli dokumentálása; a Javadoc és a JSDoc két külön dokumentációs rendszer.

## Frontend invariánsok

A frontend fejlesztésnél különösen fontos:

- multiform esetén nincs globális `fieldId` alapú fallback;
- az érték-, címke- és validációshiba-kötés teljes indexelt XML-útvonalra támaszkodik;
- a `SUBMITTED_OK` kliensoldali tiltás csak UX-réteg, a backend kontroll kötelező;
- natív `alert()`, `confirm()` és `prompt()` nem használható;
- a produkciós téma neve `nav-application-theme`;
- az XSD/XPath drawerek overlay-ként működnek;
- a főlap/melléklap kontextust minden multiform műveletnél meg kell őrizni.

## Statikus oldalak

A `src/main/resources/static` könyvtár többek között az alábbi oldalakat tartalmazza:

```text
index.html
form.html
validate.html
xml-files.html
xml-index-config.html
xpath-validator.html
m2m-submitter.html
github-templates.html
partners.html
partner-edit.html
users.html
user-edit.html
partner-access.html
configuration.html
setup.html
admin.html
audit-log.html
console-log.html
login.html
```

## Kapcsolódó Maven modulok

A web modul közvetlenül használja többek között:

- `nav-xsd-parser-tool-core`
- `nav-xsd-parser-tool-processing`
- `nav-xsd-parser-tool-print`
- `nav-xsd-parser-tool-uimodel`
- `nav-xsd-parser-tool-github-updater`
- `nav-xsd-parser-tool-m2m-submitter`

A web modulban a controller réteg lehetőleg kérésvalidációt, DTO-mappinget és HTTP-válaszkezelést végezzen; az összetettebb üzleti folyamat szolgáltatásrétegben vagy a megfelelő alacsonyabb Maven modulban maradjon.

