package hu.gov.nav.xsdparsertool.web.network;

import hu.nav.m2m.submitter.domain.ProxySettings;
import hu.nav.m2m.submitter.service.nav.NavProxySettingsProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * A NAV M2M HTTP kliens központi proxy- és TLS-konfigurációs providere.
 *
 * <p>A webalkalmazásban minden token-, nonce-, online validációs, kalkulációs és beküldési
 * hívás a {@link SystemTableM2mProxySettingsService} által kezelt {@code system_configuration}
 * és {@code system_secret} értékeket használja. A régi {@code m2m_proxy_settings} tábla csak
 * az önálló submitter modul kompatibilitási fallbackje.</p>
 */
@Component
@Primary
public class SystemConfigurationNavProxySettingsProvider implements NavProxySettingsProvider {
    private final SystemTableM2mProxySettingsService settingsService;

    /**
     * Létrehozza a központi NAV proxy/TLS providert.
     *
     * @param settingsService a központi M2M proxy/TLS konfigurációs szolgáltatás
     */
    public SystemConfigurationNavProxySettingsProvider(SystemTableM2mProxySettingsService settingsService) {
        this.settingsService = settingsService;
    }

    /**
     * Visszaadja a NAV HTTP kliens által aktuálisan használandó proxy- és TLS-beállításokat.
     *
     * @return az aktuális központi konfiguráció
     */
    @Override
    public ProxySettings getSettings() {
        return settingsService.getEntity();
    }

    /**
     * Megadja a naplózható konfigurációforrás nevét.
     *
     * @return a konfiguráció forrása
     */
    @Override
    public String sourceName() {
        return "system_configuration/system_secret";
    }
}
