package hu.gov.nav.xsdparsertool.web;

import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import org.springframework.core.env.Environment;

/**
 * Desktop indítási integrációk konfigurációs segédosztálya.
 */
public final class DesktopIntegrationSettings {

    public static final String LEGACY_DISABLED_PROPERTY = "nav.desktop.disabled";
    public static final String ENABLED_PROPERTY = "nav.xsdparsertool.desktop.enabled";
    public static final String SPLASH_ENABLED_PROPERTY = "nav.xsdparsertool.desktop.splash.enabled";
    public static final String BROWSER_OPEN_ENABLED_PROPERTY = "nav.xsdparsertool.desktop.browser-open-enabled";
    public static final String TRAY_ENABLED_PROPERTY = "nav.xsdparsertool.desktop.tray-enabled";

    /**
     * Létrehozza a {@code DesktopIntegrationSettings} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     */
    private DesktopIntegrationSettings() {
    }

    /**
     * A {@code isDesktopEnabled} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param environment a művelet bemeneti {@code environment} értéke
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    public static boolean isDesktopEnabled(Environment environment) {
        if (Boolean.getBoolean(LEGACY_DISABLED_PROPERTY)) {
            return false;
        }
        return getBoolean(environment, ENABLED_PROPERTY, true);
    }

    /**
     * A {@code isSplashEnabled} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param environment a művelet bemeneti {@code environment} értéke
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    public static boolean isSplashEnabled(Environment environment) {
        return isDesktopEnabled(environment) && getBoolean(environment, SPLASH_ENABLED_PROPERTY, true);
    }

    /**
     * A {@code isBrowserOpenEnabled} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param environment a művelet bemeneti {@code environment} értéke
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    public static boolean isBrowserOpenEnabled(Environment environment) {
        return isDesktopEnabled(environment) && getBoolean(environment, BROWSER_OPEN_ENABLED_PROPERTY, true);
    }

    /**
     * A {@code isTrayEnabled} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param environment a művelet bemeneti {@code environment} értéke
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    public static boolean isTrayEnabled(Environment environment) {
        return isDesktopEnabled(environment) && getBoolean(environment, TRAY_ENABLED_PROPERTY, true);
    }

    /**
     * A {@code isDesktopEnabledFromSystemProperties} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    public static boolean isDesktopEnabledFromSystemProperties() {
        if (Boolean.getBoolean(LEGACY_DISABLED_PROPERTY)) {
            return false;
        }
        return getSystemBoolean(ENABLED_PROPERTY, true);
    }

    /**
     * A {@code getBoolean} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param environment a művelet bemeneti {@code environment} értéke
     * @param propertyName a feloldáshoz vagy azonosításhoz használt név
     * @param defaultValue a művelet bemeneti {@code defaultValue} értéke
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    private static boolean getBoolean(Environment environment, String propertyName, boolean defaultValue) {
        if (environment == null) {
            return getSystemBoolean(propertyName, defaultValue);
        }
        String value = environment.getProperty(propertyName);
        if (value == null || value.isBlank()) {
            return getSystemBoolean(propertyName, defaultValue);
        }
        return Boolean.parseBoolean(value.trim());
    }

    /**
     * A {@code environmentVariableName} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param propertyName a feloldáshoz vagy azonosításhoz használt név
     * @return a művelet feldolgozási eredménye
     */
    private static String environmentVariableName(String propertyName) {
        if (ENABLED_PROPERTY.equals(propertyName)) return "NAV_XSDPARSERTOOL_DESKTOP_ENABLED";
        if (SPLASH_ENABLED_PROPERTY.equals(propertyName)) return "NAV_XSDPARSERTOOL_DESKTOP_SPLASH_ENABLED";
        if (BROWSER_OPEN_ENABLED_PROPERTY.equals(propertyName)) return "NAV_XSDPARSERTOOL_DESKTOP_BROWSER_OPEN_ENABLED";
        if (TRAY_ENABLED_PROPERTY.equals(propertyName)) return "NAV_XSDPARSERTOOL_DESKTOP_TRAY_ENABLED";
        if (LEGACY_DISABLED_PROPERTY.equals(propertyName)) return "NAV_DESKTOP_DISABLED";
        return propertyName.replace('.', '_').replace('-', '_');
    }

    /**
     * A {@code getSystemBoolean} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param propertyName a feloldáshoz vagy azonosításhoz használt név
     * @param defaultValue a művelet bemeneti {@code defaultValue} értéke
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    private static boolean getSystemBoolean(String propertyName, boolean defaultValue) {
        String value = ExceptionSafeOperations.systemProperty(propertyName);
        if (value == null || value.isBlank()) {
            value = System.getenv(environmentVariableName(propertyName));
        }
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }
}
