package hu.gov.nav.xsdparsertool.web.githubupdater.service;

import hu.gov.nav.xsdparsertool.core.support.SecureFileOperations;
import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hu.gov.nav.xsdparsertool.web.githubupdater.config.GitHubSchemaDownloadMode;
import hu.gov.nav.xsdparsertool.web.githubupdater.config.GitHubSchemaUpdaterProperties;
import hu.gov.nav.xsdparsertool.web.githubupdater.domain.GitHubProxySettings;
import hu.gov.nav.xsdparsertool.web.githubupdater.spi.GitHubNetworkSettingsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.win.WindowsCredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.WinHttpClients;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.FilterInputStream;
import java.net.URI;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import javax.net.ssl.SSLSession;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * A GitHub REST API és release-archívumok HTTP elérését megvalósító kliens. Kezeli a proxy/TLS konfigurációt, a tokenes API-kéréseket, a rate-limit miatti újrapróbálást, a diagnosztikai naplózást és a letöltött tartalom fájlba írását.
 */
@Component
public class GitHubApiClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubApiClient.class);

    private final GitHubSchemaUpdaterProperties properties;
    private final ObjectMapper objectMapper;
    private final GitHubNetworkSettingsProvider networkSettingsProvider;
    private final AtomicReference<ProxyAuthCacheEntry> proxyAuthCache = new AtomicReference<>();

    /**
     * Létrehozza a(z) {@code GitHubApiClient} példányt a működéshez szükséges kezdeti állapottal és függőségekkel.
     *
     * @param properties a művelethez átadott {@code properties} érték
     * @param objectMapper a művelethez átadott {@code objectMapper} érték
     * @param networkSettingsProvider az alkalmazás általános proxy/TLS beállításait biztosító provider
     */
    public GitHubApiClient(GitHubSchemaUpdaterProperties properties,
                           ObjectMapper objectMapper,
                           GitHubNetworkSettingsProvider networkSettingsProvider) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.networkSettingsProvider = networkSettingsProvider;
    }

    /**
     * Az aktuális proxykonfiguráció alapján felépíti a GitHub-kérésekhez használt HTTP klienst. A kliens minden híváskor a konfigurációs store jelenlegi állapotát veszi figyelembe.
     *
     * @return a művelet eredménye
     */
    private HttpClient currentHttpClient() {
        // Minden GitHub kérés az adatbázis aktuális SYSTEM_CONFIGURATION / SYSTEM_SECRET
        // értékeiből felépített klienst használja, ezért mentés után nem kell újraindítás.
        return buildHttpClient(networkSettingsProvider.load());
    }

    /**
     * A megadott általános proxy-, hitelesítési, timeout- és TLS/truststore-beállításokból a korábban bevált JDK {@link java.net.http.HttpClient} példányt épít. A proxy hitelesítését a {@link java.net.Authenticator} kezeli; interaktív Kerberos/JAAS bejelentkezést nem indít.
     *
     * @param settings az aktuális proxy/TLS beállítások
     * @return a művelet eredménye
     */
    private HttpClient buildHttpClient(GitHubProxySettings settings) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(safeTimeout(properties.getRequestTimeout()));

        try {
            if (settings != null && settings.isEnabled() && settings.getProxyUrl() != null && !settings.getProxyUrl().isBlank()) {
                String host = normalizeProxyHost(settings.getProxyUrl());
                int port = settings.getProxyPort() == null ? 0 : settings.getProxyPort();
                if (port > 0) {
                    builder.proxy(ProxySelector.of(new InetSocketAddress(host, port)));
                    if (settings.getUsername() != null && !settings.getUsername().isBlank()) {
                        if (settings.getPassword() != null && !settings.getPassword().isBlank()) {
                            builder.authenticator(new Authenticator() {
                                /**
                                 * Visszaadja a(z) passwordAuthentication aktuális értékét.
                                 *
                                 * @return a(z) passwordAuthentication érték
                                 */
                                @Override
                                protected PasswordAuthentication getPasswordAuthentication() {
                                    return new PasswordAuthentication(settings.getUsername(), settings.getPassword().toCharArray());
                                }
                            });
                            LOGGER.info("GitHub schema updater JDK HttpClient proxyhitelesítést használ.");
                            LOGGER.debug("GitHub schema updater proxyhitelesítési részletek: username={}", settings.getUsername());
                        } else {
                            LOGGER.warn("A GitHub proxyhitelesítés nincs teljesen konfigurálva; a kapcsolat proxyhitelesítés nélkül kerül megkísérlésre.");
                        }
                    } else {
                        LOGGER.info("GitHub proxyhitelesítés nincs konfigurálva; a kapcsolat proxyhitelesítés nélkül kerül megkísérlésre.");
                    }
                    LOGGER.info("GitHub schema updater az általános HTTP proxy beállítást használja.");
                    LOGGER.debug("GitHub schema updater proxy részletek: host={}, port={}", host, port);
                }
            }
            SSLContext sslContext = buildSslContext(settings);
            if (sslContext != null) {
                builder.sslContext(sslContext);
                LOGGER.info("GitHub schema updater az általános TLS/truststore beállítást használja.");
            }
        } catch (Exception ex) {
            LOGGER.warn("GitHub schema updater proxy/TLS settings could not be applied. Direct HTTP client will be used. Cause: {}", ex.getMessage());
        }

        return builder.build();
    }

    /**
     * A konfigurált proxy URL-ből használható hosztnevet képez; szükség esetén URI-ként értelmezi a sémával megadott címet, és elutasítja az üres/érvénytelen hostot.
     *
     * @param rawProxyUrl a művelethez átadott {@code rawProxyUrl} érték
     * @return a művelet eredménye
     */
    private String normalizeProxyHost(String rawProxyUrl) {
        String raw = rawProxyUrl == null ? "" : rawProxyUrl.trim();
        try {
            URI uri = raw.contains("://") ? URI.create(raw) : URI.create("http://" + raw);
            if (uri.getHost() != null && !uri.getHost().isBlank()) {
                return uri.getHost();
            }
        } catch (IllegalArgumentException ignored) {
            // Fallback below.
        }
        String host = raw.replace("http://", "").replace("https://", "");
        int slash = host.indexOf('/');
        if (slash >= 0) host = host.substring(0, slash);
        int colon = host.indexOf(':');
        if (colon >= 0) host = host.substring(0, colon);
        return host;
    }

    /**
     * A proxybeállítások alapján TLS kontextust épít. Egyedi truststore esetén azt tölti be, kikapcsolt SSL-ellenőrzésnél külön trust managert alkalmaz; egyébként a platform alapértelmezett bizalmi láncát használja.
     *
     * @param settings az aktuális proxy/TLS beállítások
     * @return a művelet eredménye
     * @throws Exception ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private SSLContext buildSslContext(GitHubProxySettings settings) throws Exception {
        if (settings == null) {
            return null;
        }
        if (settings.isSslVerificationDisabled()) {
            TrustManager[] trustAll = new TrustManager[]{new X509TrustManager() {
                /**
                 * Az SSL-ellenőrzés explicit kikapcsolásakor szándékosan nem végez kliens-tanúsítvány ellenőrzést. Ez a trust-all ág kizárólag a konfigurált opt-out esetén épül fel.
                 *
                 * @param chain a művelethez átadott {@code chain} érték
                 * @param authType a művelethez átadott {@code authType} érték
                 */
                @Override public void checkClientTrusted(X509Certificate[] chain, String authType) { }
                /**
                 * Az SSL-ellenőrzés explicit kikapcsolásakor szándékosan nem végez szerver-tanúsítvány ellenőrzést. Ez a trust-all ág kizárólag a konfigurált opt-out esetén épül fel.
                 *
                 * @param chain a művelethez átadott {@code chain} érték
                 * @param authType a művelethez átadott {@code authType} érték
                 */
                @Override public void checkServerTrusted(X509Certificate[] chain, String authType) { }
                /**
                 * A trust-all megvalósítás nem korlátozza az elfogadott kibocsátókat, ezért üres kibocsátólistát ad vissza.
                 *
                 * @return a(z) acceptedIssuers érték
                 */
                @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            }};
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, trustAll, new java.security.SecureRandom());
            return context;
        }
        if (settings.getTrustStorePath() != null && !settings.getTrustStorePath().isBlank()) {
            String type = settings.getTrustStoreType() == null || settings.getTrustStoreType().isBlank() ? "JKS" : settings.getTrustStoreType();
            KeyStore keyStore = KeyStore.getInstance(type);
            try (FileInputStream input = new FileInputStream(settings.getTrustStorePath().trim())) {
                char[] password = settings.getTrustStorePassword() == null ? new char[0] : settings.getTrustStorePassword().toCharArray();
                keyStore.load(input, password);
            }
            javax.net.ssl.TrustManagerFactory trustManagerFactory = javax.net.ssl.TrustManagerFactory.getInstance(javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, trustManagerFactory.getTrustManagers(), null);
            return context;
        }
        return null;
    }

    /**
     * Egy távoli GitHub repository katalógusépítéshez szükséges összefoglaló metaadatait hordozó immutable értékobjektum.
     */
    public record RepositorySummary(String name, String description, Instant updatedAt, String htmlUrl, boolean archived) {}

    /**
     * Lapozva lekéri a konfigurált GitHub organization repository-metaadatait. A maximális oldalszámot a konfiguráció korlátozza, a JSON-választ pedig a katalógushoz szükséges mezőkre képezi le.
     *
     * @return a távoli repository-k metaadatainak listája
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     * @throws InterruptedException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    public List<RepositorySummary> listOrganizationRepositorySummaries() throws IOException, InterruptedException {
        List<RepositorySummary> repositories = new ArrayList<>();
        for (int page = 1; page <= Math.max(1, properties.getMaxPages()); page++) {
            URI uri = UriComponentsBuilder.fromUriString(properties.getApiBaseUrl())
                    .pathSegment("orgs", properties.getOrganization(), "repos")
                    .queryParam("type", "all")
                    .queryParam("sort", "updated")
                    .queryParam("direction", "desc")
                    .queryParam("per_page", 100)
                    .queryParam("page", page)
                    .build().toUri();
            JsonNode response = sendJson(uri);
            if (!response.isArray() || response.isEmpty()) break;
            for (JsonNode repo : response) {
                String name = repo.path("name").asText("");
                if (name.isBlank()) continue;
                String description = repo.path("description").isNull() ? "" : repo.path("description").asText("");
                String htmlUrl = repo.path("html_url").asText("");
                Instant updatedAt = null;
                String updated = repo.path("pushed_at").asText(repo.path("updated_at").asText(""));
                if (!updated.isBlank()) {
                    try { updatedAt = Instant.parse(updated); } catch (Exception ignored) { }
                }
                repositories.add(new RepositorySummary(name, description, updatedAt, htmlUrl, repo.path("archived").asBoolean(false)));
            }
            if (response.size() < 100) break;
        }
        return repositories;
    }

    /**
     * Az organization repository-összefoglalóiból csak a repository-nevek listáját állítja elő.
     *
     * @return a távoli repository-nevek listája
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     * @throws InterruptedException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    public List<String> listOrganizationRepositories() throws IOException, InterruptedException {
        List<String> repositories = new ArrayList<>();
        for (int page = 1; page <= Math.max(1, properties.getMaxPages()); page++) {
            URI uri = UriComponentsBuilder.fromUriString(properties.getApiBaseUrl())
                    .pathSegment("orgs", properties.getOrganization(), "repos")
                    .queryParam("type", "all")
                    .queryParam("per_page", 100)
                    .queryParam("page", page)
                    .build()
                    .toUri();
            JsonNode response = sendJson(uri);
            if (!response.isArray() || response.isEmpty()) {
                break;
            }
            for (JsonNode repo : response) {
                JsonNode name = repo.get("name");
                if (name != null && name.isTextual()) {
                    repositories.add(name.asText());
                }
            }
            if (response.size() < 100) {
                break;
            }
        }
        return repositories;
    }

    /**
     * Lapozva lekéri egy repository tageit a GitHub API-ból, és a válaszból a tagneveket adja vissza.
     *
     * @param repositoryName a GitHub repository neve
     * @return a repository távoli tagnevei
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     * @throws InterruptedException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    public List<String> listRepositoryTags(String repositoryName) throws IOException, InterruptedException {
        List<String> tags = new ArrayList<>();
        for (int page = 1; page <= Math.max(1, properties.getMaxPages()); page++) {
            URI uri = UriComponentsBuilder.fromUriString(properties.getApiBaseUrl())
                    .pathSegment("repos", properties.getOrganization(), repositoryName, "tags")
                    .queryParam("per_page", 100)
                    .queryParam("page", page)
                    .build()
                    .toUri();
            JsonNode response = sendJson(uri);
            if (!response.isArray() || response.isEmpty()) {
                break;
            }
            for (JsonNode tag : response) {
                JsonNode name = tag.get("name");
                if (name != null && name.isTextual()) {
                    tags.add(name.asText());
                }
            }
            if (response.size() < 100) {
                break;
            }
        }
        return tags;
    }

    /** Közvetlenül lekéri a catalog repository content/artifact-catalog.xml állományát. */
    public String fetchArtifactCatalogXml() throws IOException, InterruptedException {
        String template = StringUtils.hasText(properties.getCatalogXmlUrlTemplate())
                ? properties.getCatalogXmlUrlTemplate()
                : "https://raw.githubusercontent.com/{owner}/{repo}/{branch}/content/artifact-catalog.xml";
        String url = template
                .replace("{owner}", encodePathSegment(properties.getOrganization()))
                .replace("{repo}", encodePathSegment(properties.getCatalogRepository()))
                .replace("{branch}", encodePathSegment(properties.getCatalogBranch()));
        URI uri = URI.create(url);
        HttpResponse<String> response = sendStringWithRateLimit(() -> {
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                    .timeout(safeTimeout(properties.getRequestTimeout()))
                    .header("Accept", "application/xml,text/xml,*/*")
                    .header("User-Agent", "M2M-XML-EDITOR")
                    .GET();
            if (properties.hasToken()) builder.header("Authorization", "Bearer " + properties.getToken());
            return builder.build();
        }, "GitHub artifact catalog download", properties.getCatalogRepository());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Az artifact-catalog.xml letöltése sikertelen: HTTP " + response.statusCode());
        }
        LOGGER.info("GitHub artifact catalog downloaded: repository={}, branch={}, bytes={}",
                properties.getCatalogRepository(), properties.getCatalogBranch(),
                response.body() == null ? 0 : response.body().getBytes(StandardCharsets.UTF_8).length);
        return response.body();
    }

    /**
     * Downloads a repository tag archive according to the configured download mode.
     *
     * @return human-readable source mode used for the successful download
     */
    public String downloadArchive(String repositoryName, String tagName, Path targetZip) throws IOException, InterruptedException {
        GitHubSchemaDownloadMode mode = properties.getDownloadMode();
        if (mode == GitHubSchemaDownloadMode.API_ZIPBALL) {
            downloadApiZipball(repositoryName, tagName, targetZip);
            return "API_ZIPBALL";
        }
        if (mode == GitHubSchemaDownloadMode.WEB_ARCHIVE) {
            downloadWebArchive(repositoryName, tagName, targetZip);
            return "WEB_ARCHIVE";
        }
        try {
            downloadWebArchive(repositoryName, tagName, targetZip);
            return "WEB_ARCHIVE";
        } catch (IOException webArchiveFailed) {
            downloadApiZipball(repositoryName, tagName, targetZip);
            return "API_ZIPBALL fallback after WEB_ARCHIVE failed: " + webArchiveFailed.getMessage();
        }
    }

    /**
     * A GitHub API zipball végpontjáról streamelve tölti le a kijelölt repository/tag archívumát a megadott célfájlba, rate-limit újrapróbálással.
     *
     * @param repositoryName a GitHub repository neve
     * @param tagName a release tag neve
     * @param targetZip a letöltendő ZIP célfájlja
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     * @throws InterruptedException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    public void downloadApiZipball(String repositoryName, String tagName, Path targetZip) throws IOException, InterruptedException {
        URI uri = UriComponentsBuilder.fromUriString(properties.getApiBaseUrl())
                .pathSegment("repos", properties.getOrganization(), repositoryName, "zipball", tagName)
                .build()
                .toUri();
        HttpResponse<InputStream> response = sendInputStreamWithRateLimit(() -> baseApiRequest(uri)
                .timeout(safeTimeout(properties.getRequestTimeout()))
                .GET()
                .build(), "GitHub API zipball download", repositoryName + "/" + tagName);
        copySuccessfulResponse(response, targetZip, "GitHub API zipball download", repositoryName, tagName);
    }

    /**
     * A konfigurált webes archívum URL-sablon és HTTP fejlécek használatával tölti le a kijelölt repository/tag ZIP archívumát.
     *
     * @param repositoryName a GitHub repository neve
     * @param tagName a release tag neve
     * @param targetZip a letöltendő ZIP célfájlja
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     * @throws InterruptedException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    public void downloadWebArchive(String repositoryName, String tagName, Path targetZip) throws IOException, InterruptedException {
        URI uri = URI.create(buildWebArchiveUrl(repositoryName, tagName));
        HttpResponse<InputStream> response = sendInputStreamWithRateLimit(() -> {
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                    .timeout(safeTimeout(properties.getRequestTimeout()))
                    .header("Accept", "application/zip,application/octet-stream,*/*")
                    .header("User-Agent", "M2M-XML-EDITOR")
                    .GET();
            if (properties.hasToken()) {
                builder.header("Authorization", "Bearer " + properties.getToken());
            }
            for (Map.Entry<String, String> header : properties.getWebArchiveHeaders().entrySet()) {
                if (StringUtils.hasText(header.getKey()) && header.getValue() != null) {
                    builder.header(header.getKey(), header.getValue());
                }
            }
            return builder.build();
        }, "GitHub web archive download", repositoryName + "/" + tagName);
        copySuccessfulResponse(response, targetZip, "GitHub web archive download", repositoryName, tagName);
    }

    /**
     * A konfigurált archívum URL-sablonba biztonságosan behelyettesíti az organization, repository és tag URL-útvonalszegmenseit.
     *
     * @param repositoryName a GitHub repository neve
     * @param tagName a release tag neve
     * @return a művelet eredménye
     */
    private String buildWebArchiveUrl(String repositoryName, String tagName) {
        String template = StringUtils.hasText(properties.getArchiveUrlTemplate())
                ? properties.getArchiveUrlTemplate()
                : "https://github.com/{owner}/{repo}/archive/refs/tags/{tag}.zip";
        return template
                .replace("{owner}", encodePathSegment(properties.getOrganization()))
                .replace("{repo}", encodePathSegment(repositoryName))
                .replace("{tag}", encodePathSegment(tagName));
    }

    /**
     * Egy organization/repository/tag értéket UTF-8 URL-útvonalszegmenssé kódol úgy, hogy a behelyettesített érték ne tudja megváltoztatni az URL szerkezetét.
     *
     * @param value a feldolgozandó érték
     * @return a művelet eredménye
     */
    private String encodePathSegment(String value) {
        return UriUtils.encodePathSegment(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    /**
     * Csak sikeres HTTP státusz esetén másolja a válasz streamjét a cél ZIP-be. Sikertelen válasz esetén bezárja a streamet és részletes IO hibát képez.
     *
     * @param response a feldolgozandó HTTP válasz
     * @param targetZip a letöltendő ZIP célfájlja
     * @param operation a diagnosztikában szereplő GitHub művelet neve
     * @param repositoryName a GitHub repository neve
     * @param tagName a release tag neve
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private void copySuccessfulResponse(HttpResponse<InputStream> response, Path targetZip, String operation, String repositoryName, String tagName) throws IOException {
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            closeQuietly(response.body());
            throw new IOException(operation + " failed for " + repositoryName + "/" + tagName + ": HTTP " + status);
        }
        ExceptionSafeOperations.createDirectories(targetZip.getParent());
        try (InputStream input = response.body()) {
            SecureFileOperations.copyPrivate(input, targetZip, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * JSON API-kérést hajt végre, ellenőrzi a HTTP státuszt, majd Jackson fára alakítja a választ.
     *
     * @param uri a művelethez átadott {@code uri} érték
     * @return a művelet eredménye
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     * @throws InterruptedException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private JsonNode sendJson(URI uri) throws IOException, InterruptedException {
        HttpResponse<String> response = sendStringWithRateLimit(() -> baseApiRequest(uri)
                .timeout(safeTimeout(properties.getRequestTimeout()))
                .GET()
                .build(), "GitHub API request", uri.toString());
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new IOException("GitHub API request failed: " + uri + " HTTP " + status + " body=" + response.body());
        }
        return objectMapper.readTree(response.body());
    }

    /**
     * Közös GitHub API request buildert készít Accept/User-Agent fejlécekkel, timeouttal és — ha konfigurálva van — Bearer tokenes Authorization fejléccel.
     *
     * @param uri a művelethez átadott {@code uri} érték
     * @return a művelet eredménye
     */
    private HttpRequest.Builder baseApiRequest(URI uri) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "M2M-XML-EDITOR");
        if (properties.hasToken()) {
            builder.header("Authorization", "Bearer " + properties.getToken());
        }
        return builder;
    }

    /**
     * Szöveges HTTP választ kér le rate-limit tudatos újrapróbálással. Az újrapróbálási ciklust konfigurált maximális kísérletszám és várakozási plafon korlátozza.
     *
     * @param requestFactory a művelethez átadott {@code requestFactory} érték
     * @param operation a diagnosztikában szereplő GitHub művelet neve
     * @param subject a művelet tárgya
     * @return a művelet eredménye
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     * @throws InterruptedException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private HttpResponse<String> sendStringWithRateLimit(Supplier<HttpRequest> requestFactory,
                                                         String operation,
                                                         String subject) throws IOException, InterruptedException {
        int maxAttempts = maxAttempts();
        HttpResponse<String> lastResponse = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            HttpRequest request = requestFactory.get();
            try {
                GitHubProxySettings settings = networkSettingsProvider.load();
                lastResponse = sendStringWithAutomaticProxyAuthentication(request, settings);
            } catch (IOException ex) {
                throw detailedTransportException(ex, request, operation, subject, attempt, maxAttempts);
            }
            logStringResponseDiagnostics(lastResponse, operation, subject);
            if (!shouldRetryAfterRateLimit(lastResponse, attempt, maxAttempts, operation, subject)) {
                return lastResponse;
            }
        }
        return lastResponse;
    }

    /**
     * Streamelt HTTP választ kér le rate-limit tudatos újrapróbálással; újrapróbálás előtt az előző válasz streamjét lezárja.
     *
     * @param requestFactory a művelethez átadott {@code requestFactory} érték
     * @param operation a diagnosztikában szereplő GitHub művelet neve
     * @param subject a művelet tárgya
     * @return a művelet eredménye
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     * @throws InterruptedException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private HttpResponse<InputStream> sendInputStreamWithRateLimit(Supplier<HttpRequest> requestFactory,
                                                                   String operation,
                                                                   String subject) throws IOException, InterruptedException {
        int maxAttempts = maxAttempts();
        HttpResponse<InputStream> lastResponse = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            HttpRequest request = requestFactory.get();
            try {
                GitHubProxySettings settings = networkSettingsProvider.load();
                lastResponse = sendStreamWithAutomaticProxyAuthentication(request, settings);
            } catch (IOException ex) {
                throw detailedTransportException(ex, request, operation, subject, attempt, maxAttempts);
            }
            logResponseHeaders(lastResponse, operation, subject);
            if (!shouldRetryAfterRateLimit(lastResponse, attempt, maxAttempts, operation, subject)) {
                return lastResponse;
            }
            closeQuietly(lastResponse.body());
        }
        return lastResponse;
    }

    /**
     * A proxyhitelesítést automatikusan kezeli. Elsőként a platformfüggetlen JDK kliens fut.
     * Csak HTTP 407 válasz esetén olvassa ki a proxy által meghirdetett hitelesítési sémákat,
     * majd egyszer próbálkozik explicit felhasználónév/jelszó alapú NTLM/Digest/Basic hitelesítéssel,
     * végül Windows alatt egyszer az aktuális Windows biztonsági kontextussal Negotiate/NTLM módban.
     * Egyetlen hitelesítési út sem ismétlődik korlátlanul.
     */
    private HttpResponse<String> sendStringWithAutomaticProxyAuthentication(HttpRequest request,
                                                                             GitHubProxySettings settings)
            throws IOException, InterruptedException {
        HttpResponse<String> response;
        Optional<ProxyAuthCacheEntry> cached = cachedProxyAuth(settings);
        if (cached.isPresent()) {
            response = sendStringWithCachedProxyAuthentication(request, settings, cached.get());
            if (response.statusCode() != 407) {
                return response;
            }
            invalidateProxyAuthCache(cached.get(), settings);
            LOGGER.info("A korábban bevált GitHub proxy-auth stratégia ismét 407 választ kapott; az auth capability újrafelderítése következik.");
        } else {
            response = buildHttpClient(settings).send(
                    request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 407 || !isProxyEnabled(settings)) {
                if (isProxyEnabled(settings)) {
                    rememberProxyAuth(settings, ProxyAuthStrategy.JDK, ProxyAuthCapabilities.none());
                }
                return response;
            }
        }

        ProxyAuthCapabilities capabilities = proxyAuthCapabilities(response.headers());
        logProxyAuthenticationChallenge(settings, capabilities);

        if (hasExplicitProxyCredentials(settings) && capabilities.supportsExplicitCredentials()) {
            HttpResponse<String> explicitResponse = sendExplicitProxyString(request, settings, capabilities);
            if (explicitResponse.statusCode() != 407) {
                rememberProxyAuth(settings, ProxyAuthStrategy.EXPLICIT, capabilities);
                return explicitResponse;
            }
            response = explicitResponse;
            LOGGER.warn("A proxy elutasította a konfigurált felhasználónév/jelszó alapú hitelesítést; Windows-integrált fallback vizsgálata következik.");
        }

        if (capabilities.supportsWindowsIntegrated() && canUseWindowsIntegratedProxy()) {
            HttpResponse<String> windowsResponse = sendWindowsIntegratedString(request, settings, capabilities);
            if (windowsResponse.statusCode() != 407) {
                rememberProxyAuth(settings, ProxyAuthStrategy.WINDOWS_INTEGRATED, capabilities);
                return windowsResponse;
            }
            response = windowsResponse;
        }
        return response;
    }

    /**
     * Az automatikus proxyhitelesítés streamelt válaszokra alkalmazott változata.
     */
    private HttpResponse<InputStream> sendStreamWithAutomaticProxyAuthentication(HttpRequest request,
                                                                                  GitHubProxySettings settings)
            throws IOException, InterruptedException {
        HttpResponse<InputStream> response;
        Optional<ProxyAuthCacheEntry> cached = cachedProxyAuth(settings);
        if (cached.isPresent()) {
            response = sendStreamWithCachedProxyAuthentication(request, settings, cached.get());
            if (response.statusCode() != 407) {
                return response;
            }
            ProxyAuthCapabilities refreshedCapabilities = proxyAuthCapabilities(response.headers());
            closeQuietly(response.body());
            invalidateProxyAuthCache(cached.get(), settings);
            LOGGER.info("A korábban bevált GitHub proxy-auth stratégia ismét 407 választ kapott; az auth capability újrafelderítése következik.");
            response = new SimpleHttpResponse<>(request, 407, response.headers(), InputStream.nullInputStream());
            if (!refreshedCapabilities.isEmpty()) {
                return retryStreamAfterProxyChallenge(request, settings, response, refreshedCapabilities);
            }
        } else {
            response = buildHttpClient(settings).send(
                    request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 407 || !isProxyEnabled(settings)) {
                if (isProxyEnabled(settings)) {
                    rememberProxyAuth(settings, ProxyAuthStrategy.JDK, ProxyAuthCapabilities.none());
                }
                return response;
            }
        }

        ProxyAuthCapabilities capabilities = proxyAuthCapabilities(response.headers());
        closeQuietly(response.body());
        return retryStreamAfterProxyChallenge(request, settings, response, capabilities);
    }

    private HttpResponse<InputStream> retryStreamAfterProxyChallenge(HttpRequest request,
                                                                      GitHubProxySettings settings,
                                                                      HttpResponse<InputStream> response,
                                                                      ProxyAuthCapabilities capabilities)
            throws IOException {
        logProxyAuthenticationChallenge(settings, capabilities);

        boolean tryExplicit = hasExplicitProxyCredentials(settings) && capabilities.supportsExplicitCredentials();
        boolean tryWindows = capabilities.supportsWindowsIntegrated() && canUseWindowsIntegratedProxy();
        if (!tryExplicit && !tryWindows) {
            return response;
        }

        if (tryExplicit) {
            HttpResponse<InputStream> explicitResponse = sendExplicitProxyStream(request, settings, capabilities);
            if (explicitResponse.statusCode() != 407) {
                rememberProxyAuth(settings, ProxyAuthStrategy.EXPLICIT, capabilities);
                return explicitResponse;
            }
            response = explicitResponse;
            if (!tryWindows) {
                return response;
            }
            closeQuietly(explicitResponse.body());
            LOGGER.warn("A proxy elutasította a konfigurált felhasználónév/jelszó alapú hitelesítést; Windows-integrált fallback vizsgálata következik.");
        }

        HttpResponse<InputStream> windowsResponse = sendWindowsIntegratedStream(request, settings, capabilities);
        if (windowsResponse.statusCode() != 407) {
            rememberProxyAuth(settings, ProxyAuthStrategy.WINDOWS_INTEGRATED, capabilities);
        }
        return windowsResponse;
    }

    private HttpResponse<String> sendStringWithCachedProxyAuthentication(HttpRequest request,
                                                                          GitHubProxySettings settings,
                                                                          ProxyAuthCacheEntry cached)
            throws IOException, InterruptedException {
        LOGGER.debug("GitHub proxy-auth cache találat: {}:{} stratégia={}",
                cached.key().host(), cached.key().port(), cached.strategy());
        return switch (cached.strategy()) {
            case JDK -> buildHttpClient(settings).send(
                    request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            case EXPLICIT -> sendExplicitProxyString(request, settings, cached.capabilities());
            case WINDOWS_INTEGRATED -> sendWindowsIntegratedString(request, settings, cached.capabilities());
        };
    }

    private HttpResponse<InputStream> sendStreamWithCachedProxyAuthentication(HttpRequest request,
                                                                               GitHubProxySettings settings,
                                                                               ProxyAuthCacheEntry cached)
            throws IOException, InterruptedException {
        LOGGER.debug("GitHub proxy-auth cache találat: {}:{} stratégia={}",
                cached.key().host(), cached.key().port(), cached.strategy());
        return switch (cached.strategy()) {
            case JDK -> buildHttpClient(settings).send(
                    request, HttpResponse.BodyHandlers.ofInputStream());
            case EXPLICIT -> sendExplicitProxyStream(request, settings, cached.capabilities());
            case WINDOWS_INTEGRATED -> sendWindowsIntegratedStream(request, settings, cached.capabilities());
        };
    }

    private Optional<ProxyAuthCacheEntry> cachedProxyAuth(GitHubProxySettings settings) {
        if (!isProxyEnabled(settings)) {
            return Optional.empty();
        }
        ProxyAuthCacheKey key = proxyAuthCacheKey(settings);
        ProxyAuthCacheEntry entry = proxyAuthCache.get();
        if (entry == null || !entry.key().equals(key)) {
            return Optional.empty();
        }
        return Optional.of(entry);
    }

    private void rememberProxyAuth(GitHubProxySettings settings,
                                   ProxyAuthStrategy strategy,
                                   ProxyAuthCapabilities capabilities) {
        if (!isProxyEnabled(settings)) {
            return;
        }
        ProxyAuthCacheEntry entry = new ProxyAuthCacheEntry(
                proxyAuthCacheKey(settings), strategy, capabilities);
        proxyAuthCache.set(entry);
        LOGGER.info("GitHub proxy-auth stratégia megjegyezve ehhez a futáshoz: stratégia={}", strategy);
        LOGGER.debug("GitHub proxy-auth cache részletek: host={}, port={}, username={}, stratégia={}",
                entry.key().host(), entry.key().port(), entry.key().username(), strategy);
    }

    private void invalidateProxyAuthCache(ProxyAuthCacheEntry expected, GitHubProxySettings settings) {
        proxyAuthCache.compareAndSet(expected, null);
        LOGGER.debug("GitHub proxy-auth cache törölve: {}:{}",
                normalizeProxyHost(settings.getProxyUrl()), settings.getProxyPort());
    }

    private ProxyAuthCacheKey proxyAuthCacheKey(GitHubProxySettings settings) {
        return new ProxyAuthCacheKey(
                normalizeProxyHost(settings.getProxyUrl()),
                settings.getProxyPort() == null ? 0 : settings.getProxyPort(),
                StringUtils.hasText(settings.getUsername()) ? settings.getUsername().trim() : "",
                hasExplicitProxyCredentials(settings));
    }

    private boolean isProxyEnabled(GitHubProxySettings settings) {
        return settings != null
                && settings.isEnabled()
                && StringUtils.hasText(settings.getProxyUrl())
                && settings.getProxyPort() != null
                && settings.getProxyPort() > 0;
    }

    private boolean hasExplicitProxyCredentials(GitHubProxySettings settings) {
        return settings != null
                && StringUtils.hasText(settings.getUsername())
                && StringUtils.hasText(settings.getPassword());
    }

    /**
     * A HTTP 407 válasz Proxy-Authenticate fejléceiből meghatározza a ténylegesen támogatott sémákat.
     */
    private ProxyAuthCapabilities proxyAuthCapabilities(HttpHeaders headers) {
        String combined = String.join(",", headers.allValues("Proxy-Authenticate"))
                .toLowerCase(java.util.Locale.ROOT);
        return new ProxyAuthCapabilities(
                combined.contains("negotiate"),
                combined.contains("ntlm"),
                combined.contains("digest"),
                combined.contains("basic"));
    }

    private void logProxyAuthenticationChallenge(GitHubProxySettings settings,
                                                  ProxyAuthCapabilities capabilities) {
        String host = normalizeProxyHost(settings.getProxyUrl());
        LOGGER.info("GitHub proxy HTTP 407 választ adott; támogatott auth sémák: {}. Automatikus hitelesítés indul.",
                capabilities.summary());
        LOGGER.debug("GitHub proxy 407 részletek: host={}, port={}", host, settings.getProxyPort());
    }

    /**
     * Explicit proxy credentialdel használható Apache HTTP klienst épít. A kliens csak a proxy által
     * meghirdetett NTLM, Digest és Basic sémák közül próbálkozik, ebben a sorrendben.
     */
    private CloseableHttpClient buildExplicitProxyHttpClient(GitHubProxySettings settings,
                                                               ProxyAuthCapabilities capabilities) throws Exception {
        String host = normalizeProxyHost(settings.getProxyUrl());
        int port = settings.getProxyPort() == null ? 0 : settings.getProxyPort();
        if (!StringUtils.hasText(host) || port <= 0) {
            throw new IOException("Érvénytelen proxy host vagy port az explicit GitHub proxyhitelesítéshez.");
        }

        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        UsernamePasswordCredentials userPassword = new UsernamePasswordCredentials(
                settings.getUsername(), settings.getPassword());
        if (capabilities.digest()) {
            credentialsProvider.setCredentials(
                    new AuthScope(host, port, AuthScope.ANY_REALM, AuthSchemes.DIGEST), userPassword);
        }
        if (capabilities.basic()) {
            credentialsProvider.setCredentials(
                    new AuthScope(host, port, AuthScope.ANY_REALM, AuthSchemes.BASIC), userPassword);
        }
        if (capabilities.ntlm()) {
            credentialsProvider.setCredentials(
                    new AuthScope(host, port, AuthScope.ANY_REALM, AuthSchemes.NTLM), ntCredentials(settings));
        }

        List<String> preferredSchemes = new ArrayList<>();
        if (capabilities.ntlm()) preferredSchemes.add(AuthSchemes.NTLM);
        if (capabilities.digest()) preferredSchemes.add(AuthSchemes.DIGEST);
        if (capabilities.basic()) preferredSchemes.add(AuthSchemes.BASIC);

        int timeoutMs = durationToMillis(safeTimeout(properties.getRequestTimeout()));
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(timeoutMs)
                .setConnectionRequestTimeout(timeoutMs)
                .setSocketTimeout(timeoutMs)
                .setProxyPreferredAuthSchemes(preferredSchemes)
                .build();

        org.apache.http.impl.client.HttpClientBuilder builder = HttpClients.custom()
                .setProxy(new HttpHost(host, port, "http"))
                .setDefaultCredentialsProvider(credentialsProvider)
                .setDefaultRequestConfig(requestConfig)
                .disableAutomaticRetries();

        SSLContext sslContext = buildSslContext(settings);
        if (sslContext != null) {
            builder.setSSLContext(sslContext);
        }
        LOGGER.info("GitHub proxy explicit credential próbálkozás indul; sémák={}", preferredSchemes);
        LOGGER.debug("GitHub proxy explicit credential részletek: host={}, port={}, username={}",
                host, port, settings.getUsername());
        return builder.build();
    }

    private NTCredentials ntCredentials(GitHubProxySettings settings) {
        String configured = settings.getUsername().trim();
        String user = configured;
        String domain = null;
        int slash = configured.indexOf('\\');
        if (slash > 0 && slash < configured.length() - 1) {
            domain = configured.substring(0, slash);
            user = configured.substring(slash + 1);
        } else {
            int at = configured.lastIndexOf('@');
            if (at > 0 && at < configured.length() - 1) {
                user = configured.substring(0, at);
                domain = configured.substring(at + 1);
            }
        }
        String workstation = System.getenv("COMPUTERNAME");
        return new NTCredentials(user, settings.getPassword(),
                StringUtils.hasText(workstation) ? workstation : null, domain);
    }

    private HttpResponse<String> sendExplicitProxyString(HttpRequest request,
                                                          GitHubProxySettings settings,
                                                          ProxyAuthCapabilities capabilities) throws IOException {
        try (CloseableHttpClient client = buildExplicitProxyHttpClient(settings, capabilities);
             CloseableHttpResponse response = executeApacheRequest(client, request)) {
            String body = response.getEntity() == null
                    ? ""
                    : EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            return new SimpleHttpResponse<>(request, response.getStatusLine().getStatusCode(),
                    toJdkHeaders(response.getAllHeaders()), body);
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException("Explicit GitHub proxyhitelesítés sikertelen: " + ex.getMessage(), ex);
        }
    }

    private HttpResponse<InputStream> sendExplicitProxyStream(HttpRequest request,
                                                               GitHubProxySettings settings,
                                                               ProxyAuthCapabilities capabilities) throws IOException {
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        try {
            client = buildExplicitProxyHttpClient(settings, capabilities);
            response = executeApacheRequest(client, request);
            InputStream raw = response.getEntity() == null
                    ? InputStream.nullInputStream()
                    : response.getEntity().getContent();
            InputStream body = new ManagedApacheInputStream(raw, response, client);
            return new SimpleHttpResponse<>(request, response.getStatusLine().getStatusCode(),
                    toJdkHeaders(response.getAllHeaders()), body);
        } catch (Exception ex) {
            if (response != null) {
                try { response.close(); } catch (IOException ignored) { }
            }
            if (client != null) {
                try { client.close(); } catch (IOException ignored) { }
            }
            if (ex instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("Explicit GitHub proxyhitelesítés sikertelen: " + ex.getMessage(), ex);
        }
    }

    private boolean canUseWindowsIntegratedProxy() {
        String osName = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);
        if (!osName.contains("win")) {
            return false;
        }
        try {
            return WinHttpClients.isWinAuthAvailable();
        } catch (LinkageError ex) {
            LOGGER.error("A Windows SSPI proxyhitelesítéshez szükséges JNA/httpclient-win binárisan nem kompatibilis: {}",
                    ex.toString());
            return false;
        }
    }

    /**
     * Windows SSPI támogatású HTTP klienst készít. Ezt csak akkor használjuk, ha a proxy 407 válaszában
     * Negotiate vagy NTLM sémát hirdetett meg és az explicit credential próbálkozás nem oldotta meg a kapcsolatot.
     * A kliens az aktuális Windows process/service identitását használja, interaktív Kerberos prompt nélkül.
     */
    private CloseableHttpClient buildWindowsIntegratedHttpClient(GitHubProxySettings settings,
                                                                   ProxyAuthCapabilities capabilities) throws Exception {
        String host = normalizeProxyHost(settings.getProxyUrl());
        int port = settings.getProxyPort() == null ? 0 : settings.getProxyPort();
        if (!StringUtils.hasText(host) || port <= 0) {
            throw new IOException("Érvénytelen proxy host vagy port a Windows-integrált GitHub kapcsolathoz.");
        }

        BasicCredentialsProvider fallbackCredentials = new BasicCredentialsProvider();
        if (hasExplicitProxyCredentials(settings)) {
            fallbackCredentials.setCredentials(
                    new AuthScope(host, port),
                    new UsernamePasswordCredentials(settings.getUsername(), settings.getPassword()));
        }
        WindowsCredentialsProvider credentialsProvider = new WindowsCredentialsProvider(fallbackCredentials);

        List<String> preferredSchemes = new ArrayList<>();
        if (capabilities.negotiate()) preferredSchemes.add(AuthSchemes.SPNEGO);
        if (capabilities.ntlm()) preferredSchemes.add(AuthSchemes.NTLM);

        int timeoutMs = durationToMillis(safeTimeout(properties.getRequestTimeout()));
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(timeoutMs)
                .setConnectionRequestTimeout(timeoutMs)
                .setSocketTimeout(timeoutMs)
                .setProxyPreferredAuthSchemes(preferredSchemes)
                .build();

        org.apache.http.impl.client.HttpClientBuilder builder = WinHttpClients.custom()
                .setProxy(new HttpHost(host, port, "http"))
                .setDefaultCredentialsProvider(credentialsProvider)
                .setDefaultRequestConfig(requestConfig)
                .disableAutomaticRetries();

        SSLContext sslContext = buildSslContext(settings);
        if (sslContext != null) {
            builder.setSSLContext(sslContext);
        }

        LOGGER.info("GitHub proxy Windows-integrált fallback indul; sémák={}; process/service Windows credential használatával.",
                preferredSchemes);
        LOGGER.debug("GitHub proxy Windows-integrált fallback részletek: host={}, port={}", host, port);
        return builder.build();
    }

    private HttpResponse<String> sendWindowsIntegratedString(HttpRequest request,
                                                              GitHubProxySettings settings,
                                                              ProxyAuthCapabilities capabilities) throws IOException {
        try (CloseableHttpClient client = buildWindowsIntegratedHttpClient(settings, capabilities);
             CloseableHttpResponse response = executeApacheRequest(client, request)) {
            String body = response.getEntity() == null
                    ? ""
                    : EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            return new SimpleHttpResponse<>(request, response.getStatusLine().getStatusCode(),
                    toJdkHeaders(response.getAllHeaders()), body);
        } catch (LinkageError ex) {
            throw new IOException("Windows SSPI/JNA proxyhitelesítés bináris kompatibilitási hibába futott: "
                    + ex.getMessage(), ex);
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException("Windows SSPI GitHub proxykapcsolat sikertelen: " + ex.getMessage(), ex);
        }
    }

    private HttpResponse<InputStream> sendWindowsIntegratedStream(HttpRequest request,
                                                                   GitHubProxySettings settings,
                                                                   ProxyAuthCapabilities capabilities) throws IOException {
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        try {
            client = buildWindowsIntegratedHttpClient(settings, capabilities);
            response = executeApacheRequest(client, request);
            InputStream raw = response.getEntity() == null
                    ? InputStream.nullInputStream()
                    : response.getEntity().getContent();
            InputStream body = new ManagedApacheInputStream(raw, response, client);
            return new SimpleHttpResponse<>(request, response.getStatusLine().getStatusCode(),
                    toJdkHeaders(response.getAllHeaders()), body);
        } catch (LinkageError ex) {
            if (response != null) {
                try { response.close(); } catch (IOException ignored) { }
            }
            if (client != null) {
                try { client.close(); } catch (IOException ignored) { }
            }
            throw new IOException("Windows SSPI/JNA proxyhitelesítés bináris kompatibilitási hibába futott: "
                    + ex.getMessage(), ex);
        } catch (Exception ex) {
            if (response != null) {
                try { response.close(); } catch (IOException ignored) { }
            }
            if (client != null) {
                try { client.close(); } catch (IOException ignored) { }
            }
            if (ex instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("Windows SSPI GitHub proxykapcsolat sikertelen: " + ex.getMessage(), ex);
        }
    }

    private CloseableHttpResponse executeApacheRequest(CloseableHttpClient client, HttpRequest request) throws IOException {
        HttpGet get = new HttpGet(request.uri());
        request.headers().map().forEach((name, values) -> {
            for (String value : values) {
                get.addHeader(name, value);
            }
        });
        return client.execute(get);
    }

    private HttpHeaders toJdkHeaders(Header[] apacheHeaders) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        if (apacheHeaders != null) {
            for (Header header : apacheHeaders) {
                headers.computeIfAbsent(header.getName(), ignored -> new ArrayList<>()).add(header.getValue());
            }
        }
        return HttpHeaders.of(headers, (name, value) -> true);
    }

    private record ProxyAuthCapabilities(boolean negotiate, boolean ntlm, boolean digest, boolean basic) {
        static ProxyAuthCapabilities none() {
            return new ProxyAuthCapabilities(false, false, false, false);
        }

        boolean isEmpty() {
            return !negotiate && !ntlm && !digest && !basic;
        }

        boolean supportsExplicitCredentials() {
            return ntlm || digest || basic;
        }

        boolean supportsWindowsIntegrated() {
            return negotiate || ntlm;
        }

        String summary() {
            List<String> schemes = new ArrayList<>();
            if (negotiate) schemes.add("Negotiate");
            if (ntlm) schemes.add("NTLM");
            if (digest) schemes.add("Digest");
            if (basic) schemes.add("Basic");
            return schemes.isEmpty() ? "ismeretlen/nincs" : String.join(", ", schemes);
        }
    }

    private int durationToMillis(Duration duration) {
        long millis = duration == null ? 60_000L : duration.toMillis();
        if (millis <= 0L) return 60_000;
        return (int) Math.min(Integer.MAX_VALUE, millis);
    }

    private enum ProxyAuthStrategy {
        JDK,
        EXPLICIT,
        WINDOWS_INTEGRATED
    }

    private record ProxyAuthCacheKey(String host, int port, String username, boolean explicitCredentials) { }

    private record ProxyAuthCacheEntry(ProxyAuthCacheKey key,
                                       ProxyAuthStrategy strategy,
                                       ProxyAuthCapabilities capabilities) { }

    private static final class ManagedApacheInputStream extends FilterInputStream {
        private final CloseableHttpResponse response;
        private final CloseableHttpClient client;
        private boolean closed;

        private ManagedApacheInputStream(InputStream input, CloseableHttpResponse response, CloseableHttpClient client) {
            super(input);
            this.response = response;
            this.client = client;
        }

        @Override
        public void close() throws IOException {
            if (closed) return;
            closed = true;
            IOException failure = null;
            try {
                super.close();
            } catch (IOException ex) {
                failure = ex;
            }
            try {
                response.close();
            } catch (IOException ex) {
                if (failure == null) failure = ex;
            }
            try {
                client.close();
            } catch (IOException ex) {
                if (failure == null) failure = ex;
            }
            if (failure != null) throw failure;
        }
    }

    private static final class SimpleHttpResponse<T> implements HttpResponse<T> {
        private final HttpRequest request;
        private final int statusCode;
        private final HttpHeaders headers;
        private final T body;

        private SimpleHttpResponse(HttpRequest request, int statusCode, HttpHeaders headers, T body) {
            this.request = request;
            this.statusCode = statusCode;
            this.headers = headers;
            this.body = body;
        }

        @Override public int statusCode() { return statusCode; }
        @Override public HttpRequest request() { return request; }
        @Override public Optional<HttpResponse<T>> previousResponse() { return Optional.empty(); }
        @Override public HttpHeaders headers() { return headers; }
        @Override public T body() { return body; }
        @Override public Optional<SSLSession> sslSession() { return Optional.empty(); }
        @Override public URI uri() { return request.uri(); }
        @Override public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
    }


    /**
     * A hálózati kivételt a művelet, cél, próbálkozásszám, URI, proxyállapot és ok-lánc alapján diagnosztikailag részletesebb {@link IOException} példánnyá alakítja anélkül, hogy titkokat naplózna.
     *
     * @param exception a művelethez átadott {@code exception} érték
     * @param request a végrehajtandó frissítés vagy letöltés paraméterei
     * @param operation a diagnosztikában szereplő GitHub művelet neve
     * @param subject a művelet tárgya
     * @param attempt a művelethez átadott {@code attempt} érték
     * @param maxAttempts a művelethez átadott {@code maxAttempts} érték
     * @return a művelet eredménye
     */
    private IOException detailedTransportException(IOException exception,
                                                   HttpRequest request,
                                                   String operation,
                                                   String subject,
                                                   int attempt,
                                                   int maxAttempts) {
        String message = "GitHub kapcsolat sikertelen: operation=" + sanitizeForLog(operation)
                + ", attempt=" + attempt + "/" + maxAttempts
                + ", method=" + sanitizeForLog(request.method())
                + ", remote service unavailable";
        LOGGER.error("GitHub kapcsolat sikertelen: operation={}, attempt={}/{}, method={}",
                sanitizeForLog(operation), attempt, maxAttempts, sanitizeForLog(request.method()));
        return GitHubTransportExceptionFactory.create(message);
    }

    /**
     * Naplózható, titokmentes összefoglalót készít az aktuális proxy/TLS beállításokról.
     *
     * @param settings az aktuális proxy/TLS beállítások
     * @return a művelet eredménye
     */
    private String proxySnapshot(GitHubProxySettings settings) {
        if (settings == null) {
            return "proxySource=SYSTEM_CONFIGURATION/SYSTEM_SECRET, proxySettings=null";
        }
        String rawHost = settings.getProxyUrl() == null ? "" : settings.getProxyUrl().trim();
        String normalizedHost = normalizeProxyHost(rawHost);
        boolean usernameConfigured = StringUtils.hasText(settings.getUsername());
        boolean passwordConfigured = StringUtils.hasText(settings.getPassword());
        return "proxySource=SYSTEM_CONFIGURATION/SYSTEM_SECRET"
                + ", proxyEnabled=" + settings.isEnabled()
                + ", proxyHostRaw=" + sanitizeForLog(rawHost)
                + ", proxyHostNormalized=" + sanitizeForLog(normalizedHost)
                + ", proxyPort=" + (settings.getProxyPort() == null ? "" : settings.getProxyPort())
                + ", proxyUsernameConfigured=" + usernameConfigured
                + ", proxyPasswordConfigured=" + passwordConfigured
                + ", sslVerificationDisabled=" + settings.isSslVerificationDisabled()
                + ", trustStoreConfigured=" + StringUtils.hasText(settings.getTrustStorePath());
    }

    /**
     * Egy kivételláncot rövid, egysoros diagnosztikai szöveggé alakít, véges mélységgel.
     *
     * @param throwable a művelethez átadott {@code throwable} érték
     * @return a művelet eredménye
     */
    private String causeChain(Throwable throwable) {
        StringBuilder result = new StringBuilder();
        Throwable current = throwable;
        int depth = 0;
        while (current != null && depth < 12) {
            if (depth > 0) {
                result.append(" -> ");
            }
            result.append(current.getClass().getName());
            if (StringUtils.hasText(current.getMessage())) {
                result.append(": ").append(current.getMessage().replace('\n', ' ').replace('\r', ' '));
            }
            if (current.getCause() == current) {
                break;
            }
            current = current.getCause();
            depth++;
        }
        return result.toString();
    }

    /**
     * Eltávolítja a vezérlő karaktereket és a potenciálisan érzékeny/veszélyes sortöréseket a naplózásra szánt értékből.
     *
     * @param value a feldolgozandó érték
     * @return a művelet eredménye
     */
    private String sanitizeForLog(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\n', ' ').replace('\r', ' ').trim();
    }

    /**
     * Diagnosztikai információt naplóz a GitHub kommunikációról úgy, hogy a titkos konfigurációs értékek ne kerüljenek a naplóba.
     *
     * @param response a feldolgozandó HTTP válasz
     * @param operation a diagnosztikában szereplő GitHub művelet neve
     * @param subject a művelet tárgya
     */
    private void logStringResponseDiagnostics(HttpResponse<String> response,
                                              String operation,
                                              String subject) {
        logResponseHeaders(response, operation, subject);
        if (response.statusCode() >= 400) {
            LOGGER.warn("GitHub error response for {} [{}], HTTP {}: body={}",
                    operation,
                    subject,
                    response.statusCode(),
                    abbreviateForLog(response.body(), 2000));
        }
    }

    /**
     * Diagnosztikai információt naplóz a GitHub kommunikációról úgy, hogy a titkos konfigurációs értékek ne kerüljenek a naplóba.
     *
     * @param response a feldolgozandó HTTP válasz
     * @param operation a diagnosztikában szereplő GitHub művelet neve
     * @param subject a művelet tárgya
     */
    private void logResponseHeaders(HttpResponse<?> response,
                                    String operation,
                                    String subject) {
        HttpHeaders headers = response.headers();
        LOGGER.info("GitHub response diagnostics for {} [{}], HTTP {}: limit={}, remaining={}, used={}, reset={}, resource={}, retryAfter={}, tokenExpiration={}, requestId={}",
                operation,
                subject,
                response.statusCode(),
                headers.firstValue("x-ratelimit-limit").orElse(""),
                headers.firstValue("x-ratelimit-remaining").orElse(""),
                headers.firstValue("x-ratelimit-used").orElse(""),
                headers.firstValue("x-ratelimit-reset").orElse(""),
                headers.firstValue("x-ratelimit-resource").orElse(""),
                headers.firstValue("retry-after").orElse(""),
                headers.firstValue("github-authentication-token-expiration").orElse(""),
                headers.firstValue("x-github-request-id").orElse(""));
        if (response.statusCode() == 407) {
            LOGGER.warn("A proxy hitelesítést kér a GitHub kapcsolathoz. Proxy-Authenticate={}",
                    sanitizeForLog(headers.firstValue("proxy-authenticate").orElse("")));
        }
    }

    /**
     * A diagnosztikai naplóba kerülő szöveget előbb megtisztítja, majd a megadott maximális hosszra csonkolja, hogy nagy HTTP-válasz ne növelje korlátlanul a naplót.
     *
     * @param value a feldolgozandó érték
     * @param maxLength a megengedett maximális hossz
     * @return a művelet eredménye
     */
    private String abbreviateForLog(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace('\r', ' ').replace('\n', ' ');
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    /**
     * Megállapítja, hogy a GitHub válasz rate-limit miatt újrapróbálható-e, kiszámítja a várakozást, szükség esetén naplózza a fejléceket, majd megszakítható várakozást végez.
     *
     * @param response a feldolgozandó HTTP válasz
     * @param attempt a művelethez átadott {@code attempt} érték
     * @param maxAttempts a művelethez átadott {@code maxAttempts} érték
     * @param operation a diagnosztikában szereplő GitHub művelet neve
     * @param subject a művelet tárgya
     * @return a művelet eredménye
     * @throws InterruptedException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private boolean shouldRetryAfterRateLimit(HttpResponse<?> response,
                                             int attempt,
                                             int maxAttempts,
                                             String operation,
                                             String subject) throws InterruptedException {
        if (!properties.isRateLimitEnabled()) {
            return false;
        }
        printRateLimitHeaders(response, operation, subject);
        if (attempt >= maxAttempts) {
            return false;
        }
        Optional<Duration> wait = primaryRateLimitWait(response);
        String reason = "primary rate limit";
        if (wait.isEmpty()) {
            wait = secondaryRateLimitWait(response);
            reason = "secondary rate limit";
        }
        if (wait.isEmpty()) {
            return false;
        }
        Duration capped = capWait(wait.get());
        LOGGER.warn("GitHub {} detected during {} [{}]. HTTP {}. Retrying {}/{} after {} seconds.",
                reason,
                operation,
                subject,
                response.statusCode(),
                attempt,
                maxAttempts,
                capped.toSeconds());
        sleep(capped);
        return true;
    }

    /**
     * Az elsődleges GitHub rate-limit fejlécek alapján számít várakozási időt, ha a maradék kvóta elfogyott.
     *
     * @param response a feldolgozandó HTTP válasz
     * @return a művelet eredménye
     */
    private Optional<Duration> primaryRateLimitWait(HttpResponse<?> response) {
        if (response.statusCode() != 403) {
            return Optional.empty();
        }
        if (!"0".equals(response.headers().firstValue("x-ratelimit-remaining").orElse("1"))) {
            return Optional.empty();
        }
        return response.headers().firstValue("x-ratelimit-reset").flatMap(value -> {
            try {
                long resetEpochSeconds = Long.parseLong(value.trim());
                long seconds = Math.max(1, resetEpochSeconds - Instant.now().getEpochSecond() + 2);
                return Optional.of(Duration.ofSeconds(seconds));
            } catch (NumberFormatException ex) {
                return Optional.empty();
            }
        });
    }

    /**
     * Másodlagos rate-limit esetén a Retry-After fejlécet, ennek hiányában a konfigurált alapértelmezett várakozást használja.
     *
     * @param response a feldolgozandó HTTP válasz
     * @return a művelet eredménye
     */
    private Optional<Duration> secondaryRateLimitWait(HttpResponse<?> response) {
        int status = response.statusCode();
        Optional<Duration> retryAfter = retryAfter(response.headers());
        if (retryAfter.isPresent()) {
            return retryAfter;
        }
        if (status == 429) {
            return Optional.of(safeDuration(properties.getRateLimitDefaultSecondaryWait(), Duration.ofSeconds(60)));
        }
        return Optional.empty();
    }

    /**
     * A Retry-After fejléc másodperc- vagy HTTP-dátum formájából várakozási időt próbál képezni.
     *
     * @param headers a művelethez átadott {@code headers} érték
     * @return a művelet eredménye
     */
    private Optional<Duration> retryAfter(HttpHeaders headers) {
        return headers.firstValue("retry-after").flatMap(value -> {
            try {
                long seconds = Long.parseLong(value.trim());
                return Optional.of(Duration.ofSeconds(Math.max(1, seconds)));
            } catch (NumberFormatException ex) {
                return Optional.empty();
            }
        });
    }

    /**
     * A kiszámított rate-limit várakozást nemnegatív értékre és a konfigurált maximális várakozási időre korlátozza.
     *
     * @param wait a művelethez átadott {@code wait} érték
     * @return a művelet eredménye
     */
    private Duration capWait(Duration wait) {
        Duration positive = safeDuration(wait, Duration.ofSeconds(1));
        Duration max = safeDuration(properties.getRateLimitMaxWait(), Duration.ofMinutes(15));
        return positive.compareTo(max) > 0 ? max : positive;
    }

    /**
     * A konfigurált rate-limit újrapróbálási számhoz hozzáadja az első kísérletet, és legalább egy próbálkozást biztosít.
     *
     * @return a művelet eredménye
     */
    private int maxAttempts() {
        if (!properties.isRateLimitEnabled()) {
            return 1;
        }
        return Math.max(1, properties.getRateLimitMaxRetries());
    }

    /**
     * Diagnosztikai információt naplóz a GitHub kommunikációról úgy, hogy a titkos konfigurációs értékek ne kerüljenek a naplóba.
     *
     * @param response a feldolgozandó HTTP válasz
     * @param operation a diagnosztikában szereplő GitHub művelet neve
     * @param subject a művelet tárgya
     */
    private void printRateLimitHeaders(HttpResponse<?> response, String operation, String subject) {
        if (!properties.isRateLimitPrintHeaders()) {
            return;
        }
        HttpHeaders headers = response.headers();
        LOGGER.info("GitHub rate limit headers for {} [{}], HTTP {}: limit={}, remaining={}, used={}, reset={}, resource={}, retry-after={}",
                operation,
                subject,
                response.statusCode(),
                headers.firstValue("x-ratelimit-limit").orElse(""),
                headers.firstValue("x-ratelimit-remaining").orElse(""),
                headers.firstValue("x-ratelimit-used").orElse(""),
                headers.firstValue("x-ratelimit-reset").orElse(""),
                headers.firstValue("x-ratelimit-resource").orElse(""),
                headers.firstValue("retry-after").orElse(""));
    }

    /**
     * A rate-limit kezeléshez szükséges várakozást megszakítható módon hajtja végre.
     *
     * @param duration a művelethez átadott {@code duration} érték
     * @throws InterruptedException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private void sleep(Duration duration) throws InterruptedException {
        long millis = Math.max(1, duration.toMillis());
        TimeUnit.MILLISECONDS.sleep(millis);
    }

    /**
     * Best-effort módon lezárja a streamet; a lezárás hibája nem írja felül az eredeti hálózati feldolgozás eredményét.
     *
     * @param input a validálandó bemeneti DTO
     */
    private void closeQuietly(InputStream input) {
        if (input == null) {
            return;
        }
        try {
            input.close();
        } catch (IOException ignored) {
            // ignored intentionally
        }
    }

    /**
     * Érvénytelen vagy hiányzó HTTP timeout esetén biztonságos alapértelmezett időtartamot választ.
     *
     * @param configured a konfigurációból érkező időtartam
     * @return a művelet eredménye
     */
    private Duration safeTimeout(Duration configured) {
        return safeDuration(configured, Duration.ofSeconds(60));
    }

    /**
     * Hiányzó vagy negatív időtartam helyett a megadott fallbacket adja vissza.
     *
     * @param configured a konfigurációból érkező időtartam
     * @param fallback hiányzó/érvénytelen értéknél használt alapérték
     * @return a művelet eredménye
     */
    private Duration safeDuration(Duration configured, Duration fallback) {
        if (configured == null || configured.isNegative() || configured.isZero()) {
            return fallback;
        }
        return configured;
    }
}
