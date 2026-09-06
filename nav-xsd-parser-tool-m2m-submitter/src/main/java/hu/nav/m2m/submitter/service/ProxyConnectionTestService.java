package hu.nav.m2m.submitter.service;

import hu.nav.m2m.submitter.config.NavM2mProperties;
import hu.nav.m2m.submitter.domain.ProxySettings;
import hu.nav.m2m.submitter.dto.ProxyTestRequest;
import hu.nav.m2m.submitter.dto.ProxyTestResponse;
import hu.nav.m2m.submitter.service.nav.NavProxySettingsProvider;
import hu.nav.m2m.submitter.service.nav.NavRestTemplateFactory;
import hu.nav.m2m.submitter.service.nav.TransportExceptionFormatter;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Ideiglenes proxy/TLS beállításokkal ellenőrzi a konfigurált NAV kapcsolatot anélkül, hogy a tesztbeállításokat perzisztálná.
 */
@Service
public class ProxyConnectionTestService {
    private final NavProxySettingsProvider proxySettingsProvider;
    private final NavRestTemplateFactory restTemplateFactory;
    private final NavM2mProperties properties;

    /**
     * Létrehozza a(z) {@code ProxyConnectionTestService} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
     *
     * @param proxySettingsProvider az aktív alkalmazási proxy/TLS konfiguráció forrása
     * @param restTemplateFactory a művelethez átadott {@code restTemplateFactory} érték
     * @param properties az M2M külső konfiguráció
     */
    public ProxyConnectionTestService(NavProxySettingsProvider proxySettingsProvider, NavRestTemplateFactory restTemplateFactory,
                                      NavM2mProperties properties) {
        this.proxySettingsProvider = proxySettingsProvider;
        this.restTemplateFactory = restTemplateFactory;
        this.properties = properties;
    }

    /**
     * A megadott vagy aktuális konfigurációval diagnosztikai kapcsolatpróbát hajt végre, majd strukturált eredményt ad vissza.
     *
     * @param request a REST vagy szolgáltatási művelet bemeneti kérése
     * @return a művelet eredménye
     */
    public ProxyTestResponse test(ProxyTestRequest request) {
        ProxySettings settings = copyCurrentSettingsForTest(request);
        String testUrl = configuredTestUrl();

        ProxyTestResponse result = new ProxyTestResponse();
        result.setProxyEnabled(settings.isEnabled());
        result.setTestUrl(testUrl);
        result.setProxySnapshot(restTemplateFactory.proxySnapshot(settings));

        long start = System.nanoTime();
        try {
            RestTemplate restTemplate = restTemplateFactory.create(settings);
            ResponseEntity<String> response = restTemplate.exchange(testUrl, HttpMethod.GET, null, String.class);
            result.setHttpStatus(response.getStatusCode().value());
            result.setSuccess(response.getStatusCode().is2xxSuccessful() || response.getStatusCode().is3xxRedirection());
            result.setMessage("Proxy kapcsolat teszt sikeres. HTTP státusz: " + response.getStatusCode().value());
        } catch (RestClientException | IllegalArgumentException | IllegalStateException ex) {
            result.setSuccess(false);
            result.setMessage("Proxy kapcsolat teszt sikertelen [" + TransportExceptionFormatter.probableArea(ex) + "]: " + TransportExceptionFormatter.describe(ex));
        } finally {
            result.setDurationMs((System.nanoTime() - start) / 1_000_000L);
        }
        return result;
    }

    /**
     * Az aktuális perzisztált proxybeállításokból ideiglenes tesztpéldányt készít, majd csak a kérésben megadott értékeket írja felül; ezzel a kapcsolatpróba nem módosítja a tárolt konfigurációt.
     *
     * @param request a REST vagy szolgáltatási művelet bemeneti kérése
     * @return a művelet eredménye
     */
    private ProxySettings copyCurrentSettingsForTest(ProxyTestRequest request) {
        ProxySettings settings = new ProxySettings();
        ProxySettings current = proxySettingsProvider.getSettings();
        settings.setId(1L);
        settings.setEnabled(request.isEnabled());
        settings.setProxyUrl(blankToNull(request.getProxyUrl()));
        settings.setProxyPort(request.getProxyPort());
        settings.setUsername(blankToNull(request.getUsername()));
        if (request.isClearPassword()) {
            settings.setPassword(null);
        } else if (request.getPassword() != null && !request.getPassword().isBlank()) {
            settings.setPassword(request.getPassword());
        } else {
            settings.setPassword(current.getPassword());
        }
        settings.setSslVerificationDisabled(request.isSslVerificationDisabled());
        settings.setTrustStorePath(blankToNull(request.getTrustStorePath()));
        settings.setTrustStoreType(defaultTrustStoreType(request.getTrustStoreType()));
        if (request.isClearTrustStorePassword()) {
            settings.setTrustStorePassword(null);
        } else if (request.getTrustStorePassword() != null && !request.getTrustStorePassword().isBlank()) {
            settings.setTrustStorePassword(request.getTrustStorePassword());
        } else {
            settings.setTrustStorePassword(current.getTrustStorePassword());
        }
        return settings;
    }

    /**
     * A(z) {@code configuredTestUrl} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @return igaz, ha a vizsgált feltétel teljesül; egyébként hamis
     */
    private String configuredTestUrl() {
        String value = properties.getEndpoints().getCommonBaseUrl();
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("A nav.m2m.endpoints.common-base-url nincs beállítva");
        }
        String trimmed = value.trim();
        if (!trimmed.startsWith("https://") && !trimmed.startsWith("http://")) {
            throw new IllegalStateException("Az M2M common base URL csak HTTP vagy HTTPS lehet");
        }
        return trimmed;
    }

    /**
     * A(z) {@code blankToNull} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param value a feldolgozandó érték
     * @return a művelet eredménye
     */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * A(z) {@code defaultTrustStoreType} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param value a feldolgozandó érték
     * @return a művelet eredménye
     */
    private String defaultTrustStoreType(String value) {
        return value == null || value.isBlank() ? "JKS" : value.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
