# nav-xsd-parser-tool-github-updater

## A modul célja

A `nav-xsd-parser-tool-github-updater` modul a M2M XML EDITOR GitHub-alapú Űrlapsablon- és technikai erőforrás-frissítési alrendszere. Feladata a GitHub organization repository-k katalógusának kezelése, a release tagek lekérése, a kijelölt release-ek letöltése, biztonságos kicsomagolása, valamint az XSD, UIModel, XPath/XSL és common erőforrások konfigurált célkönyvtárakba telepítése.

A modul önálló Spring Boot alkalmazást nem indít. A webes alkalmazás komponenseként működik, és REST vezérlőket, szolgáltatásokat, JPA repository-kat, konfigurációs property-ket és hálózati segédosztályokat biztosít.

## Fő felelősségek

- GitHub organization repository-k és release tagek lekérése.
- Lokális, adatbázisban tárolt Űrlapsablon-katalógus karbantartása.
- Távoli és lokális katalógus közötti változások felismerése.
- Kijelölt release-ek letöltése API zipball vagy webes archívum módon.
- GitHub rate-limit válaszok kontrollált újrapróbálása.
- GitHub-specifikus HTTP proxy és truststore konfiguráció alkalmazása.
- ZIP archívumok méret-, darabszám- és útvonalbiztonsági korlátok melletti kicsomagolása.
- Release fájlok típus szerinti telepítése XSD, UIModel, XPath/XSL és common célkönyvtárakba.
- A `full_check_core_public` és `common` technikai repository-k legfrissebb release-ének közvetlen aktív célkönyvtárba telepítése, verzióalmappa és marker nélkül.
- Már telepített release-ek lokális ZIP csomagjának előállítása.

## Fő belépési pontok

### `GitHubSchemaUpdaterService`

A tényleges release-letöltés és telepítés fő szolgáltatása.

```java
GitHubSchemaUpdateRequest request = new GitHubSchemaUpdateRequest();
request.setRepositories(List.of("nav-m2m-sample"));
request.setDryRun(false);

GitHubSchemaUpdateResponse result = updaterService.updateSchemas(request);
```

A `updateSchemas(...)` egyszerre csak egy frissítési futást enged. A repository-lista feloldási prioritása:

1. explicit `repositoryTags`,
2. explicit `repositories`,
3. konfigurált repository-lista,
4. a GitHub organization teljes repository-listája.

### `GitHubTemplateCatalogService`

A katalógus és a háttérfrissítés fő alkalmazási szolgáltatása.

Fontos műveletei:

- `catalog(boolean preferredOnly)` – a perzisztált katalógus lekérése;
- `checkForChanges()` – azonnali távoli változásellenőrzés;
- `startRefresh()` – aszinkron katalógusfrissítés indítása;
- `refreshStatus()` – háttérfrissítés progresszének lekérése;
- `download(...)` – kijelölt repository/tag párok letöltése;
- `writeLocalBundle(...)` – már telepített release erőforrásainak ZIP exportja.

### `GitHubApiClient`

A GitHub HTTP kommunikáció technikai rétege. Kezeli a tokenes API-kéréseket, a proxy/TLS konfigurációt, a rate-limit újrapróbálást és az archívumletöltést.

## REST végpontok

A modul az alábbi fő REST útvonalakat szolgáltatja:

- `GET /api/admin/github-schema-updater/config`
- `POST /api/admin/github-schema-updater/update`
- `POST /api/admin/github-schema-updater/dry-run`
- `GET /api/github-templates/catalog`
- `GET /api/github-templates/changes`
- `GET /api/github-templates/notification`
- `POST /api/github-templates/refresh`
- `GET /api/github-templates/refresh/status`
- `POST /api/github-templates/download`
- `GET /api/github-templates/local-bundle`
- `GET /api/github-proxy-settings`
- `POST /api/github-proxy-settings`

## Konfiguráció

A fő property prefix:

```text
nav.xsdparsertool.github-schema-updater
```

Fontos beállítások:

- `enabled`
- `organization`
- `api-base-url`
- `download-mode`
- `archive-url-template`
- `token`
- `repositories`
- `repository-name-prefix`
- `request-timeout`
- `max-pages`
- `skip-existing-tag-directories`
- `target-schema-dir`
- `rate-limit-enabled`
- `rate-limit-max-retries`
- `rate-limit-default-secondary-wait`
- `rate-limit-max-wait`
- `catalog-check-interval`

A token titoknak minősül. A kliens csak azt jelzi a REST réteg felé, hogy token konfigurálva van-e; a token értékét nem szabad válaszban vagy diagnosztikai naplóban megjeleníteni.

## Letöltési módok

A `GitHubSchemaDownloadMode` három módot támogat:

- `API_ZIPBALL` – GitHub REST API zipball;
- `WEB_ARCHIVE` – github.com webes archívum;
- `WEB_ARCHIVE_WITH_API_FALLBACK` – elsőként webes archívum, majd fallbackként API zipball.

A repository- és taglisták lekérése a választott archívumletöltési módtól függetlenül a GitHub API-n keresztül történik.

## Biztonsági korlátok

A `GitHubPathSafety` minden külső repository/tag értéket biztonságos fájlrendszeri szegmensre korlátoz. A ZIP kicsomagolás külön védekezik a path traversal és túlméretezett archívumok ellen. A jelenlegi korlátok között szerepel a maximális ZIP-bejegyzésszám, az egy bejegyzésre jutó maximális méret és a teljes kibontott méret felső határa.

A proxybeállításokat a modul saját konfigurációs portján keresztül olvassa, így a GitHub kliens nem esik vissza automatikusan más integrációk JVM-szintű proxyjára.

## Katalógus és letöltés szétválasztása

A katalógusfrissítés és a release-ek tényleges letöltése két külön művelet. GitHub token nélkül a már adatbázisban tárolt lokális katalógus továbbra is megtekinthető, de külső változásellenőrzés vagy letöltés nem indítható.

## Kapcsolódó modulok

- `nav-xsd-parser-tool-core` – közös biztonságos fájlműveletek és alapmodellek.
- `nav-xsd-parser-tool-web` – a modul Spring komponenseinek bekötése, jogosultságkezelés és konfigurációs tároló implementáció.
- `nav-xsd-parser-tool-schema-registry` – a letöltött és telepített sémák későbbi feloldása és indexelése.

## Fejlesztési szabály

A modul fájlkezelési, ZIP-bontási, repository/tag-validációs, rate-limit és titokkezelési szabályai biztonsági szempontból érzékenyek. Módosításkor ezek nem egyszerűsíthetők ellenőrzés nélkül.
