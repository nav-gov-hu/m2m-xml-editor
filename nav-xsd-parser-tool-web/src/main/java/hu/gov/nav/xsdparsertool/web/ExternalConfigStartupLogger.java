package hu.gov.nav.xsdparsertool.web;

import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.core.env.ConfigurableEnvironment;
/**
 * Az alkalmazás indulásakor naplózza a külső konfigurációs fájl feloldott helyét és az útvonalhoz kapcsolódó diagnosztikai adatokat.
 * Az osztály a web csomagból érhető el, és a projekt többi rétege innen hívja a publikus API-ját.
 * Spring regisztráció: @Component.
 * Interfész/implementáció kapcsolat: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *
 * Spring registration: @Component.
 * Interface/implementation relationship: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *

 */


@Component
public class ExternalConfigStartupLogger implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalConfigStartupLogger.class);

    private final Environment environment;

    private final ConfigurableEnvironment configurableEnvironment;

    /**
     * Létrehozza a {@code ExternalConfigStartupLogger} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param environment a művelet bemeneti {@code environment} értéke
     * @param configurableEnvironment a művelethez szükséges konfigurációs adatok
     */
    public ExternalConfigStartupLogger(Environment environment, ConfigurableEnvironment configurableEnvironment) {
        this.environment = environment;
        this.configurableEnvironment = configurableEnvironment;
    }


    /**
     * A {@code run} művelet elindítja vagy végrehajtja a kapcsolódó alkalmazási folyamatot.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param args a művelet bemeneti {@code args} értéke
     */
/**
 * Naplózza az alkalmazás indulásakor feloldott külső konfigurációs fájl helyét és elérhetőségét.
 * @param args a {@code args} paraméter átadott értéke
 */
    @Override
    public void run(ApplicationArguments args) {
        LOGGER.info("===== Külső konfiguráció ellenőrzés indul =====");
        LOGGER.info("Working directory: {}", safeSystemProperty("user.dir", "unknown"));

        String programData = System.getenv("ProgramData");
        String appDataDir = environment.getProperty("app.data.dir");
        Path configDir = resolveConfigDirectory(appDataDir);
        Path propertiesFile = configDir.resolve("nav-xsd-parser-tool-paths.properties");

        LOGGER.info("ProgramData: {}", programData);
        LOGGER.info("app.data.dir: {}", appDataDir);
        LOGGER.info("Konfigurációs könyvtár: {}", configDir.toAbsolutePath());
        LOGGER.info("Konfigurációs fájl: {}", propertiesFile.toAbsolutePath());
        LOGGER.info("Konfigurációs könyvtár létezik: {}", ExceptionSafeOperations.fileExists(configDir));
        LOGGER.info("Konfigurációs fájl létezik: {}", ExceptionSafeOperations.fileExists(propertiesFile));
        LOGGER.info("Konfigurációs fájl olvasható: {}", Files.isReadable(propertiesFile));

        LOGGER.info("spring.config.additional-location = {}",
                environment.getProperty("spring.config.additional-location"));

        LOGGER.info("nav.xsdparsertool.paths.schema-dir = {}",
                environment.getProperty("nav.xsdparsertool.paths.schema-dir"));
        LOGGER.info("nav.xsdparsertool.paths.common-xsd-dir = {}",
                environment.getProperty("nav.xsdparsertool.paths.common-xsd-dir"));
        LOGGER.info("nav.xsdparsertool.paths.ui-model-dir = {}",
                environment.getProperty("nav.xsdparsertool.paths.ui-model-dir"));
        LOGGER.info("nav.xsdparsertool.xpath-validator.rule-root-dir = {}",
                environment.getProperty("nav.xsdparsertool.xpath-validator.rule-root-dir"));
        LOGGER.info("nav.xsdparsertool.xpath-validator.xsl-root-dir = {}",
                environment.getProperty("nav.xsdparsertool.xpath-validator.xsl-root-dir"));
        LOGGER.info("Aktív property source-ok:");
        configurableEnvironment.getPropertySources().forEach(ps -> LOGGER.info(" - {}", ps.getName()));

        LOGGER.info("===== Külső konfiguráció ellenőrzés vége =====");
    }


    /**
     * Feloldja a diagnosztikában használt konfigurációs könyvtárat platformfüggetlen módon.
     * Elsődlegesen az alkalmazás közös {@code app.data.dir} értékét használja.
     * Ha ez még nem áll rendelkezésre, a munkakönyvtár alatti {@code config} könyvtárra esik vissza,
     * ezért egy hiányzó Windows-specifikus {@code ProgramData} környezeti változó sem állíthatja le az alkalmazást.
     *
     * @param appDataDir az alkalmazás adatkönyvtárának konfigurált értéke
     * @return a konfigurációs könyvtár abszolút, normalizált útvonala
     */
    private Path resolveConfigDirectory(String appDataDir) {
        if (appDataDir != null && !appDataDir.isBlank()) {
            return Path.of(appDataDir).resolve("config").toAbsolutePath().normalize();
        }
        return Path.of("config").toAbsolutePath().normalize();
    }

    /**
     * A {@code safeSystemProperty} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A konfigurációs értékeket a web modul érvényes beállításaihoz igazítja, és az esetleges alapértelmezéseket csak a komponensben definiált szabályok szerint alkalmazza.</p>
     * @param key a művelet bemeneti {@code key} értéke
     * @param defaultValue a művelet bemeneti {@code defaultValue} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String safeSystemProperty(String key, String defaultValue) {
        try {
            return ExceptionSafeOperations.systemProperty(key, defaultValue);
        } catch (SecurityException ex) {
            LOGGER.warn("Nem sikerült beolvasni a rendszer property-t: {}", key, ex);
            return defaultValue;
        }
    }
}