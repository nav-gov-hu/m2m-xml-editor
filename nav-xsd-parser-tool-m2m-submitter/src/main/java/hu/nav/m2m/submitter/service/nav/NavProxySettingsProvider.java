package hu.nav.m2m.submitter.service.nav;

import hu.nav.m2m.submitter.domain.ProxySettings;

/**
 * A NAV M2M HTTP kliens proxy- és TLS-beállításainak forrása.
 *
 * <p>A modul önálló használatakor a régi M2M proxy tábla szolgálhat
 * tartalék forrásként, az alkalmazásban pedig a központi rendszerkonfiguráció
 * ad elsődleges implementációt.</p>
 */
public interface NavProxySettingsProvider {
    /**
     * Visszaadja a NAV HTTP kliens által aktuálisan használandó proxy- és TLS-beállításokat.
     *
     * <p>Az implementáció dönti el, hogy az érték központi konfigurációból vagy a régi M2M proxy táblából származik.</p>
     *
     * @return az aktuális proxy/TLS konfiguráció
     */
    ProxySettings getSettings();

    /**
     * A(z) {@code sourceName} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @return a művelet eredménye
     */
    default String sourceName() {
        return getClass().getSimpleName();
    }
}
