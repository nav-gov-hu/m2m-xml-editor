package hu.gov.nav.xsdparsertool.web.setup;

import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertiesPropertySource;

/**
 * A web modul kezdeti beállítási területének közös alkalmazási típusa.
 *
 * <p>A {@code BootstrapDefaultsEnvironmentPostProcessor} osztály a web modul kezdeti beállítási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public class BootstrapDefaultsEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    public static final String PROPERTY_SOURCE = "m2mXmlEditorBootstrapDefaults";

    /**
     * A {@code postProcessEnvironment} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a kezdeti beállítási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param environment a művelet bemeneti {@code environment} értéke
     * @param application a művelet bemeneti {@code application} értéke
     */
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String configuredDatasourceUrl = trimToNull(environment.getProperty("spring.datasource.url"));
        boolean existingDatasourceConfiguration = configuredDatasourceUrl != null;

        Path dataDirectory = resolveDataDirectory(environment);
        boolean bootstrapAutoLoadEnabled = environment.getProperty(
                "m2m.xml.editor.bootstrap.auto-load-enabled", Boolean.class, true);
        Path bootstrapFile = bootstrapAutoLoadEnabled
                ? resolveBootstrapFile(dataDirectory)
                : dataDirectory.resolve("config").resolve("application-bootstrap.properties");
        boolean generatedBootstrapExists = bootstrapAutoLoadEnabled
                && ExceptionSafeOperations.isRegularFile(bootstrapFile);
        boolean useGeneratedBootstrap = generatedBootstrapExists && !existingDatasourceConfiguration;
        Properties generatedBootstrapProperties = null;
        if (useGeneratedBootstrap) {
            Path parent = bootstrapFile.getParent();
            if (parent != null && parent.getParent() != null) {
                dataDirectory = parent.getParent();
            }
            generatedBootstrapProperties = loadBootstrapProperties(bootstrapFile);
        }
        boolean generatedDatasourceConfiguration = generatedBootstrapProperties != null
                && trimToNull(generatedBootstrapProperties.getProperty("spring.datasource.url")) != null;
        boolean effectiveDatasourceConfiguration = existingDatasourceConfiguration || generatedDatasourceConfiguration;
        String effectiveDatabaseType = trimToNull(environment.getProperty("nav.xsdparsertool.database.type"));
        if (effectiveDatabaseType == null && generatedBootstrapProperties != null) {
            effectiveDatabaseType = trimToNull(generatedBootstrapProperties.getProperty("nav.xsdparsertool.database.type"));
        }
        if (effectiveDatabaseType == null) {
            String effectiveUrl = configuredDatasourceUrl != null
                    ? configuredDatasourceUrl
                    : generatedBootstrapProperties == null ? null : trimToNull(generatedBootstrapProperties.getProperty("spring.datasource.url"));
            if (effectiveUrl != null && effectiveUrl.startsWith("jdbc:oracle:")) {
                effectiveDatabaseType = "ORACLE";
            }
        }

        Map<String, Object> defaults = new LinkedHashMap<>();
        // app.data.dir must exist even on a completely clean IDE/dev startup.
        // Several internal defaults are expressed relative to this root. Without it,
        // Spring attempts to resolve ${app.data.dir} while the setup catalog is initialized
        // and aborts before the first-run setup can be shown.
        defaults.put("app.data.dir", normalize(dataDirectory));
        defaults.put("nav.xsdparsertool.data-directory", dataDirectory.toString());
        defaults.put("nav.xsdparsertool.bootstrap-config-file", bootstrapFile.toString());
        // A kötelező XML tárhelyek már a Spring beanek létrehozásakor szükségesek.
        // Ezek az alacsony prioritású defaultok biztosítják, hogy egy szűz vagy
        // részben migrált adatbázis se álljon le a pending setup véglegesítése előtt.
        // A databaseSystemConfiguration property source addFirst prioritása miatt
        // a DB-ben tárolt, nem üres egyedi értékek továbbra is felülírják ezeket.
        defaults.put("nav.xsdparsertool.xml-file.upload-dir", normalize(dataDirectory.resolve("data/xml")));
        defaults.put("nav.xsdparsertool.xml-file.backup-dir", normalize(dataDirectory.resolve("backup")));
        defaults.put("nav.xsdparsertool.xml-file.archive-dir", normalize(dataDirectory.resolve("data/archive")));
        defaults.put("nav.xsdparsertool.xml-file.xml-index-dir", normalize(dataDirectory.resolve("data/xml-index")));
        // Visszafelé kompatibilis Oracle fallback: a korábban létrehozott bootstrap fájlok
        // még nem tartalmazzák ezt a Hibernate-beállítást. Alacsony prioritású defaultként
        // csak Oracle datasource mellett adjuk hozzá, így a többi támogatott DB-t nem érinti.
        if ("ORACLE".equalsIgnoreCase(effectiveDatabaseType)) {
            defaults.put("spring.jpa.properties.hibernate.type.preferred_instant_jdbc_type", "TIMESTAMP");
        }
        // A H2 datasource/dialect/Flyway defaultok kizárólag valóban konfigurálatlan első
        // indításkor érvényesülhetnek. Meglévő MySQL/Oracle/PostgreSQL datasource mellé
        // még alacsony prioritású fallbackként sem szabad H2 dialektust adni, mert a
        // Hibernate azt választaná, ha a külső konfiguráció nem rögzít explicit dialektust.
        if (!effectiveDatasourceConfiguration) {
            defaults.put("spring.datasource.url", "jdbc:h2:file:" + normalize(dataDirectory.resolve("database").resolve("schema-explorer")) + ";AUTO_SERVER=TRUE");
            defaults.put("spring.datasource.username", "sa");
            defaults.put("spring.datasource.password", "");
            defaults.put("spring.datasource.driver-class-name", "org.h2.Driver");
            defaults.put("spring.jpa.database-platform", "org.hibernate.dialect.H2Dialect");
            defaults.put("spring.flyway.enabled", "true");
            defaults.put("spring.flyway.locations", "classpath:db/migration/H2");
            // A tiszta első indítás H2 fallbackje ugyanazt a logikai DB-konfigurációt
            // képviselje, mint amit a setup H2 kiválasztása generál. Ellenkező esetben
            // a setup pusztán a hiányzó explicit metaadatok miatt DB-váltást érzékelne,
            // és felesleges BOOTSTRAP_RESTART ágba kerülne setup.completed=false értékkel.
            defaults.put("nav.xsdparsertool.database.type", "H2");
            defaults.put("nav.xsdparsertool.database.schema", "PUBLIC");
            defaults.put("nav.xsdparsertool.database.encoding", "UTF-8");
            defaults.put("spring.h2.console.enabled", "true");
            defaults.put("spring.h2.console.path", "/h2-console");
        }
        defaults.put("nav.xsdparsertool.security.mode", "MULTI_USER");
        // Meglévő külső datasource esetén visszafelé kompatibilisen kész telepítésnek tekintjük.
        // A setup csak akkor indul, ha sem külső datasource, sem korábban generált bootstrap nincs.
        defaults.put("nav.xsdparsertool.setup.completed", Boolean.toString(existingDatasourceConfiguration));
        defaults.put("nav.xsdparsertool.security.bootstrap-admin.enabled", "false");
        environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE, defaults));

        if (!existingDatasourceConfiguration || useGeneratedBootstrap) {
            createDirectories(dataDirectory);
        }
        if (useGeneratedBootstrap) {
            // A generált bootstrap felülírja a beépített defaultokat, de nem írja felül
            // a parancssori, környezeti vagy explicit külső konfigurációt.
            environment.getPropertySources().addBefore(PROPERTY_SOURCE,
                    new PropertiesPropertySource("m2mXmlEditorBootstrapFile", generatedBootstrapProperties));
        }
    }

    /** Betölti a generált bootstrap properties állományt, vagy olvasási hiba esetén leállítja az indulást. */
    private Properties loadBootstrapProperties(Path bootstrapFile) {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(bootstrapFile)) {
            properties.load(input);
            return properties;
        } catch (IOException ex) {
            throw new IllegalStateException("A bootstrap konfiguráció nem olvasható: " + bootstrapFile, ex);
        }
    }

    /**
     * A {@code resolveBootstrapFile} művelet feloldja a megfelelő erőforrást, állapotot vagy értéket a rendelkezésre álló jelöltek közül.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param defaultDataDirectory a művelet bemeneti {@code defaultDataDirectory} értéke
     * @return a feloldott vagy lekért érték
     */
    private Path resolveBootstrapFile(Path defaultDataDirectory) {
        Path locator = locatorFile();
        if (ExceptionSafeOperations.isRegularFile(locator)) {
            Properties properties = new Properties();
            try (InputStream input = Files.newInputStream(locator)) {
                properties.load(input);
                String configured = properties.getProperty("bootstrap.file");
                if (configured != null && !configured.isBlank()) {
                    return Path.of(configured).toAbsolutePath().normalize();
                }
            } catch (IOException ignored) {
                // Hibás locator esetén az operációs rendszer szerinti alapértelmezés érvényesül.
            }
        }
        return defaultDataDirectory.resolve("config").resolve("application-bootstrap.properties");
    }

    /**
     * A {@code locatorFile} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @return a művelet feldolgozási eredménye
     */
    public static Path locatorFile() {
        return Path.of(ExceptionSafeOperations.systemProperty("user.home", "."), ".m2m-xml-editor", "bootstrap-location.properties")
                .toAbsolutePath().normalize();
    }

    /**
     * A {@code resolveDataDirectory} művelet feloldja a megfelelő erőforrást, állapotot vagy értéket a rendelkezésre álló jelöltek közül.
     *
     * <p>A művelet a kezdeti beállítási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param environment a művelet bemeneti {@code environment} értéke
     * @return a feloldott vagy lekért érték
     */
    private Path resolveDataDirectory(ConfigurableEnvironment environment) {
        String explicit = firstNonBlank(environment.getProperty("app.data.dir"),
                firstNonBlank(environment.getProperty("M2M_XML_EDITOR_HOME"),
                        environment.getProperty("m2m.xml.editor.home")));
        if (explicit != null) {
            return Path.of(explicit).toAbsolutePath().normalize();
        }
        String os = ExceptionSafeOperations.systemProperty("os.name", "");
        if (os.regionMatches(true, 0, "windows", 0, Math.min(os.length(), "windows".length()))) {
            String programData = System.getenv("ProgramData");
            if (programData != null && !programData.isBlank()) {
                Path candidate = Path.of(programData, "M2M-XML-EDITOR").toAbsolutePath().normalize();
                if (isWritableLocation(candidate)) {
                    return candidate;
                }
            }
            String local = System.getenv("LOCALAPPDATA");
            if (local != null && !local.isBlank()) {
                return Path.of(local, "M2M-XML-EDITOR").toAbsolutePath().normalize();
            }
        }
        String home = ExceptionSafeOperations.systemProperty("user.home", ".");
        return Path.of(home, ".local", "share", "m2m-xml-editor").toAbsolutePath().normalize();
    }

    /**
     * A {@code isWritableLocation} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a kezdeti beállítási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param target a művelet bemeneti {@code target} értéke
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    private boolean isWritableLocation(Path target) {
        Path probe = Files.exists(target) ? target : target.getParent();
        return probe != null && Files.exists(probe) && Files.isWritable(probe);
    }

    /**
     * A {@code createDirectories} művelet létrehozza vagy tartósítja a kért állapotváltozást.
     *
     * <p>A művelet a kezdeti beállítási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param root a művelet bemeneti {@code root} értéke
     */
    private void createDirectories(Path root) {
        try {
            for (String name : new String[]{"config", "database", "logs", "certificates", "backup", "data/xml", "data/import", "data/attachments", "data/exports", "data/archive", "data/xml-index", "data/xpath/results", "repo/xsd", "repo/xsd/common", "repo/uimodel", "repo/xpath", "repo/rule-xsl"}) {
                ExceptionSafeOperations.createDirectories(root.resolve(name));
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Az alapértelmezett adatkönyvtár nem hozható létre: " + root, ex);
        }
    }

    /**
     * A {@code normalize} művelet feldolgozza és normalizálja a bemeneti adatot a további feldolgozás számára.
     *
     * <p>A művelet a kezdeti beállítási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param path a feldolgozásban részt vevő fájl vagy elérési út
     * @return a művelet feldolgozási eredménye
     */
    private String normalize(Path path) {
        return path.toAbsolutePath().normalize().toString().replace('\\', '/');
    }

    /**
     * A {@code firstNonBlank} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a kezdeti beállítási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param a a művelet bemeneti {@code a} értéke
     * @param b a művelet bemeneti {@code b} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return null;
    }

    /**
     * A {@code trimToNull} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a kezdeti beállítási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param value a művelet bemeneti {@code value} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * A {@code getOrder} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a kezdeti beállítási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    @Override
    public int getOrder() {
        // A Spring Config Data feldolgozása után kell futnia, hogy a meglévő külső
        // MySQL/Oracle/PostgreSQL/H2 konfiguráció felismerhető legyen.
        return Ordered.LOWEST_PRECEDENCE - 100;
    }
}
