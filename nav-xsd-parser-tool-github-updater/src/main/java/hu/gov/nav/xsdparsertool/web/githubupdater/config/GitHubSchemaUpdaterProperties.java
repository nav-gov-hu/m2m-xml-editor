package hu.gov.nav.xsdparsertool.web.githubupdater.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * A GitHub organization alapú séma- és Űrlapsablon-frissítő konfigurációs értékeit tartalmazza. A property-k a {@code nav.xsdparsertool.github-schema-updater} prefix alatt köthetők be.
 */
@ConfigurationProperties(prefix = "nav.xsdparsertool.github-schema-updater")
public class GitHubSchemaUpdaterProperties {
    /** A frissítő API és szolgáltatás engedélyezése vagy tiltása. */
    private boolean enabled = true;

    /** A GitHub organization tulajdonosneve. */
    private String organization = "nav-gov-hu-templates";

    /** A GitHub REST API alap URL-je. */
    private String apiBaseUrl = "https://api.github.com";

    /** A release-archívum letöltési módja; a repository- és taglisták lekérése ettől függetlenül továbbra is a GitHub API-t használja. */
    private GitHubSchemaDownloadMode downloadMode = GitHubSchemaDownloadMode.API_ZIPBALL;

    /** A webes archívum URL-sablonja; támogatott helyőrzők: {@code {owner}}, {@code {repo}}, {@code {tag}}. */
    private String archiveUrlTemplate = "https://github.com/{owner}/{repo}/archive/refs/tags/{tag}.zip";

    /** Csak webes archívumletöltéskor küldött extra HTTP fejlécek, például belső proxykövetelményekhez. */
    private Map<String, String> webArchiveHeaders = new LinkedHashMap<>();

    /** Opcionális GitHub hozzáférési token; privát/belső repository-khoz és magasabb API-kvótához szükséges. */
    private String token;

    /** Opcionális explicit repository-engedélylista; üres listánál az organization minden látható repository-ja figyelembe vehető. */
    private List<String> repositories = new ArrayList<>();

    /** Opcionális repository névprefix-szűrő; üres értéknél nincs prefix szerinti korlátozás. */
    private String repositoryNamePrefix;

    /** A GitHub API- és archívumletöltési HTTP kérések időkorlátja. */
    private Duration requestTimeout = Duration.ofSeconds(60);

    /** A GitHub lapozott API-hívásokból beolvasható maximális oldalszám biztonsági korlátként. */
    private int maxPages = 50;

    /** Igaz értéknél a meglévő lokális repository/tag könyvtárak megmaradnak és a letöltés kihagyható. */
    private boolean skipExistingTagDirectories = true;

    /** A letöltés közben a schema-dir alatt használt ideiglenes könyvtár neve. */
    private String tempDirectoryName = ".github-schema-updater-tmp";

    /** Opcionális célkönyvtár-felülírás; hiányában a {@code nav.xsdparsertool.paths.schema-dir} értéke használatos. */
    private Path targetSchemaDir;

    /** Engedélyezi a GitHub elsődleges és másodlagos rate-limit válaszokhoz tartozó újrapróbálási logikát. */
    private boolean rateLimitEnabled = true;

    /** Rate-limit válasz esetén engedélyezett maximális újrapróbálások száma. */
    private int rateLimitMaxRetries = 5;

    /** Másodlagos rate-limit esetén használt alapértelmezett várakozás, ha nincs Retry-After fejléc. */
    private Duration rateLimitDefaultSecondaryWait = Duration.ofSeconds(60);

    /** A rate-limit miatti bármely várakozás felső biztonsági korlátja. */
    private Duration rateLimitMaxWait = Duration.ofMinutes(15);

    /** Jelzi, hogy diagnosztikai célból naplózhatók-e a GitHub rate-limit fejlécek. */
    private boolean rateLimitPrintHeaders = false;

    /** Az organization háttérben végzett változásellenőrzésének időköze; nulla vagy negatív érték kikapcsolja a pollingot. */
    private Duration catalogCheckInterval = Duration.ofMinutes(15);

    /**
     * Visszaadja a(z) engedélyezettság aktuális értékét.
     *
     * @return engedélyezettság
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Beállítja a(z) engedélyezettság értékét.
     *
     * @param enabled a művelethez átadott {@code enabled} érték
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Visszaadja a(z) GitHub organization neve aktuális értékét.
     *
     * @return GitHub organization neve
     */
    public String getOrganization() {
        return organization;
    }

    /**
     * Beállítja a(z) GitHub organization neve értékét.
     *
     * @param organization a művelethez átadott {@code organization} érték
     */
    public void setOrganization(String organization) {
        this.organization = organization;
    }

    /**
     * Visszaadja a(z) GitHub API alap URL-je aktuális értékét.
     *
     * @return GitHub API alap URL-je
     */
    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    /**
     * Beállítja a(z) GitHub API alap URL-je értékét.
     *
     * @param apiBaseUrl a művelethez átadott {@code apiBaseUrl} érték
     */
    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    /**
     * Visszaadja a(z) archívumletöltési mód aktuális értékét.
     *
     * @return archívumletöltési mód
     */
    public GitHubSchemaDownloadMode getDownloadMode() {
        return downloadMode;
    }

    /**
     * Beállítja a(z) archívumletöltési mód értékét.
     *
     * @param downloadMode a művelethez átadott {@code downloadMode} érték
     */
    public void setDownloadMode(GitHubSchemaDownloadMode downloadMode) {
        this.downloadMode = downloadMode == null ? GitHubSchemaDownloadMode.API_ZIPBALL : downloadMode;
    }

    /**
     * Visszaadja a(z) webes archívum URL-sablon aktuális értékét.
     *
     * @return webes archívum URL-sablon
     */
    public String getArchiveUrlTemplate() {
        return archiveUrlTemplate;
    }

    /**
     * Beállítja a(z) webes archívum URL-sablon értékét.
     *
     * @param archiveUrlTemplate a művelethez átadott {@code archiveUrlTemplate} érték
     */
    public void setArchiveUrlTemplate(String archiveUrlTemplate) {
        this.archiveUrlTemplate = archiveUrlTemplate;
    }

    /**
     * Visszaadja a(z) webes archívumkérés extra HTTP fejlécei aktuális értékét.
     *
     * @return webes archívumkérés extra HTTP fejlécei
     */
    public Map<String, String> getWebArchiveHeaders() {
        return webArchiveHeaders;
    }

    /**
     * Beállítja a(z) webes archívumkérés extra HTTP fejlécei értékét.
     *
     * @param webArchiveHeaders a művelethez átadott {@code webArchiveHeaders} érték
     */
    public void setWebArchiveHeaders(Map<String, String> webArchiveHeaders) {
        this.webArchiveHeaders = webArchiveHeaders == null ? new LinkedHashMap<>() : new LinkedHashMap<>(webArchiveHeaders);
    }

    /**
     * Visszaadja a(z) GitHub hozzáférési token aktuális értékét.
     *
     * @return GitHub hozzáférési token
     */
    public String getToken() {
        return token;
    }

    /**
     * Beállítja a(z) GitHub hozzáférési token értékét.
     *
     * @param token a művelethez átadott {@code token} érték
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * Visszaadja a(z) konfigurált repository-lista aktuális értékét.
     *
     * @return konfigurált repository-lista
     */
    public List<String> getRepositories() {
        return repositories;
    }

    /**
     * Beállítja a(z) konfigurált repository-lista értékét.
     *
     * @param repositories a művelethez átadott {@code repositories} érték
     */
    public void setRepositories(List<String> repositories) {
        this.repositories = repositories == null ? new ArrayList<>() : new ArrayList<>(repositories);
    }

    /**
     * Visszaadja a(z) repository névprefix aktuális értékét.
     *
     * @return repository névprefix
     */
    public String getRepositoryNamePrefix() {
        return repositoryNamePrefix;
    }

    /**
     * Beállítja a(z) repository névprefix értékét.
     *
     * @param repositoryNamePrefix a művelethez átadott {@code repositoryNamePrefix} érték
     */
    public void setRepositoryNamePrefix(String repositoryNamePrefix) {
        this.repositoryNamePrefix = repositoryNamePrefix;
    }

    /**
     * Visszaadja a(z) HTTP kérési időkorlát aktuális értékét.
     *
     * @return HTTP kérési időkorlát
     */
    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    /**
     * Beállítja a(z) HTTP kérési időkorlát értékét.
     *
     * @param requestTimeout a művelethez átadott {@code requestTimeout} érték
     */
    public void setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    /**
     * Visszaadja a(z) API lapozás maximális oldalszáma aktuális értékét.
     *
     * @return API lapozás maximális oldalszáma
     */
    public int getMaxPages() {
        return maxPages;
    }

    /**
     * Beállítja a(z) API lapozás maximális oldalszáma értékét.
     *
     * @param maxPages a művelethez átadott {@code maxPages} érték
     */
    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
    }

    /**
     * Visszaadja a(z) meglévő tagkönyvtárak kihagyási jelzője aktuális értékét.
     *
     * @return meglévő tagkönyvtárak kihagyási jelzője
     */
    public boolean isSkipExistingTagDirectories() {
        return skipExistingTagDirectories;
    }

    /**
     * Beállítja a(z) meglévő tagkönyvtárak kihagyási jelzője értékét.
     *
     * @param skipExistingTagDirectories a művelethez átadott {@code skipExistingTagDirectories} érték
     */
    public void setSkipExistingTagDirectories(boolean skipExistingTagDirectories) {
        this.skipExistingTagDirectories = skipExistingTagDirectories;
    }

    /**
     * Visszaadja a(z) ideiglenes könyvtár neve aktuális értékét.
     *
     * @return ideiglenes könyvtár neve
     */
    public String getTempDirectoryName() {
        return tempDirectoryName;
    }

    /**
     * Beállítja a(z) ideiglenes könyvtár neve értékét.
     *
     * @param tempDirectoryName a művelethez átadott {@code tempDirectoryName} érték
     */
    public void setTempDirectoryName(String tempDirectoryName) {
        this.tempDirectoryName = tempDirectoryName;
    }

    /**
     * Visszaadja a(z) cél séma könyvtár aktuális értékét.
     *
     * @return cél séma könyvtár
     */
    public Path getTargetSchemaDir() {
        return targetSchemaDir;
    }

    /**
     * Beállítja a(z) cél séma könyvtár értékét.
     *
     * @param targetSchemaDir a séma-release-ek alap célkönyvtára
     */
    public void setTargetSchemaDir(Path targetSchemaDir) {
        this.targetSchemaDir = targetSchemaDir;
    }

    /**
     * Visszaadja a(z) rate-limit újrapróbálás engedélyezettsége aktuális értékét.
     *
     * @return rate-limit újrapróbálás engedélyezettsége
     */
    public boolean isRateLimitEnabled() {
        return rateLimitEnabled;
    }

    /**
     * Beállítja a(z) rate-limit újrapróbálás engedélyezettsége értékét.
     *
     * @param rateLimitEnabled a művelethez átadott {@code rateLimitEnabled} érték
     */
    public void setRateLimitEnabled(boolean rateLimitEnabled) {
        this.rateLimitEnabled = rateLimitEnabled;
    }

    /**
     * Visszaadja a(z) rate-limit miatti maximális újrapróbálások száma aktuális értékét.
     *
     * @return rate-limit miatti maximális újrapróbálások száma
     */
    public int getRateLimitMaxRetries() {
        return rateLimitMaxRetries;
    }

    /**
     * Beállítja a(z) rate-limit miatti maximális újrapróbálások száma értékét.
     *
     * @param rateLimitMaxRetries a művelethez átadott {@code rateLimitMaxRetries} érték
     */
    public void setRateLimitMaxRetries(int rateLimitMaxRetries) {
        this.rateLimitMaxRetries = rateLimitMaxRetries;
    }

    /**
     * Visszaadja a(z) másodlagos rate-limit alapértelmezett várakozása aktuális értékét.
     *
     * @return másodlagos rate-limit alapértelmezett várakozása
     */
    public Duration getRateLimitDefaultSecondaryWait() {
        return rateLimitDefaultSecondaryWait;
    }

    /**
     * Beállítja a(z) másodlagos rate-limit alapértelmezett várakozása értékét.
     *
     * @param rateLimitDefaultSecondaryWait a művelethez átadott {@code rateLimitDefaultSecondaryWait} érték
     */
    public void setRateLimitDefaultSecondaryWait(Duration rateLimitDefaultSecondaryWait) {
        this.rateLimitDefaultSecondaryWait = rateLimitDefaultSecondaryWait;
    }

    /**
     * Visszaadja a(z) rate-limit maximális várakozása aktuális értékét.
     *
     * @return rate-limit maximális várakozása
     */
    public Duration getRateLimitMaxWait() {
        return rateLimitMaxWait;
    }

    /**
     * Beállítja a(z) rate-limit maximális várakozása értékét.
     *
     * @param rateLimitMaxWait a művelethez átadott {@code rateLimitMaxWait} érték
     */
    public void setRateLimitMaxWait(Duration rateLimitMaxWait) {
        this.rateLimitMaxWait = rateLimitMaxWait;
    }

    /**
     * Visszaadja a(z) rate-limit fejlécek diagnosztikai naplózásának jelzője aktuális értékét.
     *
     * @return rate-limit fejlécek diagnosztikai naplózásának jelzője
     */
    public boolean isRateLimitPrintHeaders() {
        return rateLimitPrintHeaders;
    }

    /**
     * Beállítja a(z) rate-limit fejlécek diagnosztikai naplózásának jelzője értékét.
     *
     * @param rateLimitPrintHeaders a művelethez átadott {@code rateLimitPrintHeaders} érték
     */
    public void setRateLimitPrintHeaders(boolean rateLimitPrintHeaders) {
        this.rateLimitPrintHeaders = rateLimitPrintHeaders;
    }


    /**
     * Visszaadja a(z) katalógusellenőrzés időköze aktuális értékét.
     *
     * @return katalógusellenőrzés időköze
     */
    public Duration getCatalogCheckInterval() {
        return catalogCheckInterval;
    }

    /**
     * Beállítja a(z) katalógusellenőrzés időköze értékét.
     *
     * @param catalogCheckInterval a művelethez átadott {@code catalogCheckInterval} érték
     */
    public void setCatalogCheckInterval(Duration catalogCheckInterval) {
        this.catalogCheckInterval = catalogCheckInterval;
    }

    /**
     * A bemeneti állapot és a modul szabályai alapján eldönti, hogy a vizsgált feltétel teljesül-e.
     *
     * @return a művelet eredménye
     */
    public boolean hasToken() {
        return StringUtils.hasText(token);
    }
}
