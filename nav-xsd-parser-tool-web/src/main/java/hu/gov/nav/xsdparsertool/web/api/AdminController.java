package hu.gov.nav.xsdparsertool.web.api;

import hu.gov.nav.xsdparsertool.core.support.SecureFileOperations;
import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import hu.gov.nav.xsdparsertool.schemaregistry.service.FileSystemSchemaRegistryService;
import hu.gov.nav.xsdparsertool.schemaregistry.service.SchemaRegistryStatus;
import hu.gov.nav.xsdparsertool.web.config.PathConfigurationProperties;
import hu.gov.nav.xsdparsertool.web.xmlfile.config.XmlFileStorageProperties;
import hu.gov.nav.xsdparsertool.web.xpath.config.XPathValidatorProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
/**
 * Az alkalmazás adminisztrációs, diagnosztikai, naplózási és cache-kezelési REST végpontjait biztosító controller.
 * Az osztály a api csomagból érhető el, és a projekt többi rétege innen hívja a publikus API-ját.
 * Spring regisztráció: @RestController.
 * Interfész/implementáció kapcsolat: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *
 * Spring registration: @RestController.
 * Interface/implementation relationship: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *

 */


@Tag(name = "Admin", description = "Alkalmazás-adminisztrációs REST végpontok. / Application administration REST endpoints.")
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminController.class);
    private static final String EXTERNAL_CONFIG_FILE_NAME = "nav-xsd-parser-tool-paths.properties";
    private static final List<String> ACTIVE_PROPERTY_KEYS = List.of(
            "spring.application.name",
            "server.port",
            "app.version",
            "app.data.dir",
            "app.log.level",
            "logging.file.name",
            "nav.xsdparsertool.paths.schema-dir",
            "nav.xsdparsertool.paths.common-xsd-dir",
            "nav.xsdparsertool.paths.ui-model-dir",
            "nav.xsdparsertool.xpath-validator.rule-root-dir",
            "nav.xsdparsertool.xpath-validator.xsl-root-dir",
            "nav.xsdparsertool.xpath-validator.result-dir",
            "nav.xsdparsertool.form.renderer.default",
            "nav.xsdparsertool.database.type",
            "nav.xsdparsertool.database.schema",
            "nav.xsdparsertool.database.encoding",
            "nav.xsdparsertool.xml-file.upload-dir",
            "nav.xsdparsertool.xml-file.backup-dir",
            "nav.xsdparsertool.xml-file.archive-dir",
            "nav.xsdparsertool.xml-file.xml-index-dir",
            "nav.xsdparsertool.xml-file.server-import.root-dir",
            "nav.xsdparsertool.xml-index.config-path",
            "nav.xsdparsertool.github-schema-updater.enabled",
            "nav.xsdparsertool.github-schema-updater.organization",
            "nav.xsdparsertool.github-schema-updater.api-base-url",
            "nav.xsdparsertool.github-schema-updater.download-mode",
            "nav.xsdparsertool.github-schema-updater.archive-url-template",
            "nav.xsdparsertool.github-schema-updater.repository-name-prefix",
            "nav.xsdparsertool.github-schema-updater.rate-limit-enabled",
            "nav.xsdparsertool.github-schema-updater.rate-limit-max-retries",
            "nav.xsdparsertool.github-schema-updater.rate-limit-default-secondary-wait",
            "nav.xsdparsertool.github-schema-updater.rate-limit-max-wait",
            "nav.xsdparsertool.github-schema-updater.rate-limit-print-headers",
            "spring.datasource.url",
            "spring.datasource.driver-class-name",
            "spring.jpa.hibernate.ddl-auto",
            "spring.flyway.enabled",
            "spring.flyway.locations",
            "spring.flyway.encoding"
    );

    private final FileSystemSchemaRegistryService schemaRegistryService;
    private final PathConfigurationProperties pathProperties;
    private final Environment environment;
    private final XmlFileStorageProperties storageProperties;
    private final XPathValidatorProperties xpathValidatorProperties;
    private final ConfigurableEnvironment configurableEnvironment;
    private final Instant startedAt = Instant.now();

    /**
     * Létrehozza a {@code AdminController} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param schemaRegistryService a művelet bemeneti {@code schemaRegistryService} értéke
     * @param pathProperties a feldolgozásban részt vevő fájl vagy elérési út
     * @param storageProperties a művelethez szükséges konfigurációs adatok
     * @param xpathValidatorProperties a feldolgozásban részt vevő fájl vagy elérési út
     * @param environment a művelet bemeneti {@code environment} értéke
     * @param configurableEnvironment a művelethez szükséges konfigurációs adatok
     */
    public AdminController(FileSystemSchemaRegistryService schemaRegistryService,
                           PathConfigurationProperties pathProperties,
                           XmlFileStorageProperties storageProperties,
                           XPathValidatorProperties xpathValidatorProperties,
                           Environment environment,
                           ConfigurableEnvironment configurableEnvironment) {
        this.schemaRegistryService = schemaRegistryService;
        this.pathProperties = pathProperties;
        this.storageProperties = storageProperties;
        this.xpathValidatorProperties = xpathValidatorProperties;
        this.environment = environment;
        this.configurableEnvironment = configurableEnvironment;
    }

    
    /**
     * A {@code systemInfo} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feldolgozás során felépített kulcs-érték leképezés
     */
    @Operation(summary = "HU: systemInfo REST művelet. EN: systemInfo REST operation.", description = "HU: Dokumentált REST végpont. EN: Documented REST endpoint.")

    @ApiResponses({

        @ApiResponse(responseCode = "200", description = "HU: Sikeres végrehajtás. EN: Successful execution."),

        @ApiResponse(responseCode = "400", description = "HU: Hibás kérés. EN: Bad request.", content = @Content(schema = @Schema(implementation = String.class))),

        @ApiResponse(responseCode = "500", description = "HU: Belső szerverhiba. EN: Internal server error.", content = @Content(schema = @Schema(implementation = String.class)))

    })
@GetMapping("/system")
/**
 * Összeállítja az admin felület számára az alkalmazás és a futtatókörnyezet alapvető rendszerinformációit.
 * @return a metódus által előállított eredmény
 */
    public Map<String, Object> systemInfo() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("applicationName", environment.getProperty("spring.application.name", "nav-xsd-parser-tool-web"));
        response.put("version", environment.getProperty("app.version", "unknown"));
        response.put("uptime", formatDuration(Duration.between(startedAt, Instant.now())));
        response.put("java", safeSystemProperty("java.runtime.version",safeSystemProperty("java.version", "unknown")));
        response.put("os", safeSystemProperty("os.name", "unknown") + " " + safeSystemProperty("os.version", ""));
        response.put("configDirectory", resolveExternalConfigPath().getParent() == null ? null : resolveExternalConfigPath().getParent().toString());
        response.put("logFile", resolveLogFile().toString());
        response.put("startedAt", startedAt.toString());
        response.put("storageDirectories", Map.of(
                "upload", resolvedPath(storageProperties.getUploadDir()),
                "backup", resolvedPath(storageProperties.getBackupDir()),
                "archive", resolvedPath(storageProperties.getArchiveDir()),
                "xmlIndex", resolvedPath(storageProperties.getXmlIndexDir())));
        return response;
    }

    /**
     * A {@code resolvedPath} művelet feloldja a megfelelő erőforrást, állapotot vagy értéket a rendelkezésre álló jelöltek közül.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param configured a művelethez szükséges konfigurációs adatok
     * @return a feloldott vagy lekért érték
     */
    private String resolvedPath(String configured) {
        return configured == null || configured.isBlank() ? null : Path.of(configured).toAbsolutePath().normalize().toString();
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
    
    /**
     * A {@code loggingInfo} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feldolgozás során felépített kulcs-érték leképezés
     */
    @Operation(summary = "HU: loggingInfo REST művelet. EN: loggingInfo REST operation.", description = "HU: Dokumentált REST végpont. EN: Documented REST endpoint.")

    @ApiResponses({

        @ApiResponse(responseCode = "200", description = "HU: Sikeres végrehajtás. EN: Successful execution."),

        @ApiResponse(responseCode = "400", description = "HU: Hibás kérés. EN: Bad request.", content = @Content(schema = @Schema(implementation = String.class))),

        @ApiResponse(responseCode = "500", description = "HU: Belső szerverhiba. EN: Internal server error.", content = @Content(schema = @Schema(implementation = String.class)))

    })
@GetMapping("/logging")
/**
 * Visszaadja az aktuális naplózási konfigurációt és a feloldott naplófájl adatait.
 * @return a metódus által előállított eredmény
 */
    public Map<String, Object> loggingInfo() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("rootLevel", getLoggerLevel(org.slf4j.Logger.ROOT_LOGGER_NAME));
        response.put("logFile", resolveLogFile().toString());
        response.put("externalConfigPath", resolveExternalConfigPath().toString());
        response.put("tailLines", readLastLogLines(200));
        return response;
    }

    
    /**
     * A {@code updateLogging} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param payload a művelet bemeneti {@code payload} értéke
     * @return a feldolgozás során felépített kulcs-érték leképezés
     */
    @Operation(summary = "HU: updateLogging REST művelet. EN: updateLogging REST operation.", description = "HU: Dokumentált REST végpont. EN: Documented REST endpoint.")

    @ApiResponses({

        @ApiResponse(responseCode = "200", description = "HU: Sikeres végrehajtás. EN: Successful execution."),

        @ApiResponse(responseCode = "400", description = "HU: Hibás kérés. EN: Bad request.", content = @Content(schema = @Schema(implementation = String.class))),

        @ApiResponse(responseCode = "500", description = "HU: Belső szerverhiba. EN: Internal server error.", content = @Content(schema = @Schema(implementation = String.class)))

    })
@PostMapping("/logging")
/**
 * Alkalmazza a támogatott futásidejű naplózási beállításokat a kapott adminisztrációs kérés alapján.
 * @param payload a {@code payload} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 */
    public Map<String, Object> updateLogging(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "HU: Kérés törzse. EN: Request body.") @RequestBody Map<String, Object> payload) {
        String rootLevel = normalizeLevel((String) payload.get("rootLevel"));
        boolean saveToExternalConfig = Boolean.TRUE.equals(payload.get("saveToExternalConfig"));

        if (rootLevel != null) {
            setLoggerLevel(org.slf4j.Logger.ROOT_LOGGER_NAME, rootLevel);
        }
        if (saveToExternalConfig) {
            saveLoggingSettings(rootLevel);
        }
        return loggingInfo();
    }

    
    /**
     * A {@code loggingTail} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param lines a művelet bemeneti {@code lines} értéke
     * @return a feldolgozás során felépített kulcs-érték leképezés
     */
    @Operation(summary = "Naplófájl végének lekérdezése / Read log tail", description = "HU: Dokumentált REST művelet. EN: Documented REST operation.")

    @ApiResponses({

            @ApiResponse(responseCode = "200", description = "HU: Sikeres végrehajtás. EN: Successful execution."),

            @ApiResponse(responseCode = "400", description = "HU: Hibás kérés. EN: Bad request.", content = @Content(schema = @Schema(implementation = String.class))),

            @ApiResponse(responseCode = "500", description = "HU: Belső szerverhiba. EN: Internal server error.", content = @Content(schema = @Schema(implementation = String.class)))

    })
@GetMapping("/logging/tail")
    public Map<String, Object> loggingTail(@Parameter(description = "HU: Végpont paraméter. EN: Endpoint parameter.") @RequestParam(name = "lines", defaultValue = "200") int lines) {
        if (lines < 1 || lines > 2000) {
            throw new IllegalArgumentException("A lines értéke 1 és 2000 közötti lehet.");
        }
        return Map.of("lines", readLastLogLines(lines));
    }

    
    /**
     * A {@code downloadLog} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a művelet feldolgozási eredménye
     */
    @Operation(summary = "HU: downloadLog REST művelet. EN: downloadLog REST operation.", description = "HU: Dokumentált REST végpont. EN: Documented REST endpoint.")

    @ApiResponses({

        @ApiResponse(responseCode = "200", description = "HU: Sikeres végrehajtás. EN: Successful execution."),

        @ApiResponse(responseCode = "400", description = "HU: Hibás kérés. EN: Bad request.", content = @Content(schema = @Schema(implementation = String.class))),

        @ApiResponse(responseCode = "500", description = "HU: Belső szerverhiba. EN: Internal server error.", content = @Content(schema = @Schema(implementation = String.class)))

    })
@GetMapping("/logging/download")
/**
 * Letölthető erőforrásként adja vissza az aktuális alkalmazásnapló fájlt.
 * @return a metódus által előállított eredmény
 */
    public ResponseEntity<Resource> downloadLog() {
        Path logFile = resolveLogFile();
        if (!ExceptionSafeOperations.fileExists(logFile)) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(logFile);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=app.log")
                .body(resource);
    }

    
    /**
     * A {@code configInfo} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A konfigurációs értékeket a web modul érvényes beállításaihoz igazítja, és az esetleges alapértelmezéseket csak a komponensben definiált szabályok szerint alkalmazza.</p>
     * @return a feldolgozás során felépített kulcs-érték leképezés
     */
    @Operation(summary = "HU: configInfo REST művelet. EN: configInfo REST operation.", description = "HU: Dokumentált REST végpont. EN: Documented REST endpoint.")

    @ApiResponses({

        @ApiResponse(responseCode = "200", description = "HU: Sikeres végrehajtás. EN: Successful execution."),

        @ApiResponse(responseCode = "400", description = "HU: Hibás kérés. EN: Bad request.", content = @Content(schema = @Schema(implementation = String.class))),

        @ApiResponse(responseCode = "500", description = "HU: Belső szerverhiba. EN: Internal server error.", content = @Content(schema = @Schema(implementation = String.class)))

    })
@GetMapping("/config")
/**
 * Összegyűjti a diagnosztikai felületen megjelenített aktív konfigurációs és útvonalinformációkat.
 * @return a metódus által előállított eredmény
 */
    public Map<String, Object> configInfo() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("externalConfigPath", resolveExternalConfigPath().toString());
        response.put("activeProperties", activeProperties());
        response.put("propertySources", propertySources());
        response.put("diagnostics", diagnostics());
        return response;
    }

    
    /**
     * A {@code reloadConfig} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A konfigurációs értékeket a web modul érvényes beállításaihoz igazítja, és az esetleges alapértelmezéseket csak a komponensben definiált szabályok szerint alkalmazza.</p>
     * @return a feldolgozás során felépített kulcs-érték leképezés
     */
    @Operation(summary = "HU: reloadConfig REST művelet. EN: reloadConfig REST operation.", description = "HU: Dokumentált REST végpont. EN: Documented REST endpoint.")

    @ApiResponses({

        @ApiResponse(responseCode = "200", description = "HU: Sikeres végrehajtás. EN: Successful execution."),

        @ApiResponse(responseCode = "400", description = "HU: Hibás kérés. EN: Bad request.", content = @Content(schema = @Schema(implementation = String.class))),

        @ApiResponse(responseCode = "500", description = "HU: Belső szerverhiba. EN: Internal server error.", content = @Content(schema = @Schema(implementation = String.class)))

    })
@PostMapping("/config/reload")
/**
 * Újraolvassa a támogatott külső konfigurációs forrásokat, majd visszaadja az újratöltés eredményét.
 * @return a metódus által előállított eredmény
 */
    public Map<String, Object> reloadConfig() {
        Map<String, String> properties = loadExternalProperties();
        applyPathProperties(properties);
        String rootLevel = firstNonBlank(properties.get("logging.level.root"), properties.get("app.log.level"));
        if (rootLevel != null) {
            setLoggerLevel(org.slf4j.Logger.ROOT_LOGGER_NAME, rootLevel);
        }
        return configInfo();
    }

    
    /**
     * A {@code cacheInfo} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feldolgozás során felépített kulcs-érték leképezés
     */
    @Operation(summary = "HU: cacheInfo REST művelet. EN: cacheInfo REST operation.", description = "HU: Dokumentált REST végpont. EN: Documented REST endpoint.")

    @ApiResponses({

        @ApiResponse(responseCode = "200", description = "HU: Sikeres végrehajtás. EN: Successful execution."),

        @ApiResponse(responseCode = "400", description = "HU: Hibás kérés. EN: Bad request.", content = @Content(schema = @Schema(implementation = String.class))),

        @ApiResponse(responseCode = "500", description = "HU: Belső szerverhiba. EN: Internal server error.", content = @Content(schema = @Schema(implementation = String.class)))

    })
@GetMapping("/cache")
/**
 * Összegyűjti a felületen megjelenített cache-állapotinformációkat.
 * @return a metódus által előállított eredmény
 */
    public Map<String, Object> cacheInfo() {
        SchemaRegistryStatus status = schemaRegistryService.getStatus();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("schemaRegistry", status);
        return response;
    }

    
    /**
     * A {@code clearCaches} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feldolgozás során felépített kulcs-érték leképezés
     */
    @Operation(summary = "HU: clearCaches REST művelet. EN: clearCaches REST operation.", description = "HU: Dokumentált REST végpont. EN: Documented REST endpoint.")

    @ApiResponses({

        @ApiResponse(responseCode = "200", description = "HU: Sikeres végrehajtás. EN: Successful execution."),

        @ApiResponse(responseCode = "400", description = "HU: Hibás kérés. EN: Bad request.", content = @Content(schema = @Schema(implementation = String.class))),

        @ApiResponse(responseCode = "500", description = "HU: Belső szerverhiba. EN: Internal server error.", content = @Content(schema = @Schema(implementation = String.class)))

    })
@PostMapping("/cache/clear")
/**
 * Törli az adminisztrációból üríthető alkalmazás-cache-eket.
 * @return a metódus által előállított eredmény
 */
    public Map<String, Object> clearCaches() {
        schemaRegistryService.reloadAsync(toPath(pathProperties.getSchemaDir()), toPath(pathProperties.getCommonXsdDir()));
        return cacheInfo();
    }

    
    /**
     * A {@code reloadCaches} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feldolgozás során felépített kulcs-érték leképezés
     */
    @Operation(summary = "HU: reloadCaches REST művelet. EN: reloadCaches REST operation.", description = "HU: Dokumentált REST végpont. EN: Documented REST endpoint.")

    @ApiResponses({

        @ApiResponse(responseCode = "200", description = "HU: Sikeres végrehajtás. EN: Successful execution."),

        @ApiResponse(responseCode = "400", description = "HU: Hibás kérés. EN: Bad request.", content = @Content(schema = @Schema(implementation = String.class))),

        @ApiResponse(responseCode = "500", description = "HU: Belső szerverhiba. EN: Internal server error.", content = @Content(schema = @Schema(implementation = String.class)))

    })
@PostMapping("/cache/reload")
/**
 * Újraépíti vagy újratölti az adminisztrációból frissíthető cache-eket.
 * @return a metódus által előállított eredmény
 */
    public Map<String, Object> reloadCaches() {
        schemaRegistryService.reloadAsync(toPath(pathProperties.getSchemaDir()), toPath(pathProperties.getCommonXsdDir()));
        return cacheInfo();
    }

    
    /**
     * A {@code diagnostics} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feldolgozás során felépített kulcs-érték leképezés
     */
    @Operation(summary = "HU: diagnostics REST művelet. EN: diagnostics REST operation.", description = "HU: Dokumentált REST végpont. EN: Documented REST endpoint.")

    @ApiResponses({

        @ApiResponse(responseCode = "200", description = "HU: Sikeres végrehajtás. EN: Successful execution."),

        @ApiResponse(responseCode = "400", description = "HU: Hibás kérés. EN: Bad request.", content = @Content(schema = @Schema(implementation = String.class))),

        @ApiResponse(responseCode = "500", description = "HU: Belső szerverhiba. EN: Internal server error.", content = @Content(schema = @Schema(implementation = String.class)))

    })
@GetMapping("/diagnostics")
/**
 * Összeállítja az alkalmazás diagnosztikai állapotát a támogatási és hibakeresési felület számára.
 * @return a metódus által előállított eredmény
 */
    public Map<String, Object> diagnostics() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("schemaRoot", pathProperties.getSchemaDir());
        response.put("commonXsd", pathProperties.getCommonXsdDir());
        response.put("xpathRules", xpathValidatorProperties.getRuleRootDir());
        response.put("uiModels", pathProperties.getUiModelDir());
        response.put("logDirectory", resolveLogFile().getParent() == null ? null : resolveLogFile().getParent().toString());
        response.put("externalConfigPath", resolveExternalConfigPath().toString());
        response.put("externalConfigExists", ExceptionSafeOperations.fileExists(resolveExternalConfigPath()));
        response.put("schemaRootExists", isDirectory(pathProperties.getSchemaDir()));
        response.put("commonXsdExists", isDirectory(pathProperties.getCommonXsdDir()));
        response.put("xpathRulesExists", isDirectory(xpathValidatorProperties.getRuleRootDir()));
        response.put("uiModelsExists", isDirectory(pathProperties.getUiModelDir()));
        response.put("logDirectoryExists", ExceptionSafeOperations.isDirectory(resolveLogFile().getParent()));
        return response;
    }
/**
 * Visszaadja a diagnosztikában engedélyezett aktív konfigurációs kulcsok feloldott értékeit.
 * @return a metódus által előállított eredmény
 */

    private Map<String, String> activeProperties() {
        Map<String, String> map = new LinkedHashMap<>();
        for (String key : ACTIVE_PROPERTY_KEYS) {
            map.put(key, environment.getProperty(key));
        }
        return map;
    }
/**
 * Felsorolja a Spring környezet aktív property-forrásait diagnosztikai célra.
 * @return a metódus által előállított eredmény
 */

    private List<String> propertySources() {
        List<String> names = new ArrayList<>();
        for (PropertySource<?> propertySource : configurableEnvironment.getPropertySources()) {
            names.add(propertySource.getName());
        }
        return names;
    }
/**
 * A konfigurált naplófájl utolsó sorait olvassa be a megadott biztonságos korláton belül.
 * @param lines a {@code lines} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 */

    private List<String> readLastLogLines(int lines) {
        if (lines < 1 || lines > 2000) {
            throw new IllegalArgumentException("A lines értéke 1 és 2000 közötti lehet.");
        }
        Path logFile = resolveLogFile();
        if (!ExceptionSafeOperations.fileExists(logFile)) {
            return List.of("A log fájl még nem létezik: " + logFile);
        }
        try (Stream<String> stream = Files.lines(logFile, StandardCharsets.UTF_8)) {
            List<String> all = stream.toList();
            int from = Math.max(0, all.size() - lines);
            return all.subList(from, all.size());
        } catch (IOException e) {
            LOGGER.error("Failed to read application log tail.", e);
            return List.of("A log fájl nem olvasható.");
        }
    }
/**
 * Perzisztálja a támogatott naplózási beállításokat a külső konfigurációs állományban.
 * @param rootLevel a {@code rootLevel} paraméter átadott értéke
 */

    private void saveLoggingSettings(String rootLevel) {
        Path externalConfigPath = resolveExternalConfigPath();
        try {
            ExceptionSafeOperations.createDirectories(externalConfigPath.getParent());
            Map<String, String> properties = loadExternalProperties();
            if (rootLevel != null) {
                properties.put("app.log.level", rootLevel);
                properties.put("logging.level.root", rootLevel);
            }
            storeProperties(properties, externalConfigPath);
        } catch (IOException e) {
            throw new IllegalStateException("Nem sikerült menteni a külső konfigurációt: " + e.getMessage(), e);
        }
    }
/**
 * Beolvassa az alkalmazás külső properties állományát, ha az létezik és olvasható.
 * @return a metódus által előállított eredmény
 */

private Map<String, String> loadExternalProperties() {
    Path externalConfigPath = resolveSafeExternalConfigPath();
    Map<String, String> properties = new LinkedHashMap<>();

    if (externalConfigPath == null || !ExceptionSafeOperations.fileExists(externalConfigPath, LinkOption.NOFOLLOW_LINKS)) {
        return properties;
    }

    try (BufferedReader reader = Files.newBufferedReader(externalConfigPath, StandardCharsets.UTF_8)) {
        String line;

        while ((line = reader.readLine()) != null) {
            String trimmed = line.trim();

            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
                continue;
            }

            int separatorIndex = findPropertySeparator(line);
            if (separatorIndex < 0) {
                properties.put(trimmed, "");
                continue;
            }

            String key = line.substring(0, separatorIndex).trim();
            String value = line.substring(separatorIndex + 1).trim();
            properties.put(key, value);
        }
    } catch (IOException e) {
        throw new IllegalStateException("Nem sikerült beolvasni a külső konfigurációt: " + externalConfigPath, e);
    }

    return properties;
}
/**
 * Megkeresi egy properties sor kulcs és érték közötti első érvényes elválasztóját.
 * @param line a {@code line} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 */

    private int findPropertySeparator(String line) {
        boolean escaped = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '\\' && !escaped) {
                escaped = true;
                continue;
            }
            if ((ch == '=' || ch == ':') && !escaped) {
                return i;
            }
            escaped = false;
        }
        return -1;
    }
/**
 * Biztonságosan kiírja a módosított properties tartalmat a cél konfigurációs fájlba.
 * @param properties a {@code properties} paraméter átadott értéke
 * @param externalConfigPath a {@code externalConfigPath} paraméter átadott értéke
 * @throws IOException Hiba esetén dobott kivétel.
 */

    private void storeProperties(Map<String, String> properties, Path externalConfigPath) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("# NAV XSD parser tool external configuration");
        properties.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> lines.add(entry.getKey() + "=" + entry.getValue()));
        SecureFileOperations.writePrivateString(externalConfigPath, String.join(System.lineSeparator(), lines) + System.lineSeparator(), StandardCharsets.UTF_8);
    }
/**
 * Az újratöltött külső útvonalbeállításokat átvezeti a futásidejű path konfigurációs objektumra.
 * @param properties a {@code properties} paraméter átadott értéke
 */

    private void applyPathProperties(Map<String, String> properties) {
        pathProperties.setSchemaDir(properties.getOrDefault("nav.xsdparsertool.paths.schema-dir", pathProperties.getSchemaDir()));
        pathProperties.setCommonXsdDir(properties.getOrDefault("nav.xsdparsertool.paths.common-xsd-dir", pathProperties.getCommonXsdDir()));
        xpathValidatorProperties.setRuleRootDir(properties.getOrDefault("nav.xsdparsertool.xpath-validator.rule-root-dir", xpathValidatorProperties.getRuleRootDir()));
        xpathValidatorProperties.setXslRootDir(properties.getOrDefault("nav.xsdparsertool.xpath-validator.xsl-root-dir", xpathValidatorProperties.getXslRootDir()));
        xpathValidatorProperties.setResultDir(properties.getOrDefault("nav.xsdparsertool.xpath-validator.result-dir", xpathValidatorProperties.getResultDir()));
    }
/**
 * Beállítja a {@code loggerLevel} mező értékét.
 * @param loggerName a beállítandó új érték
 * @param levelName a {@code levelName} paraméter átadott értéke
 */

    private void setLoggerLevel(String loggerName, String levelName) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger logger = context.getLogger(loggerName);
        logger.setLevel(Level.toLevel(levelName, Level.INFO));
    }
/**
 * Visszaadja a {@code loggerLevel} mező aktuális értékét.
 * @param loggerName a {@code loggerName} paraméter átadott értéke
 * @return a {@code loggerLevel} mező értéke
 */

    private String getLoggerLevel(String loggerName) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger logger = context.getLogger(loggerName);
        Level level = logger.getLevel();
        if (level != null) {
            return level.levelStr;
        }
        Level effectiveLevel = logger.getEffectiveLevel();
        return effectiveLevel == null ? null : effectiveLevel.levelStr;
    }
/**
 * Feloldja a szerkesztendő külső konfigurációs fájl tényleges helyét az aktív környezetből.
 * @return a metódus által előállított eredmény
 */

    private Path resolveExternalConfigPath() {
        String importValue = environment.getProperty("spring.config.import");
        if (StringUtils.hasText(importValue)) {
            for (String token : importValue.split(",")) {
                Path candidate = resolveConfigImportCandidate(token);
                if (candidate != null
                        && candidate.getFileName() != null
                        && EXTERNAL_CONFIG_FILE_NAME.equals(candidate.getFileName().toString())) {
                    return candidate;
                }
            }
        }
        String programData = System.getenv("ProgramData");
        if (StringUtils.hasText(programData)) {
            return Path.of(programData, "NAV-M2M", "config", EXTERNAL_CONFIG_FILE_NAME).toAbsolutePath().normalize();
        }
        return Path.of("..", "config", EXTERNAL_CONFIG_FILE_NAME).toAbsolutePath().normalize();
    }

    /**
     * A {@code resolveConfigImportCandidate} művelet feloldja a megfelelő erőforrást, állapotot vagy értéket a rendelkezésre álló jelöltek közül.
     *
     * <p>A konfigurációs értékeket a web modul érvényes beállításaihoz igazítja, és az esetleges alapértelmezéseket csak a komponensben definiált szabályok szerint alkalmazza.</p>
     * @param token a művelet bemeneti {@code token} értéke
     * @return a feloldott vagy lekért érték
     */
    private Path resolveConfigImportCandidate(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        String cleaned = token.trim();
        if (cleaned.startsWith("optional:file:")) {
            cleaned = cleaned.substring("optional:file:".length());
        } else if (cleaned.startsWith("file:")) {
            cleaned = cleaned.substring("file:".length());
        } else {
            return null;
        }
        if (!StringUtils.hasText(cleaned) || cleaned.contains("${")) {
            return null;
        }
        return Path.of(cleaned).toAbsolutePath().normalize();
    }
/**
 * Feloldja az alkalmazás aktuális naplófájljának útvonalát a konfigurált logging beállításokból.
 * @return a metódus által előállított eredmény
 */

private Path resolveLogFile() {
    Map<String, String> externalProperties = safeLoadExternalProperties();

    String configured = firstNonBlank(
            environment.getProperty("logging.file.name"),
            externalProperties.get("logging.file.name")
    );

    if (StringUtils.hasText(configured)) {
        return Path.of(configured).toAbsolutePath().normalize();
    }

    String dataDir = firstNonBlank(
            environment.getProperty("app.data.dir"),
            externalProperties.get("app.data.dir")
    );

    if (StringUtils.hasText(dataDir)) {
        return Path.of(dataDir).toAbsolutePath().normalize().resolve("logs").resolve("app.log");
    }

    return Path.of(ExceptionSafeOperations.systemProperty("java.io.tmpdir"))
            .toAbsolutePath()
            .normalize()
            .resolve("nav-xsd-parser-tool.log");
}

    /**
     * A {@code safeLoadExternalProperties} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feldolgozás során felépített kulcs-érték leképezés
     */
    private Map<String, String> safeLoadExternalProperties() {
        try {
            return loadExternalProperties();
        } catch (RuntimeException ex) {
            LOGGER.warn("Nem sikerült beolvasni a külső konfigurációt.", ex);
            return Map.of();
        }
    }
/**
 * Egységes, támogatott naplózási szintre normalizálja a felhasználótól kapott szintnevet.
 * @param value a {@code value} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 */

    private String normalizeLevel(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
/**
 * Emberileg olvasható időtartam-szöveget készít a megadott milliszekundum értékből.
 * @param duration a {@code duration} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 */

    private String formatDuration(Duration duration) {
        long seconds = Math.max(0, duration.getSeconds());
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        List<String> parts = new ArrayList<>();
        if (days > 0) parts.add(days + " nap");
        if (hours > 0 || days > 0) parts.add(hours + " óra");
        if (minutes > 0 || hours > 0 || days > 0) parts.add(minutes + " perc");
        parts.add(secs + " mp");
        return String.join(" ", parts);
    }
/**
 * Prioritási sorrendben az első nem üres szöveges értéket választja ki.
 * @param values a {@code values} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 */

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }
/**
 * Megadja a {@code directory} logikai állapot aktuális értékét.
 * @param value a {@code value} paraméter átadott értéke
 * @return a {@code directory} mező értéke
 */

    private boolean isDirectory(String value) {
        Path path = toPath(value);
        return path != null && ExceptionSafeOperations.isDirectory(path);
    }
/**
 * A konfigurációs szöveget opcionális, normalizált fájlrendszeri útvonallá alakítja.
 * @param value a {@code value} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 */

    private Path toPath(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return Path.of(value.trim()).toAbsolutePath().normalize();
    }

    /**
     * A {@code resolveSafeExternalConfigPath} művelet feloldja a megfelelő erőforrást, állapotot vagy értéket a rendelkezésre álló jelöltek közül.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @return a feloldott vagy lekért érték
     */
    private Path resolveSafeExternalConfigPath() {
        Path configuredPath = resolveExternalConfigPath();
        if (configuredPath == null) {
            return null;
        }

        Path normalizedPath = configuredPath.toAbsolutePath().normalize();

        if (normalizedPath.getFileName() == null
                || !EXTERNAL_CONFIG_FILE_NAME.equals(normalizedPath.getFileName().toString())) {
            throw new IllegalStateException("Nem engedélyezett külső konfigurációs fájlnév: " + normalizedPath);
        }

        Path parent = normalizedPath.getParent();
        if (parent == null) {
            throw new IllegalStateException("A külső konfigurációs fájlnak nincs szülőkönyvtára: " + normalizedPath);
        }

        Path normalizedParent = parent.toAbsolutePath().normalize();

        if (!ExceptionSafeOperations.fileExists(normalizedParent, LinkOption.NOFOLLOW_LINKS)) {
            return normalizedPath;
        }

        Path realParent;
        try {
            realParent = normalizedParent.toRealPath(LinkOption.NOFOLLOW_LINKS);
        } catch (IOException e) {
            throw new IllegalStateException("A külső konfigurációs könyvtár nem oldható fel: " + normalizedParent, e);
        }

        Path safePath = realParent.resolve(EXTERNAL_CONFIG_FILE_NAME).normalize();

        if (!safePath.startsWith(realParent)) {
            throw new IllegalStateException("A külső konfigurációs útvonal kilépne a config könyvtárból: " + safePath);
        }

        if (ExceptionSafeOperations.fileExists(safePath, LinkOption.NOFOLLOW_LINKS)
                && !ExceptionSafeOperations.isRegularFile(safePath, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("A külső konfigurációs útvonal nem normál fájl: " + safePath);
        }

        return safePath;
    }

}
