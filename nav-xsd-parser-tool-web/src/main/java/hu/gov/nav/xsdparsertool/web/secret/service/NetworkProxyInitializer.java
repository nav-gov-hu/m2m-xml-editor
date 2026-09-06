package hu.gov.nav.xsdparsertool.web.secret.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URI;

/**
 * A web modul titokkezelési területének közös alkalmazási típusa.
 *
 * <p>A {@code NetworkProxyInitializer} osztály a web modul titokkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Component
public class NetworkProxyInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(NetworkProxyInitializer.class);
    private final Environment env;

    /**
     * Létrehozza a {@code NetworkProxyInitializer} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param env a művelet bemeneti {@code env} értéke
     */
    public NetworkProxyInitializer(Environment env) {
        this.env = env;
    }

    /**
     * A {@code run} művelet elindítja vagy végrehajtja a kapcsolódó alkalmazási folyamatot.
     *
     * <p>A művelet a titokkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param args a művelet bemeneti {@code args} értéke
     */
    @Override
    public void run(ApplicationArguments args) {
        if (!env.getProperty("nav.xsdparsertool.network.proxy.enabled", Boolean.class, false)) {
            return;
        }
        String rawHost = env.getProperty("nav.xsdparsertool.network.proxy.host", "");
        String port = env.getProperty("nav.xsdparsertool.network.proxy.port", "8080");
        String host = normalizeHost(rawHost);
        if (host.isBlank()) {
            return;
        }
        System.setProperty("http.proxyHost", host);
        System.setProperty("http.proxyPort", port);
        System.setProperty("https.proxyHost", host);
        System.setProperty("https.proxyPort", port);
        String bypass = env.getProperty("nav.xsdparsertool.network.proxy.non-proxy-hosts", "").replace(',', '|');
        if (!bypass.isBlank()) {
            System.setProperty("http.nonProxyHosts", bypass);
            System.setProperty("https.nonProxyHosts", bypass);
        }
        log.info("Adatbázisos proxybeállítás aktiválva. host={}, port={}", host, port);
    }

    /**
     * A {@code normalizeHost} művelet feldolgozza és normalizálja a bemeneti adatot a további feldolgozás számára.
     *
     * <p>A művelet a titokkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param rawValue a művelet bemeneti {@code rawValue} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String normalizeHost(String rawValue) {
        String raw = rawValue == null ? "" : rawValue.trim();
        if (raw.isBlank()) {
            return "";
        }
        try {
            URI uri = raw.contains("://") ? URI.create(raw) : URI.create("http://" + raw);
            if (uri.getHost() != null && !uri.getHost().isBlank()) {
                return uri.getHost();
            }
        } catch (IllegalArgumentException ignored) {
            // Fallback below.
        }
        String host = raw.replaceFirst("(?i)^https?://", "");
        int slash = host.indexOf('/');
        if (slash >= 0) host = host.substring(0, slash);
        int colon = host.indexOf(':');
        if (colon >= 0) host = host.substring(0, colon);
        return host.trim();
    }
}
