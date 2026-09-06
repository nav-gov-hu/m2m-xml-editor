package hu.nav.m2m.submitter.service.nav;

import hu.nav.m2m.submitter.domain.ProxySettings;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.auth.CredentialsProviderBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.net.URI;
import java.util.Arrays;

/**
 * A NAV kommunikációhoz RestTemplate példányt épít az aktuális proxy-, TLS- és truststore-beállítások figyelembevételével.
 */
@Component
public class NavRestTemplateFactory {
    private final NavProxySettingsProvider proxySettingsProvider;

    /**
     * Létrehozza a(z) {@code NavRestTemplateFactory} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
     *
     * @param proxySettingsProvider a művelethez átadott {@code proxySettingsProvider} érték
     */
    public NavRestTemplateFactory(NavProxySettingsProvider proxySettingsProvider) {
        this.proxySettingsProvider = proxySettingsProvider;
    }

    /**
     * RestTemplate példányt készít az aktuális proxy és TLS/truststore beállításokból; a hálózati kliens létrehozásakor csak ellenőrzött konfigurációt alkalmaz.
     *
     * @return a művelet eredménye
     */
    public RestTemplate create() {
        return create(proxySettingsProvider.getSettings());
    }

    /**
     * RestTemplate példányt készít az aktuális proxy és TLS/truststore beállításokból; a hálózati kliens létrehozásakor csak ellenőrzött konfigurációt alkalmaz.
     *
     * @param settings az aktuális proxy/TLS beállítások
     * @return a művelet eredménye
     */
    public RestTemplate create(ProxySettings settings) {
        ProxySettings effectiveSettings = settings == null ? new ProxySettings() : settings;
        RequestConfig.Builder requestConfigBuilder = requestConfig(effectiveSettings);
        HttpHost proxy = null;
        ProxyTarget proxyTarget = null;
        if (effectiveSettings.isEnabled()) {
            proxyTarget = parseProxy(effectiveSettings);
            proxy = new HttpHost(proxyTarget.scheme(), proxyTarget.host(), proxyTarget.port());
            requestConfigBuilder.setProxy(proxy);
        }

        CredentialsProvider credentialsProvider = null;
        if (effectiveSettings.isEnabled() && proxyTarget != null
                && effectiveSettings.getUsername() != null && !effectiveSettings.getUsername().isBlank()
                && effectiveSettings.getPassword() != null && !effectiveSettings.getPassword().isBlank()) {
            credentialsProvider = CredentialsProviderBuilder.create()
                    .add(new AuthScope(proxyTarget.host(), proxyTarget.port()),
                            new UsernamePasswordCredentials(effectiveSettings.getUsername(), effectiveSettings.getPassword().toCharArray()))
                    .build();
        }

        PoolingHttpClientConnectionManager connectionManager = buildConnectionManager(effectiveSettings);
        var clientBuilder = HttpClients.custom()
                .setDefaultRequestConfig(requestConfigBuilder.build())
                .setConnectionManager(connectionManager)
                .disableRedirectHandling();
        if (credentialsProvider != null) {
            clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
        }

        CloseableHttpClient client = clientBuilder.build();
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(client);
        return new RestTemplate(factory);
    }

    /**
     * Létrehozza a NAV HTTP-hívások alap Apache HttpClient kéréskonfigurációját.
     *
     * <p>A proxy- és TLS-specifikus beállításokat a hívó később egészíti ki; ez a lépés a kapcsolódási,
     * kapcsolatfoglalási és válaszidő-korlátok egységes alapértékeit állítja be.</p>
     *
     * @param settings az aktuális proxy/TLS beállítások; a jelenlegi timeoutértékek kiválasztását nem módosítja
     * @return tovább konfigurálható kéréskonfiguráció-builder
     */
    RequestConfig.Builder requestConfig(ProxySettings settings) {
        return RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(10))
                .setConnectionRequestTimeout(Timeout.ofSeconds(10))
                .setResponseTimeout(Timeout.ofSeconds(60));
    }

    /**
     * A(z) {@code proxySnapshot} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @return a művelet eredménye
     */
    public String proxySnapshot() {
        return "proxy.source=" + proxySettingsProvider.sourceName() + '\n'
                + proxySnapshot(proxySettingsProvider.getSettings());
    }

    /**
     * A(z) {@code proxySnapshot} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param settings az aktuális proxy/TLS beállítások
     * @return a művelet eredménye
     */
    public String proxySnapshot(ProxySettings settings) {
        StringBuilder sb = new StringBuilder();
        sb.append("proxy.enabled=").append(settings.isEnabled()).append('\n');
        sb.append("proxy.url=").append(settings.getProxyUrl() == null ? "" : settings.getProxyUrl()).append('\n');
        sb.append("proxy.port=").append(settings.getProxyPort() == null ? "" : settings.getProxyPort()).append('\n');
        sb.append("proxy.username=").append(mask(settings.getUsername())).append('\n');
        sb.append("proxy.password=").append(settings.getPassword() == null || settings.getPassword().isBlank() ? "" : "****").append('\n');
        sb.append("tls.sslVerificationDisabled=").append(settings.isSslVerificationDisabled()).append('\n');
        sb.append("tls.trustStorePath=").append(settings.getTrustStorePath() == null ? "" : settings.getTrustStorePath()).append('\n');
        sb.append("tls.trustStoreType=").append(settings.getTrustStoreType() == null ? "JKS" : settings.getTrustStoreType()).append('\n');
        sb.append("tls.trustStorePassword=").append(settings.getTrustStorePassword() == null || settings.getTrustStorePassword().isBlank() ? "" : "****");
        return sb.toString();
    }

    /**
     * Apache HTTP connection managert épít a proxy- és opcionális egyedi TLS/truststore konfigurációhoz.
     *
     * @param settings az aktuális proxy/TLS beállítások
     * @return a művelet eredménye
     */
    private PoolingHttpClientConnectionManager buildConnectionManager(ProxySettings settings) {
        try {
            if (settings.isSslVerificationDisabled()) {
                throw new IllegalStateException("A TLS tanúsítvány-ellenőrzés kikapcsolása biztonsági okból nem engedélyezett.");
            }

            if (settings.getTrustStorePath() != null && !settings.getTrustStorePath().isBlank()) {
                java.nio.file.Path trustStorePath = java.nio.file.Path.of(settings.getTrustStorePath().trim())
                        .toAbsolutePath().normalize().toRealPath(java.nio.file.LinkOption.NOFOLLOW_LINKS);
                File trustStoreFile = trustStorePath.toFile();
                if (!trustStoreFile.isFile()) {
                    throw new IllegalStateException("A megadott truststore nem található: " + trustStoreFile.getAbsolutePath());
                }
                char[] password = settings.getTrustStorePassword() == null ? new char[0] : settings.getTrustStorePassword().toCharArray();
                try {
                    SSLContext sslContext = SSLContextBuilder.create()
                            .setKeyStoreType(settings.getTrustStoreType() == null || settings.getTrustStoreType().isBlank() ? "JKS" : settings.getTrustStoreType())
                            .loadTrustMaterial(trustStoreFile, password)
                            .build();
                    return PoolingHttpClientConnectionManagerBuilder.create()
                            .setSSLSocketFactory(SSLConnectionSocketFactoryBuilder.create()
                                    .setSslContext(sslContext)
                                    .build())
                            .build();
                } finally {
                    Arrays.fill(password, '\0');
                }
            }

            SSLContext defaultContext = SSLContext.getDefault();
            return PoolingHttpClientConnectionManagerBuilder.create()
                    .setSSLSocketFactory(SSLConnectionSocketFactoryBuilder.create()
                            .setSslContext(defaultContext)
                            .build())
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("TLS/truststore konfiguráció hibás: " + e.getMessage(), e);
        }
    }

    /**
     * A(z) {@code hasCustomTlsSettings} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param settings az aktuális proxy/TLS beállítások
     * @return igaz, ha a vizsgált feltétel teljesül; egyébként hamis
     */
    private boolean hasCustomTlsSettings(ProxySettings settings) {
        return settings != null && (settings.isSslVerificationDisabled()
                || (settings.getTrustStorePath() != null && !settings.getTrustStorePath().isBlank()));
    }

    /**
     * A(z) {@code parseProxy} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param settings az aktuális proxy/TLS beállítások
     * @return a művelet eredménye
     */
    private ProxyTarget parseProxy(ProxySettings settings) {
        if (settings.getProxyUrl() == null || settings.getProxyUrl().isBlank()) {
            throw new IllegalStateException("Proxy használat engedélyezve van, de a proxy URL nincs megadva");
        }
        if (settings.getProxyPort() == null || settings.getProxyPort() < 1 || settings.getProxyPort() > 65535) {
            throw new IllegalStateException("Proxy használat engedélyezve van, de a proxy port hibás vagy üres");
        }

        String raw = settings.getProxyUrl().trim();
        String scheme = "http";
        String host = raw;
        try {
            URI uri = raw.contains("://") ? URI.create(raw) : URI.create("http://" + raw);
            if (uri.getScheme() != null && !uri.getScheme().isBlank()) {
                scheme = uri.getScheme();
            }
            if (uri.getHost() != null && !uri.getHost().isBlank()) {
                host = uri.getHost();
            }
        } catch (IllegalArgumentException ignored) {
            host = raw.replace("http://", "").replace("https://", "");
        }
        if (host.contains("/")) {
            host = host.substring(0, host.indexOf('/'));
        }
        if (host.contains(":")) {
            host = host.substring(0, host.indexOf(':'));
        }
        return new ProxyTarget(scheme, host, settings.getProxyPort());
    }

    /**
     * Érzékeny konfigurációs vagy transport adatot maszkol naplózáshoz.
     *
     * @param value a feldolgozandó érték
     * @return a művelet eredménye
     */
    private String mask(String value) {
        return value == null || value.isBlank() ? value : "****";
    }

    /**
     * A NAV M2M submitter modul {@code ProxyTarget} típusának felelősségét megvalósító típus.
     */
    /**
     * Létrehozza a(z) {@code ProxyTarget} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
     *
     * @param scheme a művelethez átadott {@code scheme} érték
     * @param host a művelethez átadott {@code host} érték
     * @param port a művelethez átadott {@code port} érték
     */
    private record ProxyTarget(String scheme, String host, int port) {}
}
