package hu.gov.nav.xsdparsertool.web.githubupdater.api;

import hu.gov.nav.xsdparsertool.web.githubupdater.config.GitHubSchemaUpdaterProperties;
import hu.gov.nav.xsdparsertool.web.githubupdater.dto.GitHubSchemaUpdateRequest;
import hu.gov.nav.xsdparsertool.web.githubupdater.dto.GitHubSchemaUpdateResponse;
import hu.gov.nav.xsdparsertool.web.githubupdater.service.GitHubSchemaUpdaterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adminisztrációs REST vezérlő a GitHub sémafrissítő konfigurációjának lekérdezéséhez, a tényleges frissítéshez és a dry-run futtatáshoz. A kliens által megadott repository- és tagazonosítókat a szolgáltatáshívás előtt szűri.
 */
@RestController
@RequestMapping("/api/admin/github-schema-updater")
public class GitHubSchemaUpdaterController {
    private final GitHubSchemaUpdaterService updaterService;
    private final GitHubSchemaUpdaterProperties properties;

    /**
     * Létrehozza a(z) {@code GitHubSchemaUpdaterController} példányt a működéshez szükséges kezdeti állapottal és függőségekkel.
     *
     * @param updaterService a művelethez átadott {@code updaterService} érték
     * @param properties a művelethez átadott {@code properties} érték
     */
    public GitHubSchemaUpdaterController(GitHubSchemaUpdaterService updaterService,
                                         GitHubSchemaUpdaterProperties properties) {
        this.updaterService = updaterService;
        this.properties = properties;
    }

    /**
     * Visszaadja az admin felület számára szükséges, nem titkos GitHub updater konfigurációt, beleértve a feloldott célkönyvtárat és azt, hogy token konfigurálva van-e.
     *
     * @return a művelet eredménye
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> config() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("enabled", properties.isEnabled());
        response.put("organization", properties.getOrganization());
        response.put("apiBaseUrl", properties.getApiBaseUrl());
        response.put("downloadMode", properties.getDownloadMode());
        response.put("archiveUrlTemplate", properties.getArchiveUrlTemplate());
        response.put("targetSchemaDir", updaterService.resolveTargetSchemaDir().toString());
        response.put("tokenConfigured", properties.hasToken());
        response.put("configuredRepositories", properties.getRepositories());
        response.put("repositoryNamePrefix", properties.getRepositoryNamePrefix());
        response.put("skipExistingTagDirectories", properties.isSkipExistingTagDirectories());
        response.put("rateLimitEnabled", properties.isRateLimitEnabled());
        response.put("rateLimitMaxRetries", properties.getRateLimitMaxRetries());
        response.put("rateLimitDefaultSecondaryWait", properties.getRateLimitDefaultSecondaryWait());
        response.put("rateLimitMaxWait", properties.getRateLimitMaxWait());
        response.put("rateLimitPrintHeaders", properties.isRateLimitPrintHeaders());
        return ResponseEntity.ok(response);
    }

    /**
     * Validálja a kliens frissítési kérését, majd elindítja a tényleges GitHub sémafrissítést.
     *
     * @param request a végrehajtandó frissítés vagy letöltés paraméterei
     * @return a művelet eredménye
     */
    @PostMapping("/update")
    public ResponseEntity<GitHubSchemaUpdateResponse> update(@RequestBody(required = false) GitHubSchemaUpdateRequest request) {
        return ResponseEntity.ok(updaterService.updateSchemas(validateRequest(request, false)));
    }

    /**
     * A kérés tartalmát validálja, majd kényszerítetten dry-run módban futtatja a frissítési folyamatot, így fájlrendszeri telepítés nélkül lehet felmérni a várható műveleteket.
     *
     * @param request a végrehajtandó frissítés vagy letöltés paraméterei
     * @return a művelet eredménye
     */
    @PostMapping("/dry-run")
    public ResponseEntity<GitHubSchemaUpdateResponse> dryRun(@RequestBody(required = false) GitHubSchemaUpdateRequest request) {
        return ResponseEntity.ok(updaterService.updateSchemas(validateRequest(request, true)));
    }

    /**
     * Védett másolatot készít a frissítési kérésből. Korlátozza a repository/tag tételek számát, minden azonosítót karakterkészlet és hossz szerint ellenőriz, és szükség esetén kényszeríti a dry-run jelzőt.
     *
     * @param request a végrehajtandó frissítés vagy letöltés paraméterei
     * @param forceDryRun a művelethez átadott {@code forceDryRun} érték
     * @return a művelet eredménye
     */
    private static GitHubSchemaUpdateRequest validateRequest(GitHubSchemaUpdateRequest request, boolean forceDryRun) {
        GitHubSchemaUpdateRequest safe = new GitHubSchemaUpdateRequest();
        if (request == null) {
            safe.setDryRun(forceDryRun);
            return safe;
        }
        if (request.getRepositories() != null && request.getRepositories().size() > 100) {
            throw new IllegalArgumentException("Túl sok repository szerepel a kérésben.");
        }
        if (request.getRepositoryTags() != null && request.getRepositoryTags().size() > 100) {
            throw new IllegalArgumentException("Túl sok repository tag szerepel a kérésben.");
        }
        java.util.List<String> repositories = new java.util.ArrayList<>();
        if (request.getRepositories() != null) {
            for (String repository : request.getRepositories()) repositories.add(boundedIdentifier(repository, "repository"));
        }
        java.util.Map<String,String> tags = new java.util.LinkedHashMap<>();
        if (request.getRepositoryTags() != null) {
            request.getRepositoryTags().forEach((repository, tag) -> tags.put(boundedIdentifier(repository, "repository"), boundedIdentifier(tag, "tag")));
        }
        safe.setRepositories(repositories);
        safe.setRepositoryTags(tags);
        safe.setForceDownloadAll(request.isForceDownloadAll());
        safe.setDryRun(forceDryRun || request.isDryRun());
        return safe;
    }

    /**
     * Repository- vagy tagazonosítót trimel és szigorú, fájlrendszeri/URL használatra alkalmas karakterkészletre korlátoz.
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
