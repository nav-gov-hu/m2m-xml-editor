# nav-xsd-parser-tool-m2m-submitter

## A modul célja

A `nav-xsd-parser-tool-m2m-submitter` modul választja el a NAV M2M kommunikációt az XML-szerkesztő általános feldolgozási funkcióitól. A modul kezeli egy XML beküldési életciklusát, a csatolmányok feltöltését és NAV `fileId` visszaírását, a Bizonylat API hívásokat, az online validációt és kalkulációt, a státuszfrissítést/pollingot, valamint a kommunikációs eseménynaplót.

A modul Spring komponenseket és REST vezérlőket tartalmaz, de nincs saját `main` metódusa: a teljes alkalmazás részeként töltődik be.

## Fő belépési pontok

A központi üzleti szolgáltatás:

```text
hu.nav.m2m.submitter.service.SubmissionService
```

A NAV kommunikáció absztrakciója:

```text
hu.nav.m2m.submitter.service.nav.NavGateway
```

Valós NAV kapcsolat esetén a fő implementáció:

```text
hu.nav.m2m.submitter.service.nav.RealNavGateway
```

Fejlesztési/teszt módban a `MockNavGateway` használható.

## Fő szolgáltatások

### SubmissionService

A teljes beküldési folyamat orchestrátora. Felelős többek között:

- beküldési munkamenet létrehozásáért és lekérdezéséért;
- beküldésre jelölésért és annak visszavonásáért;
- XML-tartalom cseréjéért a megengedett életciklusállapotokban;
- csatolmányok lokális tárolásáért és NAV filestore feltöltéséért;
- az XML-ben deklarált csatolmányok és a feltöltött fájlok összerendeléséért;
- a NAV `fileId` értékek megfelelő XML csomópontba történő visszaírásáért;
- Bizonylat API beküldésért;
- online validáció és kalkuláció indításáért és eredményfeldolgozásáért;
- NAV státuszfrissítésért és időzített pollingért;
- kommunikációs és életciklus-események naplózásáért.

A `SUBMITTED_OK` állapot végleges. A szolgáltatási réteg ilyen beküldésnél blokkolja a módosító műveleteket, beleértve az újraküldést és a csatolmány-módosítást.

### NavGateway

A NAV transport réteg technológiafüggetlen szerződése. Fő műveletei:

```java
UploadedFile uploadFile(...);
BizonylatCreateResult createBizonylat(...);
ValidacioResult createValidacio(...);
ValidacioResult getValidacio(...);
KalkulacioResult createKalkulacio(...);
KalkulacioResult getKalkulacio(...);
StatusResult getStatus(...);
```

A magasabb üzleti réteg így nem függ közvetlenül a konkrét HTTP implementációtól.

### M2mSignatureService és RuntimeSignatureKeyService

A `M2mSignatureService` a NAV által elvárt időbélyeges SHA-256 alapú kérésaláírást állítja elő. A `RuntimeSignatureKeyService` a nonce beváltásakor kapott futásidejű kulcsrész életciklusát kezeli. A runtime kulcsrész elsőbbséget élvez a konfigurált fallbackhez képest.

### XmlAttachmentReferenceExtractor / XmlAttachmentReferenceInjector

Az extractor az XML-ből kinyeri a csatolmányhivatkozásokat. Az injector a NAV feltöltés után kapott `fileId` értéket a megfelelő csatolmánycsomópontba írja vissza. Több csatolmány esetén a párosítás fájlnév és konkrét XML-struktúra alapján történik; már meglévő `fileId` nem írható felül.

### FileStorageService és ManagedStoragePathPolicy

A `FileStorageService` a modul által kezelt fájlokat kontrollált könyvtárstruktúrába menti, és SHA-256 hash-t számít. A `ManagedStoragePathPolicy` olvasáskor kanonizálja az útvonalakat és megakadályozza, hogy egy kérés a konfigurált storage gyökéren kívüli fájlhoz férjen hozzá.

### Proxy/TLS szolgáltatások

A `ProxySettingsService`, `NavProxySettingsProvider`, `NavRestTemplateFactory` és `ProxyConnectionTestService` kezeli a NAV HTTP kapcsolat proxy- és TLS/truststore-beállításait. A kapcsolatpróba ideiglenes beállításokkal is futtatható anélkül, hogy azokat perzisztálná.

## REST API

A fő REST gyökér:

```text
/api/submissions
```

Fontosabb végpontok:

| Művelet | Végpont |
|---|---|
| Beküldés létrehozása | `POST /api/submissions` |
| Lista | `GET /api/submissions` |
| Egy beküldés | `GET /api/submissions/{id}` |
| Csatolmány hozzáadás | `POST /api/submissions/{id}/attachments` |
| Beküldésre jelölés | `POST /api/submissions/{id}/mark-for-submit` |
| Jelölés visszavonása | `POST /api/submissions/{id}/withdraw-submit-mark` |
| Beküldés | `POST /api/submissions/{id}/submit` |
| Csatolmányok NAV feltöltése | `POST /api/submissions/{id}/step/upload-attachments` |
| Bizonylat létrehozás | `POST /api/submissions/{id}/step/create-bizonylat` |
| Online validáció | `POST /api/submissions/{id}/validation/online` |
| Validációs állapot | `GET /api/submissions/{id}/validation/status` |
| Validációs hibák | `GET /api/submissions/{id}/validation/errors` |
| Online kalkuláció | `POST /api/submissions/{id}/calculation/online` |
| Kalkulációs eredmény | `GET /api/submissions/{id}/calculation/result` |
| NAV státusz frissítése | `POST /api/submissions/{id}/refresh` |
| Eseménynapló | `GET /api/submissions/{id}/events` |

További REST csoportok:

```text
/api/m2m
/api/nav-token
/api/nav-registration
/api/proxy-settings
/api/test-tool
```

## Tipikus beküldési folyamat

```text
XML
  -> SubmissionService
  -> XML metaadat és csatolmányhivatkozások feloldása
  -> lokális csatolmányok ellenőrzése
  -> NAV filestore feltöltés
  -> NAV fileId visszaírás az XML-be
  -> Bizonylat payload előkészítés
  -> M2mSignatureService
  -> RealNavGateway / NavGateway
  -> NAV Bizonylat API
  -> státusz + eseménynapló
  -> szükség esetén polling
```

Az online validáció és kalkuláció ugyanerre a Bizonylat API route/metaadat-feloldásra épül.

## Konfiguráció

A modul fő konfigurációs prefixe:

```text
nav.m2m
```

A `NavM2mProperties` többek között a következő területeket tartalmazza:

- `mockMode`;
- menedzselt storage könyvtár;
- NAV common és Bizonylat API végpontok;
- token, nonce, activation és filestore útvonalak;
- hitelesítési és aláírási beállítások;
- polling és státuszpolling paraméterek;
- beküldési és csatolmánykorlátok.

Titkos értékeket (token, jelszó, aláírási kulcsrész, truststore-jelszó) dokumentációban és naplóban sem szabad valós értékkel megjeleníteni.

## HTTP audit és diagnosztika

A `NavHttpAuditHolder`, `NavHttpAuditFormatter` és `NavHttpAuditLogger` a NAV transport eseményeket strukturáltan gyűjti. A formatter maszkolja az érzékeny headereket és credential jellegű értékeket. Az audit trace a magasabb szintű M2M eseménynaplóhoz kapcsolható, de titkos token nem kerülhet letölthető vagy felhasználói naplóba.

## Adatmodell és perzisztencia

A fő JPA entitások:

- `M2mSubmission` – beküldési életciklus;
- `M2mAttachment` – csatolmány;
- `M2mSubmissionEvent` – kommunikációs/eseménynapló;
- `XmlAttachmentReference` – XML-ben deklarált csatolmányhivatkozás;
- `ProxySettings` – legacy/perzisztált proxy konfiguráció.

A repository interfészek Spring Data JPA-ra épülnek.

## Függőségek és modulkapcsolatok

A modul közvetlenül használja:

- `nav-xsd-parser-tool-core` – közös biztonsági és domain segédosztályok;
- `nav-xsd-parser-tool-schema-registry` – XML-hez tartozó séma/metaadat feloldás;
- `nav-xsd-parser-tool-processing` – XML feldolgozási és validációs szolgáltatások;
- Spring Web, Validation, Security és Data JPA;
- Apache HttpClient 5;
- Apache Commons Compress.

A web alkalmazás a modul Spring komponenseit és REST vezérlőit együtt tölti be a teljes M2M XML EDITOR alkalmazással.

## Fejlesztési határok

A NAV protokollspecifikus HTTP részletek maradjanak a `service.nav` rétegben. Az életciklus, csatolmány és preflight szabályok a `SubmissionService` és kapcsolódó szolgáltatások felelősségei. A kliensoldali felület ne kerülje meg ezeket a backend ellenőrzéseket.

Különösen megőrzendő szabályok:

- `SUBMITTED_OK` után nincs módosító M2M művelet;
- több csatolmány esetén nincs globális fájlnév nélküli `fileId` fallback;
- meglévő XML `fileId` nem írható felül kontroll nélkül;
- a Bizonylat route metaadata elsődlegesen a tényleges XML/séma alapján oldódik fel;
- az érzékeny NAV hitelesítési adatok nem kerülhetnek audit- vagy felhasználói naplóba.
