package hu.gov.nav.xsdparsertool.web.githubupdater.spi;

import hu.gov.nav.xsdparsertool.web.githubupdater.domain.GitHubProxySettings;

/**
 * A GitHub kimenő HTTP kapcsolat számára biztosítja az alkalmazás aktuális általános
 * proxy- és TLS-beállításait. A konkrét konfigurációs forrást a web modul adja.
 */
public interface GitHubNetworkSettingsProvider {

    /**
     * Visszaadja a GitHub kliens által használandó aktuális általános hálózati beállításokat.
     *
     * @return az általános proxy/TLS konfiguráció
     */
    GitHubProxySettings load();
}
