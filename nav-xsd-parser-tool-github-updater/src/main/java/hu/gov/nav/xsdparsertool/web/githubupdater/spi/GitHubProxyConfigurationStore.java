package hu.gov.nav.xsdparsertool.web.githubupdater.spi;

import hu.gov.nav.xsdparsertool.web.githubupdater.domain.GitHubProxySettings;

/**
 * A GitHub proxy konfiguráció perzisztencia-független elérése.
 * A web modul implementációja a SYSTEM_CONFIGURATION és SYSTEM_SECRET táblákat használja.
 */
public interface GitHubProxyConfigurationStore {
    /**
     * Betölti az aktuálisan érvényes GitHub proxy- és truststore-konfigurációt a konkrét perzisztencia-megvalósítástól függetlenül.
     *
     * @return a művelet eredménye
     */
    GitHubProxySettings load();
    /**
     * Elmenti a megadott GitHub proxy-konfigurációt; a törlési jelzők külön szabályozzák a tárolt titkok eltávolítását.
     *
     * @param settings az aktuális proxy/TLS beállítások
     * @param clearPassword ha igaz, a tárolt proxyjelszót törölni kell
     * @param clearTrustStorePassword ha igaz, a tárolt truststore-jelszót törölni kell
     * @return a művelet eredménye
     */
    GitHubProxySettings save(GitHubProxySettings settings, boolean clearPassword, boolean clearTrustStorePassword);
}
