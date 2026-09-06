package hu.gov.nav.xsdparsertool.web.githubupdater.service;

import hu.gov.nav.xsdparsertool.core.support.SecureFileOperations;
import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hu.gov.nav.xsdparsertool.web.githubupdater.config.GitHubSchemaDownloadMode;
import hu.gov.nav.xsdparsertool.web.githubupdater.config.GitHubSchemaUpdaterProperties;
import hu.gov.nav.xsdparsertool.web.githubupdater.domain.GitHubProxySettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.Proxy;
import java.net.SocketAddress;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * A GitHub REST API és release-archívumok HTTP elérését megvalósító kliens. Kezeli a proxy/TLS konfigurációt, a tokenes API-kéréseket, a rate-limit miatti újrapróbálást, a diagnosztikai naplózást és a letöltött tartalom fájlba írását.
 */
@Component
public class GitHubApiClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubApiClient.class);

    private final GitHubSchemaUpdaterProperties properties;
    private final ObjectMapper objectMapper;
    private final GitHubProxySettingsService proxySettingsService;

    /**
     * Létrehozza a(z) {@code GitHubApiClient} példányt a működéshez szükséges kezdeti állapottal és függőségekkel.
     *
     * @param properties a művelethez átadott {@code properties} érték
     * @param objectMapper a művelethez átadott {@code objectMapper} érték
     * @param proxySettingsService a művelethez átadott {@code proxySettingsService} érték
     */
    public GitHubApiClient(GitHubSchemaUpdaterProperties properties,
                           ObjectMapper objectMapper,
                           GitHubProxySettingsService proxySettingsService) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.proxySettingsService = proxySettingsService;
    }

    /**
     * Az aktuális proxykonfiguráció alapján felépíti a GitHub-kérésekhez használt HTTP klienst. A kliens minden híváskor a konfigurációs store jelenlegi állapotát veszi figyelembe.
     *
     * @return a művelet eredménye
     */
    private HttpClient currentHttpClient() {
        // Minden GitHub kérés az adatbázis aktuális SYSTEM_CONFIGURATION / SYSTEM_SECRET
        // értékeiből felépített klienst használja, ezért mentés után nem kell újraindítás.
        return buildHttpClient(proxySettingsService.getEntity());
    }

    /**
     * A megadott proxy-, hitelesítési, timeout- és TLS/truststore-beállításokból konfigurált {@link java.net.http.HttpClient} példányt épít. Hibás TLS-konfiguráció esetén nem nyeli el a problémát, hanem konfigurációs hibát jelez.
     *
     * @param settings az aktuális proxy/TLS beállítások
     * @return a művelet eredménye
     */
    private HttpClient buildHttpClient(GitHubProxySettings settings) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(safeTimeout(properties.getRequestTimeout()));

        try {
            // A GitHub kliens soha ne essen vissza a JVM globális (M2M) proxyjára.
            builder.proxy(noProxySelector());
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
                            LOGGER.info("GitHub schema updater HTTP proxy authentication enabled for user '{}'.", settings.getUsername());
                        } else {
                            LOGGER.warn("GitHub proxy username is configured, but no proxy password is stored; the connection will be attempted without proxy authentication.");
                        }
                    } else {
                        LOGGER.info("GitHub proxy username is empty; the connection will be attempted without proxy authentication.");
                    }
                    LOGGER.info("GitHub schema updater HTTP proxy enabled from SYSTEM_CONFIGURATION/SYSTEM_SECRET: {}:{}", host, port);
                }
            }
            SSLContext sslContext = buildSslContext(settings);
            if (sslContext != null) {
                builder.sslContext(sslContext);
                LOGGER.info("GitHub schema updater custom TLS settings enabled from SYSTEM_CONFIGURATION/SYSTEM_SECRET.");
            }
        } catch (Exception ex) {
            LOGGER.warn("GitHub schema updater proxy/TLS settings could not be applied. Direct HTTP client will be used. Cause: {}", ex.getMessage());
        }

        return builder.build();
    }

    /**
     * Olyan ProxySelector példányt hoz létre, amely minden URI-hoz közvetlen kapcsolatot választ. Ezzel a GitHub kliens megakadályozza, hogy konfigurált GitHub-proxy hiányában a JVM globális, más integrációhoz tartozó proxyjára essen vissza.
     *
     * @return a művelet eredménye
     */
    private ProxySelector noProxySelector() {
        return new ProxySelector() {
            /**
             * Minden cél URI esetén kizárólag a {@link java.net.Proxy#NO_PROXY} közvetlen kapcsolatot adja vissza.
             *
             * @param uri a művelethez átadott {@code uri} érték
             * @return a művelet eredménye
             */
            @Override
            public List<Proxy> select(URI uri) {
                return List.of(Proxy.NO_PROXY);
            }

            /**
             * A közvetlen GitHub kapcsolat sikertelenségét debug szinten naplózza az URI, socket cím és hibaüzenet megadásával; alternatív proxyt nem választ.
             *
             * @param uri a művelethez átadott {@code uri} érték
             * @param sa a művelethez átadott {@code sa} érték
             * @param ioe a művelethez átadott {@code ioe} érték
             */
            @Override
            public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                LOGGER.debug("Direct GitHub kapcsolat sikertelen. uri={}, address={}, reason={}", uri, sa, ioe == null ? "" : ioe.getMessage());
            }
        };
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
                lastResponse = currentHttpClient().send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
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
                lastResponse = currentHttpClient().send(request, HttpResponse.BodyHandlers.ofInputStream());
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
