package hu.nav.m2m.submitter.service.nav;

import hu.nav.m2m.submitter.domain.ProxySettings;
import hu.nav.m2m.submitter.service.ProxySettingsService;
import org.springframework.stereotype.Component;

/**
 * Visszafelé kompatibilis proxyforrás az önálló M2M modul számára.
 * A teljes alkalmazásban ezt a központi rendszerkonfigurációs provider felülírja.
 */
@Component
public class LegacyM2mProxySettingsProvider implements NavProxySettingsProvider {
    private final ProxySettingsService proxySettingsService;

    /**
     * Létrehozza a(z) {@code LegacyM2mProxySettingsProvider} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
     *
     * @param proxySettingsService a művelethez átadott {@code proxySettingsService} érték
     */
    public LegacyM2mProxySettingsProvider(ProxySettingsService proxySettingsService) {
        this.proxySettingsService = proxySettingsService;
    }

    /**
     * Visszaadja a(z) settings aktuális értékét.
     *
     * @return a művelet eredménye
     */
    @Override
    public ProxySettings getSettings() {
        return proxySettingsService.getEntity();
    }

    /**
     * A(z) {@code sourceName} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @return a művelet eredménye
     */
    @Override
    public String sourceName() {
        return "m2m_proxy_settings";
    }
}
