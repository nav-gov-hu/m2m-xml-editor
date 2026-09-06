package hu.gov.nav.xsdparsertool.web.githubupdater.api;

import hu.gov.nav.xsdparsertool.web.githubupdater.dto.GitHubSchemaUpdateResponse;
import hu.gov.nav.xsdparsertool.web.githubupdater.dto.GitHubTemplateCatalogDtos;
import hu.gov.nav.xsdparsertool.web.githubupdater.service.GitHubTemplateCatalogService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * REST vezérlő a GitHub Űrlapsablon-katalógus lekérdezéséhez, változásellenőrzéséhez, háttérfrissítéséhez, release-letöltéséhez és a már telepített lokális csomag streameléséhez.
 */
@RestController
@RequestMapping("/api/github-templates")
public class GitHubTemplateCatalogController {
    private final GitHubTemplateCatalogService service;
    /**
     * Létrehozza a(z) {@code GitHubTemplateCatalogController} példányt a működéshez szükséges kezdeti állapottal és függőségekkel.
     *
     * @param service a művelethez átadott {@code service} érték
     */
    public GitHubTemplateCatalogController(GitHubTemplateCatalogService service) { this.service = service; }

    /**
     * Lekéri a perzisztált GitHub Űrlapsablon-katalógust; a {@code preferredOnly} jelzővel csak a lokálisan ismert űrlaptípusokhoz tartozó sorok kérhetők.
     *
     * @param preferredOnly ha igaz, csak a lokálisan ismert űrlaptípusok repository-it tartalmazza a katalógus
     * @return az összeállított katalógusválasz
     */
    @GetMapping("/catalog")
    public ResponseEntity<GitHubTemplateCatalogDtos.CatalogResponse> catalog(
            @RequestParam(defaultValue = "true") boolean preferredOnly) {
        return ResponseEntity.ok(service.catalog(preferredOnly));
    }

    /**
     * Azonnali távoli GitHub változásellenőrzést kér a katalógusszolgáltatástól.
     *
     * @return a művelet eredménye
     * @throws Exception ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    @GetMapping("/changes")
    public ResponseEntity<GitHubTemplateCatalogDtos.ChangeCheckResponse> changes() throws Exception {
        return ResponseEntity.ok(service.checkForChanges());
    }

    /**
     * Visszaadja a legutóbbi változásellenőrzésből előállított értesítési állapotot anélkül, hogy önmagában új távoli ellenőrzést indítana.
     *
     * @return a legutóbbi változásellenőrzésből képzett értesítési állapot
     */
    @GetMapping("/notification")
    public ResponseEntity<GitHubTemplateCatalogDtos.NotificationResponse> notification() {
        return ResponseEntity.ok(service.notification());
    }

    /**
     * Elindítja a katalógus háttérfrissítését; a kérés azonnal visszatér az indítás eredményével.
     *
     * @return a művelet eredménye
     */
    @PostMapping("/refresh")
    public ResponseEntity<GitHubTemplateCatalogDtos.RefreshStartResponse> refresh() {
        return ResponseEntity.ok(service.startRefresh());
    }

    /**
     * Visszaadja a háttérben futó katalógusfrissítés aktuális progresszét és befejezési állapotát.
     *
     * @return a háttérfrissítés aktuális állapota
     */
    @GetMapping("/refresh/status")
    public ResponseEntity<GitHubTemplateCatalogDtos.RefreshStatusResponse> refreshStatus() {
        return ResponseEntity.ok(service.refreshStatus());
    }

    /**
     * Validálja a kiválasztott repository/tag párokat, majd elindítja azok tényleges letöltését és telepítését.
     *
     * @param request a végrehajtandó frissítés vagy letöltés paraméterei
     * @return a művelet eredménye
     */
    @PostMapping("/download")
    public ResponseEntity<GitHubSchemaUpdateResponse> download(
            @RequestBody GitHubTemplateCatalogDtos.DownloadRequest request) {
        if (request == null || request.items() == null) throw new IllegalArgumentException("A letöltési kérés hiányzik.");
        if (request.items().size() > 100) throw new IllegalArgumentException("Túl sok letöltési elem.");
        java.util.List<GitHubTemplateCatalogDtos.DownloadItem> safeItems = request.items().stream()
                .map(item -> new GitHubTemplateCatalogDtos.DownloadItem(
                        boundedIdentifier(item.repository(), "repository"),
                        boundedIdentifier(item.tag(), "tag")))
                .toList();
        GitHubTemplateCatalogDtos.DownloadRequest safeRequest = new GitHubTemplateCatalogDtos.DownloadRequest(safeItems, request.force());
        return ResponseEntity.ok(service.download(safeRequest));
    }

    /**
     * A már helyben telepített repository/tag erőforrásokból ZIP csomagot streamel a kliensnek. A repository- és tagazonosítókat a fájlnév és az útvonal felépítése előtt validálja.
     *
     * @param repository a GitHub repository neve
     * @param tag a release tag
     * @return a művelet eredménye
     */

    /** A kijelölt lokális GitHub release-eket eltávolítja a fájlrendszerből és a lokális katalógus-adatbázisból. */
    @PostMapping("/local-delete")
    public ResponseEntity<GitHubTemplateCatalogDtos.LocalDeleteResponse> deleteLocal(
            @RequestBody GitHubTemplateCatalogDtos.LocalDeleteRequest request) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("Legalább egy lokális repository/release kijelölése szükséges.");
        }
        if (request.items().size() > 100) throw new IllegalArgumentException("Túl sok törlendő elem.");
        java.util.List<GitHubTemplateCatalogDtos.DownloadItem> safeItems = request.items().stream()
                .map(item -> new GitHubTemplateCatalogDtos.DownloadItem(
                        boundedIdentifier(item.repository(), "repository"), boundedIdentifier(item.tag(), "tag")))
                .toList();
        return ResponseEntity.ok(service.deleteLocal(new GitHubTemplateCatalogDtos.LocalDeleteRequest(safeItems)));
    }

    @GetMapping(value = "/local-bundle", produces = "application/zip")
    public ResponseEntity<StreamingResponseBody> localBundle(
            @RequestParam String repository,
            @RequestParam String tag) {
        String safeRepository = boundedIdentifier(repository, "repository");
        String safeTag = boundedIdentifier(tag, "tag");
        String fileName = service.localBundleFileName(safeRepository, safeTag);
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        StreamingResponseBody body = output -> service.writeLocalBundle(safeRepository, safeTag, output);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + encodedFileName)
                .body(body);
    }
    /**
     * Repository- vagy tagazonosítót trimel, maximális hosszra és engedélyezett karakterkészletre ellenőriz; hibás értéket még a szolgáltatás vagy fájlrendszeri feloldás előtt elutasít.
     *
     * @param raw a nyers bemeneti szöveg
     * @param field a validált mező neve
     * @return a művelet eredménye
     */
    private static String boundedIdentifier(String raw, String field) {
        if (raw == null) throw new IllegalArgumentException("Hiányzó " + field + ".");
        String value = raw.trim();
        if (value.isEmpty() || value.length() > 160 || !value.matches("[A-Za-z0-9._/@+-]+")) {
            throw new IllegalArgumentException("Érvénytelen " + field + ".");
        }
        return value;
    }
}
