package hu.gov.nav.xsdparsertool.web.setup;

import hu.gov.nav.xsdparsertool.core.support.SecureFileOperations;
import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import hu.gov.nav.xsdparsertool.web.support.RepositoryAccess;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import hu.gov.nav.xsdparsertool.web.audit.AuditLogService;
import hu.gov.nav.xsdparsertool.web.security.usermanagement.UserManagementService;
import hu.gov.nav.xsdparsertool.web.security.usermanagement.dto.UserSaveRequest;
import hu.gov.nav.xsdparsertool.web.systemconfig.entity.SystemConfigurationEntity;
import hu.gov.nav.xsdparsertool.web.systemconfig.repository.SystemConfigurationRepository;
import hu.gov.nav.xsdparsertool.web.systemconfig.service.ConfigurationCatalog;
import hu.gov.nav.xsdparsertool.web.systemconfig.service.SystemConfigurationService;

/**
 * A kapcsolódó webes üzleti vagy alkalmazási folyamatokat összefogó szolgáltatás.
 *
 * <p>A {@code SetupService} osztály a web modul kezdeti beállítási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Service
public class SetupService {
    private final Environment environment;
    private final SetupStateService state;
    private final UserManagementService users;
    private final SystemConfigurationRepository configurations;
    private final SystemConfigurationService systemConfigurationService;
    private final AuditLogService audit;
    private volatile String pendingCompletionError = "";
    /**
     * Létrehozza a {@code SetupService} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param environment a művelet bemeneti {@code environment} értéke
     * @param state a feldolgozandó elemek kollekciója
     * @param users a művelet felhasználói kontextusa vagy felhasználóneve
     * @param configurations a művelethez szükséges konfigurációs adatok
     * @param systemConfigurationService a rendszerkonfiguráció mentését végző szolgáltatás
     * @param audit a művelet bemeneti {@code audit} értéke
     */
    public SetupService(Environment environment, SetupStateService state, UserManagementService users,
                        SystemConfigurationRepository configurations,
                        SystemConfigurationService systemConfigurationService,
                        AuditLogService audit) {
        this.environment = environment;
        this.state = state;
        this.users = users;
        this.configurations = configurations;
        this.systemConfigurationService = systemConfigurationService;
        this.audit = audit;
    }
    /**
     * A {@code defaultDataDirectory} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a kezdeti beállítási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a művelet feldolgozási eredménye
     */
    public String defaultDataDirectory(){ return environment.getProperty("nav.xsdparsertool.data-directory", ""); }
    /**
     * A {@code complete} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a kezdeti beállítási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet eredményeként előállított egyedi elemek halmaza
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @Transactional
    public synchronized SetupResult complete(SetupRequest request) throws IOException {
        String token = SetupDataDirectorySelectionVault.issue(request == null ? null : request.dataDirectory());
        return completeSelected(
                token,
                request == null ? null : request.securityMode(),
                request == null ? null : request.databaseType(),
                request == null ? null : request.databaseHost(),
                request == null ? null : request.databasePort(),
                request == null ? null : request.databaseName(),
                request == null ? null : request.databaseSchema(),
                request == null ? null : request.databaseUsername(),
                request == null ? null : request.databasePassword(),
                request == null ? null : request.adminUsername(),
                request == null ? null : request.adminDisplayName(),
                request == null ? null : request.adminEmail(),
                request == null ? null : request.adminPassword(),
                request == null ? null : request.adminPasswordConfirmation(),
                request == null ? null : request.githubToken(),
                request == null ? null : request.m2mApiKey(),
                request == null ? null : request.m2mClientId(),
                request == null ? null : request.m2mClientSecret(),
                request == null ? null : request.databaseTestToken(),
                true);
    }

    /**
     * A {@code completeSelected} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a kezdeti beállítási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param dataDirectoryToken a művelet bemeneti {@code dataDirectoryToken} értéke
     * @param securityMode a művelet bemeneti {@code securityMode} értéke
     * @param databaseType a cél adatbázis típusa
     * @param databaseHost a külső adatbázis host neve vagy címe
     * @param databasePort a külső adatbázis portja
     * @param databaseName az adatbázis vagy Oracle service neve
     * @param databaseSchema az alkalmazás cél sémája
     * @param databaseUsername az adatbázis-felhasználó neve
     * @param databasePassword az adatbázis-felhasználó jelszava
     * @param adminUsername a művelet felhasználói kontextusa vagy felhasználóneve
     * @param adminDisplayName a feloldáshoz vagy azonosításhoz használt név
     * @param adminEmail a művelet bemeneti {@code adminEmail} értéke
     * @param adminPassword a művelet bemeneti {@code adminPassword} értéke
     * @param adminPasswordConfirmation a művelet bemeneti {@code adminPasswordConfirmation} értéke
     * @param githubToken az opcionális GitHub hozzáférési token
     * @param m2mApiKey a NAV-tól kapott opcionális, négy részből álló M2M API-kulcs
     * @param m2mClientId az opcionális M2M kliensazonosító
     * @param m2mClientSecret az opcionális M2M kliens titok
     * @param databaseTestToken a sikeres adatbázis-kapcsolati teszt rövid élettartamú tokenje
     * @param requireDatabaseTest jelzi, hogy a véglegesítéshez kötelező-e az adatbázis-kapcsolati teszt
     * @return a művelet eredményeként előállított egyedi elemek halmaza
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @Transactional
    public synchronized SetupResult completeSelected(
            String dataDirectoryToken,
            String securityMode,
            String databaseType,
            String databaseHost,
            String databasePort,
            String databaseName,
            String databaseSchema,
            String databaseUsername,
            String databasePassword,
            String adminUsername,
            String adminDisplayName,
            String adminEmail,
            String adminPassword,
            String adminPasswordConfirmation,
            String githubToken,
            String m2mApiKey,
            String m2mClientId,
            String m2mClientSecret,
            String databaseTestToken,
            boolean requireDatabaseTest) throws IOException {
        if(state.isCompleted()) throw new IllegalStateException("Az első indítási beállítás már befejeződött.");
        String mode=required(securityMode, "A működési mód megadása kötelező.").toUpperCase(Locale.ROOT);
        if(!List.of("STANDALONE","MULTI_USER").contains(mode)) throw new IllegalArgumentException("Nem támogatott működési mód: "+mode);
        validateOptionalIntegrationCredentials(m2mApiKey, m2mClientId, m2mClientSecret);

        // The filesystem path comes exclusively from a one-time server-side selection token.
        // HTTP request data is not accepted as a Path by this method.
        Path requestedDataDir = SetupDataDirectorySelectionVault.consume(dataDirectoryToken);
        createDirectories(requestedDataDir);
        Path dataDir=requestedDataDir.toRealPath(java.nio.file.LinkOption.NOFOLLOW_LINKS);
        Path currentDataDir = Path.of(defaultDataDirectory()).toAbsolutePath().normalize();
        boolean dataDirectoryChanged = !currentDataDir.equals(dataDir);
        DatabaseSetup databaseSetup = DatabaseSetup.resolve(databaseType, databaseHost, databasePort, databaseName,
                databaseSchema, databaseUsername, databasePassword, dataDir);
        if (requireDatabaseTest && !SetupDatabaseConnectionTestVault.matches(databaseTestToken, databaseSetup.fingerprint())) {
            throw new IllegalArgumentException("A beállítások mentése előtt sikeres adatbázis-kapcsolati teszt szükséges az aktuális kapcsolati adatokkal.");
        }

        InstallerIntegrationCredentials installerCredentials = readPendingInstallerIntegrationCredentials(currentDataDir);
        String effectiveAdminUsername = preferRequestValue(adminUsername, installerCredentials.adminUsername());
        String effectiveAdminDisplayName = preferRequestValue(adminDisplayName, installerCredentials.adminDisplayName());
        String effectiveAdminEmail = preferRequestValue(adminEmail, installerCredentials.adminEmail());
        String effectiveAdminPassword = preferSecretValue(adminPassword, installerCredentials.adminPassword());
        String effectiveAdminPasswordConfirmation = StringUtils.hasText(adminPassword) ? adminPasswordConfirmation : effectiveAdminPassword;
        String effectiveGithubToken = preferRequestValue(githubToken, installerCredentials.githubToken());
        String effectiveM2mApiKey = preferRequestValue(m2mApiKey, installerCredentials.m2mApiKey());
        String effectiveM2mClientId = preferRequestValue(m2mClientId, installerCredentials.m2mClientId());
        String effectiveM2mClientSecret = preferRequestValue(m2mClientSecret, installerCredentials.m2mClientSecret());
        validateSetupAdmin(effectiveAdminUsername, effectiveAdminPassword, effectiveAdminPasswordConfirmation);
        validateOptionalIntegrationCredentials(effectiveM2mApiKey, effectiveM2mClientId, effectiveM2mClientSecret);

        boolean databaseChanged = !databaseSetup.matches(environment);
        if (dataDirectoryChanged || databaseChanged) {
            movePendingInstallerIntegrationCredentials(currentDataDir, dataDir);
            storePendingSetupCredentials(dataDir, effectiveAdminUsername, effectiveAdminDisplayName, effectiveAdminEmail, effectiveAdminPassword,
                    effectiveGithubToken, effectiveM2mApiKey, effectiveM2mClientId, effectiveM2mClientSecret);
            Path selectedFile = bootstrapFile(dataDir);
            storeBootstrap(selectedFile, dataDir, mode, false, databaseSetup);
            storeBootstrapLocator(selectedFile);
            audit.log("INITIAL_SETUP_BOOTSTRAP_SELECTED", effectiveAdminUsername, "SUCCESS",
                    "Bootstrap cél kiválasztva, újraindítás szükséges. databaseType=" + databaseSetup.type() + ", dataDirectory=" + dataDir);
            if (requireDatabaseTest) {
                SetupDatabaseConnectionTestVault.revoke(databaseTestToken);
            }
            return new SetupResult(false, true, false, "BOOTSTRAP_RESTART",
                    "Az adatkönyvtár és az adatbázis-beállítások mentése sikeres. Indítsa újra az alkalmazást; a kezdeti beállítás az újraindítás után automatikusan befejeződik.");
        }

        if(users.list().isEmpty())
            users.createInitialAdmin(new UserSaveRequest(effectiveAdminUsername, effectiveAdminDisplayName, effectiveAdminEmail, true, false, effectiveAdminPassword, java.util.Set.of("ADMIN")));
        Path configuredBootstrap = existingConfiguredBootstrapFile(dataDir);
        Path file;
        if (configuredBootstrap != null) {
            file = configuredBootstrap;
            // A Windows installer bootstrapja csak első indítási seed. A sikeres setup
            // ugyanazzal a kanonikus íróval véglegesíti, mint a tisztán webes setup,
            // így nem maradhatnak ki kötelező BOOTSTRAP katalóguskulcsok.
            storeBootstrap(file, dataDir, mode, true, databaseSetup);
        } else {
            file = bootstrapFile(dataDir);
            storeBootstrap(file, dataDir, mode, true, databaseSetup);
        }
        storeBootstrapLocator(file);
        initializeDatabaseConfigurationCatalog();
        initializeDirectoryConfiguration(dataDir);
        saveOptionalIntegrationCredentials(effectiveAdminUsername, effectiveGithubToken, effectiveM2mApiKey, effectiveM2mClientId, effectiveM2mClientSecret);
        deletePendingInstallerIntegrationCredentials(dataDir);
        saveDatabaseConfiguration("nav.xsdparsertool.setup.completed", "true");
        state.markCompleted();
        if (requireDatabaseTest) {
            SetupDatabaseConnectionTestVault.revoke(databaseTestToken);
        }
        audit.log("INITIAL_SETUP_COMPLETED", effectiveAdminUsername, "SUCCESS", "Első indítási beállítás befejezve. mode="+mode+", dataDirectory="+dataDir);
        return new SetupResult(true, true, false, "COMPLETED",
                "A beállítások mentése sikeres. Indítsa újra az alkalmazást.");
    }

    /**
     * Ellenőrzi, hogy a setupban megadott adatbázis-kapcsolat ténylegesen felépíthető-e.
     *
     * <p>A sikeres teszt egy rövid élettartamú szerveroldali tokent ad vissza. A végleges setup
     * csak akkor fogadja el ezt a tokent, ha az adatbázis-paraméterek azóta nem változtak.</p>
     *
     * @param dataDirectoryToken a biztonságosan kiválasztott adatkönyvtár egyszer használatos tokenje
     * @param databaseType az adatbázis típusa
     * @param databaseHost a külső adatbázis hostja
     * @param databasePort a külső adatbázis portja
     * @param databaseName az adatbázis vagy service neve
     * @param databaseSchema a cél séma
     * @param databaseUsername az adatbázis-felhasználó
     * @param databasePassword az adatbázis-jelszó
     * @return a sikeres kapcsolatteszt eredménye és a véglegesítéshez szükséges token
     * @throws IOException ha az adatkönyvtár nem készíthető elő
     */
    public DatabaseConnectionTestResult testDatabaseConnection(
            String dataDirectoryToken,
            String databaseType,
            String databaseHost,
            String databasePort,
            String databaseName,
            String databaseSchema,
            String databaseUsername,
            String databasePassword) throws IOException {
        Path requestedDataDir = SetupDataDirectorySelectionVault.consume(dataDirectoryToken);
        createDirectories(requestedDataDir);
        Path dataDir = requestedDataDir.toRealPath(java.nio.file.LinkOption.NOFOLLOW_LINKS);
        DatabaseSetup databaseSetup = DatabaseSetup.resolve(databaseType, databaseHost, databasePort, databaseName,
                databaseSchema, databaseUsername, databasePassword, dataDir);
        try {
            Class.forName(databaseSetup.driver());
            Properties connectionProperties = new Properties();
            connectionProperties.put("user", databaseSetup.username());
            connectionProperties.put("password", databaseSetup.password());
            try (Connection connection = DriverManager.getConnection(databaseSetup.url(), connectionProperties)) {
                if (!connection.isValid(5)) {
                    throw new IllegalStateException("Az adatbázis-kapcsolat létrejött, de az érvényességi ellenőrzés sikertelen.");
                }
                String productName = connection.getMetaData().getDatabaseProductName();
                String productVersion = connection.getMetaData().getDatabaseProductVersion();
                String token = SetupDatabaseConnectionTestVault.issue(databaseSetup.fingerprint());
                return new DatabaseConnectionTestResult(token, productName, productVersion);
            }
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Az adatbázis JDBC drivere nem érhető el: " + databaseSetup.driver(), ex);
        } catch (SQLException ex) {
            throw new IllegalStateException("Az adatbázis-kapcsolat sikertelen: " + ex.getMessage(), ex);
        }
    }

    /** Jelzi, hogy az újraindítás utáni automatikus setup-véglegesítés még függőben van-e. */
    public boolean hasPendingSetupCompletion() {
        if (state.isCompleted()) {
            return false;
        }
        String dataDirectory = defaultDataDirectory();
        if (!StringUtils.hasText(dataDirectory)) {
            return false;
        }
        return Files.isRegularFile(pendingInstallerIntegrationFile(Path.of(dataDirectory).toAbsolutePath().normalize()));
    }

    /** A legutóbbi automatikus pending setup-véglegesítés felhasználónak is megjeleníthető hibája. */
    public String pendingSetupCompletionError() {
        return pendingCompletionError;
    }

    /** A setup adatbázis-kapcsolati tesztjének eredménye. */
    public record DatabaseConnectionTestResult(String token, String productName, String productVersion) {
    }

    /**
     * Külső adatbázis vagy adatkönyvtár váltása után, az első újraindításkor automatikusan
     * befejezi a setup második fázisát az egyszer használatos handoff fájlból.
     *
     * <p>A véglegesítés csak akkor történik meg, ha a futó datasource és a bootstrapban
     * tárolt adatbázis-beállítások teljesen megegyeznek. Eltérés esetén a metódus nem
     * módosít semmit, így a setup képernyő helyreállítási lehetőségként elérhető marad.</p>
     *
     * @return {@code true}, ha egy pending setup sikeresen automatikusan befejeződött
     * @throws IOException ha a handoff vagy bootstrap fájl kezelése sikertelen
     */
    @Transactional
    public synchronized boolean completePendingAfterRestart() throws IOException {
        pendingCompletionError = "";
        if (state.isCompleted()) {
            return false;
        }
        Path dataDir = Path.of(defaultDataDirectory()).toAbsolutePath().normalize();
        if (!Files.isRegularFile(pendingInstallerIntegrationFile(dataDir))) {
            return false;
        }

        try {
            String type = environment.getProperty("nav.xsdparsertool.database.type", "H2").toUpperCase(Locale.ROOT);
            String url = environment.getProperty("spring.datasource.url", "");
            DatabaseLocation location = DatabaseLocation.parse(type, url);

            // Az alkalmazás ezen a ponton már sikeresen felépítette a tényleges datasource-t,
            // ezért a második fázist az aktív Spring Environment értékeiből kell véglegesíteni.
            // A korábbi újra-rekonstrukció + matches() ellenőrzés normalizációs eltérés esetén
            // némán false-szal tért vissza, miközben a handoff megmaradt és a setup UI örökké várt.
            String dataDirectoryToken = SetupDataDirectorySelectionVault.issue(dataDir.toString());
            SetupResult result = completeSelected(
                    dataDirectoryToken,
                    currentSecurityMode(),
                    type,
                    location.host(),
                    location.port(),
                    location.databaseName(),
                    environment.getProperty("nav.xsdparsertool.database.schema", ""),
                    environment.getProperty("spring.datasource.username", ""),
                    environment.getProperty("spring.datasource.password", ""),
                    "", "", "", "", "",
                    "", "", "", "",
                    null, false);
            if (!result.completed()) {
                throw new IllegalStateException("A pending setup automatikus véglegesítése nem jutott COMPLETED állapotba: " + result.phase());
            }
            return true;
        } catch (IOException | RuntimeException ex) {
            pendingCompletionError = "Az automatikus setup-véglegesítés sikertelen. Ellenőrizze az alkalmazás naplóját.";
            throw ex;
        }
    }

    private static final String INSTALLER_INTEGRATION_HANDOFF = "setup-integrations.properties";

    /**
     * A webes setupban megadott érték elsőbbséget élvez; üres mező esetén a Windows telepítő
     * egyszer használatos átadási fájljából származó érték kerül felhasználásra.
     */
    private String preferRequestValue(String requestValue, String installerValue) {
        return StringUtils.hasText(requestValue) ? requestValue.trim() : installerValue;
    }

    private String preferSecretValue(String requestValue, String installerValue) {
        return StringUtils.hasText(requestValue) ? requestValue : installerValue;
    }

    /**
     * Beolvassa a Windows telepítő egyszer használatos integrációs adatait. A fájl tartalmát
     * szándékosan nem naplózza, mert titkos értékeket is tartalmazhat.
     */
    private InstallerIntegrationCredentials readPendingInstallerIntegrationCredentials(Path dataDir) throws IOException {
        Path file = pendingInstallerIntegrationFile(dataDir);
        if (!Files.isRegularFile(file)) {
            return InstallerIntegrationCredentials.empty();
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line == null || line.isBlank() || line.startsWith("#")) {
                continue;
            }
            int separator = line.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            values.put(line.substring(0, separator).trim(), line.substring(separator + 1));
        }
        return new InstallerIntegrationCredentials(
                values.getOrDefault("githubToken", ""),
                values.getOrDefault("m2mApiKey", ""),
                values.getOrDefault("m2mClientId", ""),
                values.getOrDefault("m2mClientSecret", ""),
                values.getOrDefault("adminUsername", ""),
                values.getOrDefault("adminDisplayName", ""),
                values.getOrDefault("adminEmail", ""),
                values.getOrDefault("adminPassword", ""));
    }

    /**
     * Ha a webes első beállítás másik adatkönyvtárat választ, az installer egyszer használatos
     * átadási fájlját is áthelyezi az új konfigurációs könyvtárba.
     */
    private void movePendingInstallerIntegrationCredentials(Path sourceDataDir, Path targetDataDir) throws IOException {
        Path source = pendingInstallerIntegrationFile(sourceDataDir);
        if (!Files.isRegularFile(source)) {
            return;
        }
        Path target = pendingInstallerIntegrationFile(targetDataDir);
        Files.createDirectories(target.getParent());
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * A kétfázisú setup újraindítása előtt privát ideiglenes fájlban megőrzi a még szükséges
     * admin- és integrációs adatokat. A sikeres setup ezt a fájlt törli.
     */
    private void storePendingSetupCredentials(Path dataDir, String adminUsername, String adminDisplayName,
                                              String adminEmail, String adminPassword, String githubToken,
                                              String m2mApiKey, String m2mClientId, String m2mClientSecret) throws IOException {
        InstallerIntegrationCredentials existing = readPendingInstallerIntegrationCredentials(dataDir);
        Map<String, String> values = new LinkedHashMap<>();
        values.put("adminUsername", preferRequestValue(adminUsername, existing.adminUsername()));
        values.put("adminDisplayName", preferRequestValue(adminDisplayName, existing.adminDisplayName()));
        values.put("adminEmail", preferRequestValue(adminEmail, existing.adminEmail()));
        values.put("adminPassword", preferSecretValue(adminPassword, existing.adminPassword()));
        values.put("githubToken", preferRequestValue(githubToken, existing.githubToken()));
        values.put("m2mApiKey", preferRequestValue(m2mApiKey, existing.m2mApiKey()));
        values.put("m2mClientId", preferRequestValue(m2mClientId, existing.m2mClientId()));
        values.put("m2mClientSecret", preferRequestValue(m2mClientSecret, existing.m2mClientSecret()));
        Path file = pendingInstallerIntegrationFile(dataDir);
        Files.createDirectories(file.getParent());
        try (Writer writer = SecureFileOperations.newPrivateBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write("# Temporary setup handoff - deleted after successful setup\n");
            for (Map.Entry<String, String> entry : values.entrySet()) {
                writer.write(entry.getKey());
                writer.write('=');
                writer.write(entry.getValue() == null ? "" : entry.getValue());
                writer.write('\n');
            }
        }
    }

    private void deletePendingInstallerIntegrationCredentials(Path dataDir) throws IOException {
        Files.deleteIfExists(pendingInstallerIntegrationFile(dataDir));
    }

    private Path pendingInstallerIntegrationFile(Path dataDir) {
        return dataDir.resolve("config").resolve(INSTALLER_INTEGRATION_HANDOFF);
    }

    record InstallerIntegrationCredentials(String githubToken, String m2mApiKey, String m2mClientId, String m2mClientSecret,
                                           String adminUsername, String adminDisplayName, String adminEmail, String adminPassword) {
        static InstallerIntegrationCredentials empty() {
            return new InstallerIntegrationCredentials("", "", "", "", "", "", "", "");
        }
    }

    /**
     * Még bootstrap-váltás előtt ellenőrzi a setup admin kötelező mezőit és a 8 karakteres komplexitási szabályt.
     * @param username admin felhasználónév
     * @param password admin jelszó
     * @param confirmation admin jelszó megerősítése
     */
    private void validateSetupAdmin(String username, String password, String confirmation) {
        required(username, "A kezdő admin felhasználónevének megadása kötelező.");
        if (!StringUtils.hasText(password) || !password.equals(confirmation)) {
            throw new IllegalArgumentException("A kezdő admin jelszavai nem egyeznek.");
        }
        if (password.length() < 8
                || password.chars().noneMatch(Character::isLowerCase)
                || password.chars().noneMatch(Character::isUpperCase)
                || password.chars().noneMatch(Character::isDigit)
                || password.chars().noneMatch(ch -> !Character.isLetterOrDigit(ch))) {
            throw new IllegalArgumentException("A kezdő admin jelszava legalább 8 karakteres legyen, és tartalmazzon kisbetűt, nagybetűt, számot és speciális karaktert.");
        }
    }

    /**
     * Még a setup állapotváltozásai előtt ellenőrzi az opcionális M2M adatok formátumát.
     */
    private void validateOptionalIntegrationCredentials(String m2mApiKey, String m2mClientId, String m2mClientSecret) {
        if (StringUtils.hasText(m2mApiKey)) {
            parseM2mApiKey(m2mApiKey);
        }
        boolean hasClientId = StringUtils.hasText(m2mClientId);
        boolean hasClientSecret = StringUtils.hasText(m2mClientSecret);
        if (hasClientId != hasClientSecret) {
            throw new IllegalArgumentException("Az M2M Client ID és Client Secret csak együtt adható meg.");
        }
    }

    /**
     * Az első indítás során opcionálisan megadott GitHub- és M2M-hozzáférési adatokat
     * a központi rendszerkonfigurációba menti. Az érzékeny értékeket a meglévő
     * titkosított secret-tároló kezeli.
     */
    private void saveOptionalIntegrationCredentials(String username, String githubToken, String m2mApiKey,
                                                    String m2mClientId, String m2mClientSecret) throws IOException {
        Map<String, String> values = new LinkedHashMap<>();
        java.util.Set<String> confirmedSensitiveKeys = new java.util.LinkedHashSet<>();

        if (StringUtils.hasText(githubToken)) {
            values.put("nav.xsdparsertool.github-schema-updater.token", githubToken.trim());
            confirmedSensitiveKeys.add("nav.xsdparsertool.github-schema-updater.token");
        }

        if (StringUtils.hasText(m2mApiKey)) {
            M2mApiKeyParts parts = parseM2mApiKey(m2mApiKey);
            values.put("nav.m2m.auth.username", parts.userId());
            values.put("nav.m2m.auth.password", parts.password());
            values.put("nav.m2m.signature.key-first-part", parts.signatureKeyFirstPart());
            values.put("nav.m2m.signature.nonce", parts.nonce());
            confirmedSensitiveKeys.add("nav.m2m.auth.password");
            confirmedSensitiveKeys.add("nav.m2m.signature.key-first-part");
            confirmedSensitiveKeys.add("nav.m2m.signature.nonce");
        }

        boolean hasClientId = StringUtils.hasText(m2mClientId);
        boolean hasClientSecret = StringUtils.hasText(m2mClientSecret);
        if (hasClientId != hasClientSecret) {
            throw new IllegalArgumentException("Az M2M Client ID és Client Secret csak együtt adható meg.");
        }
        if (hasClientId) {
            values.put("nav.m2m.auth.client-id", m2mClientId.trim());
            values.put("nav.m2m.auth.client-secret", m2mClientSecret.trim());
            confirmedSensitiveKeys.add("nav.m2m.auth.client-secret");
        }

        if (!values.isEmpty()) {
            systemConfigurationService.save(values, confirmedSensitiveKeys, username);
        }
    }

    /**
     * A NAV által egy mezőben átadott M2M API-kulcsot a kötőjelek mentén négy részre bontja.
     * A kulcs formátuma: userId-password-aláírókulcsElsőFele-nonce.
     */
    static M2mApiKeyParts parseM2mApiKey(String apiKey) {
        String normalized = apiKey == null ? "" : apiKey.trim();
        String[] parts = normalized.split("-", -1);
        if (parts.length != 4) {
            throw new IllegalArgumentException("Az M2M API-kulcs formátuma hibás. A kulcsnak négy, kötőjellel elválasztott részből kell állnia.");
        }
        for (String part : parts) {
            if (!StringUtils.hasText(part)) {
                throw new IllegalArgumentException("Az M2M API-kulcs formátuma hibás. Egyik komponense sem lehet üres.");
            }
        }
        return new M2mApiKeyParts(parts[0].trim(), parts[1].trim(), parts[2].trim(), parts[3].trim());
    }

    record M2mApiKeyParts(String userId, String password, String signatureKeyFirstPart, String nonce) {}

    /**
     * A {@code bootstrapFile} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param dataDir a művelet bemeneti {@code dataDir} értéke
     * @return a művelet feldolgozási eredménye
     */
    private Path bootstrapFile(Path dataDir) {
        return dataDir.resolve("config").resolve("application-bootstrap.properties");
    }

    /**
     * Installer-managed installations provide an initial bootstrap seed. During first-run setup
     * that same file is selected as the canonical target, then finalized through the regular
     * web setup bootstrap writer so both installation paths produce the same configuration model.
     */
    private Path existingConfiguredBootstrapFile(Path dataDir) {
        String configured = environment.getProperty("nav.xsdparsertool.bootstrap-config-file");
        if (!StringUtils.hasText(configured)) {
            return null;
        }
        Path candidate = Path.of(configured.trim()).toAbsolutePath().normalize();
        Path configRoot = dataDir.resolve("config").toAbsolutePath().normalize();
        if (!candidate.startsWith(configRoot) || !ExceptionSafeOperations.isRegularFile(candidate)) {
            return null;
        }
        return candidate;
    }

    /**
     * A {@code storePropertiesPreservingFile} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param file a feldolgozásban részt vevő fájl vagy elérési út
     * @param properties a művelethez szükséges konfigurációs adatok
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private void storePropertiesPreservingFile(Path file, Properties properties) throws IOException {
        ExceptionSafeOperations.createDirectories(file.getParent());
        if (ExceptionSafeOperations.fileExists(file)) {
            SecureFileOperations.copyPrivate(file, file.resolveSibling(file.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
        }
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try (Writer writer = SecureFileOperations.newPrivateBufferedWriter(temporary, StandardCharsets.UTF_8)) {
            properties.store(writer, "M2M XML EDITOR bootstrap configuration");
        }
        SecureFileOperations.movePrivate(temporary, file, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * A {@code initializeDirectoryConfiguration} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A konfigurációs értékeket a web modul érvényes beállításaihoz igazítja, és az esetleges alapértelmezéseket csak a komponensben definiált szabályok szerint alkalmazza.</p>
     * @param dataDir a művelet bemeneti {@code dataDir} értéke
     */
    private void initializeDirectoryConfiguration(Path dataDir) {
        Map<String, Path> directorySettings = new LinkedHashMap<>();
        directorySettings.put("nav.xsdparsertool.paths.schema-dir", configuredDirectory(
                "nav.xsdparsertool.paths.schema-dir", dataDir.resolve("repo/xsd")));
        directorySettings.put("nav.xsdparsertool.paths.common-xsd-dir", configuredDirectory(
                "nav.xsdparsertool.paths.common-xsd-dir", dataDir.resolve("repo/xsd/common")));
        directorySettings.put("nav.xsdparsertool.paths.ui-model-dir", configuredDirectory(
                "nav.xsdparsertool.paths.ui-model-dir", dataDir.resolve("repo/uimodel")));
        directorySettings.put("nav.xsdparsertool.xpath-validator.xsl-root-dir", configuredDirectory(
                "nav.xsdparsertool.xpath-validator.xsl-root-dir", dataDir.resolve("repo/rule-xsl")));
        directorySettings.put("nav.xsdparsertool.xpath-validator.rule-root-dir", configuredDirectory(
                "nav.xsdparsertool.xpath-validator.rule-root-dir", dataDir.resolve("repo/xpath")));
        directorySettings.put("nav.xsdparsertool.xpath-validator.result-dir", dataDir.resolve("data/xpath/results"));
        directorySettings.put("nav.xsdparsertool.xml-file.upload-dir", dataDir.resolve("data/xml"));
        directorySettings.put("nav.xsdparsertool.xml-file.backup-dir", dataDir.resolve("backup"));
        directorySettings.put("nav.xsdparsertool.xml-file.archive-dir", dataDir.resolve("data/archive"));
        directorySettings.put("nav.xsdparsertool.xml-file.xml-index-dir", dataDir.resolve("data/xml-index"));
        directorySettings.put("nav.xsdparsertool.xml-file.server-import.root-dir", dataDir.resolve("data/import"));
        directorySettings.put("nav.xsdparsertool.xml-index.config-path", dataDir.resolve("config/xml-index-config.xml"));
        directorySettings.put("nav.m2m.storage-directory", dataDir.resolve("data/attachments"));
        directorySettings.forEach((key, path) -> saveDatabaseConfiguration(key, path.toAbsolutePath().normalize().toString()));
    }

    /**
     * A {@code initializeDatabaseConfigurationCatalog} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A konfigurációs értékeket a web modul érvényes beállításaihoz igazítja, és az esetleges alapértelmezéseket csak a komponensben definiált szabályok szerint alkalmazza.</p>
     */
    private void initializeDatabaseConfigurationCatalog() {
        for (ConfigurationCatalog.Spec spec : ConfigurationCatalog.ITEMS) {
            if (!"DATABASE".equals(spec.storage())) {
                continue;
            }
            SystemConfigurationEntity existing = RepositoryAccess.findById(configurations, spec.key()).orElse(null);
            if (existing == null) {
                saveDatabaseConfiguration(spec.key(), spec.sensitive() ? "" : spec.defaultValue());
            } else if (!spec.sensitive()
                    && (existing.getValue() == null || existing.getValue().isBlank())
                    && spec.defaultValue() != null && !spec.defaultValue().isBlank()) {
                saveDatabaseConfiguration(spec.key(), spec.defaultValue());
            }
        }
    }

    /**
     * A {@code configuredDirectory} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A konfigurációs értékeket a web modul érvényes beállításaihoz igazítja, és az esetleges alapértelmezéseket csak a komponensben definiált szabályok szerint alkalmazza.</p>
     * @param key a művelet bemeneti {@code key} értéke
     * @param fallback a művelet bemeneti {@code fallback} értéke
     * @return a művelet feldolgozási eredménye
     */
    private Path configuredDirectory(String key, Path fallback) {
        String configured = environment.getProperty(key);
        if (!StringUtils.hasText(configured)) {
            return fallback.toAbsolutePath().normalize();
        }
        return Path.of(configured.trim()).toAbsolutePath().normalize();
    }

    /**
     * A {@code saveDatabaseConfiguration} művelet létrehozza vagy tartósítja a kért állapotváltozást.
     *
     * <p>A konfigurációs értékeket a web modul érvényes beállításaihoz igazítja, és az esetleges alapértelmezéseket csak a komponensben definiált szabályok szerint alkalmazza.</p>
     * @param key a művelet bemeneti {@code key} értéke
     * @param value a művelet bemeneti {@code value} értéke
     */
    private void saveDatabaseConfiguration(String key, String value) {
        SystemConfigurationEntity entity = RepositoryAccess.findById(configurations, key).orElseGet(SystemConfigurationEntity::new);
        entity.setKey(key);
        entity.setValue(value);
        entity.setUpdatedBy("setup");
        entity.setUpdatedAt(java.time.Instant.now());
        configurations.save(entity);
        systemConfigurationService.bindRuntimeDatabaseValue(key, value);
    }

    /**
     * A {@code storeBootstrap} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a kezdeti beállítási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param file a feldolgozásban részt vevő fájl vagy elérési út
     * @param dataDir a művelet bemeneti {@code dataDir} értéke
     * @param mode a művelet bemeneti {@code mode} értéke
     * @param completed a művelet bemeneti {@code completed} értéke
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private void storeBootstrap(Path file, Path dataDir, String mode, boolean completed, DatabaseSetup databaseSetup) throws IOException {
        Properties existingProperties = loadExistingProperties(file);
        Properties properties = new Properties();
        for (ConfigurationCatalog.Spec spec : ConfigurationCatalog.ITEMS) {
            if (!"BOOTSTRAP".equals(spec.storage())) {
                continue;
            }
            String configuredValue = environment.getProperty(spec.key());
            if (!StringUtils.hasText(configuredValue)) {
                configuredValue = existingProperties.getProperty(spec.key());
            }
            if (configuredValue == null && spec.defaultValue() != null) {
                configuredValue = spec.defaultValue();
            }
            if (configuredValue != null) {
                properties.put(spec.key(), configuredValue);
            }
        }
        properties.put("nav.xsdparsertool.setup.completed", Boolean.toString(completed));
        properties.put("nav.xsdparsertool.data-directory", dataDir.toString());
        properties.put("nav.xsdparsertool.bootstrap-config-file", file.toString());
        properties.put("nav.xsdparsertool.security.mode", mode);
        properties.put("nav.xsdparsertool.security.standalone.username", "local-user");
        properties.put("app.data.dir", dataDir.toAbsolutePath().normalize().toString().replace('\\', '/'));
        properties.put("logging.file.name", "${app.data.dir}/logs/app.log");
        databaseSetup.apply(properties);
        properties.put("spring.flyway.enabled", "true");
        properties.put("spring.flyway.encoding", "UTF-8");
        properties.put("spring.flyway.baseline-on-migrate", "true");
        Path masterKeyFile = dataDir.resolve("config/master.key");
        properties.put("m2m.xml.editor.secret.master-key-file", masterKeyFile.toString());
        storePropertiesPreservingFile(file, properties);
    }


    /**
     * A {@code loadExistingProperties} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a kezdeti beállítási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param file a feldolgozásban részt vevő fájl vagy elérési út
     * @return a feloldott vagy lekért érték
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private Properties loadExistingProperties(Path file) throws IOException {
        Properties properties = new Properties();
        if (!ExceptionSafeOperations.isRegularFile(file)) {
            return properties;
        }
        try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }

    /**
     * A {@code storeBootstrapLocator} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a kezdeti beállítási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param bootstrapFile a feldolgozásban részt vevő fájl vagy elérési út
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private void storeBootstrapLocator(Path bootstrapFile) throws IOException {
        Path locator = BootstrapDefaultsEnvironmentPostProcessor.locatorFile();
        ExceptionSafeOperations.createDirectories(locator.getParent());
        Properties locatorProperties = new Properties();
        locatorProperties.put("bootstrap.file", bootstrapFile.toAbsolutePath().normalize().toString());
        try (Writer writer = SecureFileOperations.newPrivateBufferedWriter(locator, StandardCharsets.UTF_8)) {
            locatorProperties.store(writer, "M2M XML EDITOR bootstrap location");
        }
    }

    /**
     * A {@code createDirectories} művelet létrehozza vagy tartósítja a kért állapotváltozást.
     *
     * <p>A művelet a kezdeti beállítási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param root a művelet bemeneti {@code root} értéke
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private void createDirectories(Path root) throws IOException { for(String n:new String[]{"config","database","logs","certificates","backup","data/xml","data/import","data/attachments","data/exports","data/archive","data/xml-index","data/xpath/results","repo/xsd","repo/xsd/common","repo/uimodel","repo/xpath","repo/rule-xsl"}) ExceptionSafeOperations.createDirectories(root.resolve(n)); }
    /**
     * A {@code required} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a kezdeti beállítási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param v a művelet bemeneti {@code v} értéke
     * @param m a művelet bemeneti {@code m} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String required(String v,String m){ if(v==null||v.isBlank()) throw new IllegalArgumentException(m); return v.trim(); }
    /** Visszaadja az aktuális security módot a setup kliens számára. */
    public String currentSecurityMode() {
        return environment.getProperty("nav.xsdparsertool.security.mode", "MULTI_USER");
    }

    /** Visszaadja a Windows telepítő egyszer használatos admin előbeállításának nem titkos metaadatait. */
    public Map<String, Object> currentInstallerPreset() {
        try {
            Path dataDir = Path.of(defaultDataDirectory()).toAbsolutePath().normalize();
            InstallerIntegrationCredentials preset = readPendingInstallerIntegrationCredentials(dataDir);
            return Map.of(
                    "adminUsername", preset.adminUsername(),
                    "adminDisplayName", preset.adminDisplayName(),
                    "adminEmail", preset.adminEmail(),
                    "hasAdminPassword", StringUtils.hasText(preset.adminPassword()));
        } catch (Exception ex) {
            return Map.of("adminUsername", "", "adminDisplayName", "", "adminEmail", "", "hasAdminPassword", false);
        }
    }

    /** Visszaadja a setup képernyőn előtöltendő adatbázis-paramétereket. */
    public Map<String, String> currentDatabaseSetup() {
        String type = environment.getProperty("nav.xsdparsertool.database.type", "H2").toUpperCase(Locale.ROOT);
        String url = environment.getProperty("spring.datasource.url", "");
        DatabaseLocation location = DatabaseLocation.parse(type, url);
        return Map.of(
                "type", type,
                "url", url,
                "host", location.host(),
                "port", location.port(),
                "databaseName", location.databaseName(),
                "username", environment.getProperty("spring.datasource.username", "sa"),
                "schema", environment.getProperty("nav.xsdparsertool.database.schema", ""));
    }

    /** A JDBC URL setup-képernyőn szükséges hordozható kapcsolati részeit bontja ki. */
    record DatabaseLocation(String host, String port, String databaseName) {
        static DatabaseLocation parse(String type, String url) {
            if (!StringUtils.hasText(url) || "H2".equalsIgnoreCase(type)) {
                return new DatabaseLocation("", "", "");
            }
            String normalizedType = StringUtils.hasText(type) ? type.toUpperCase(Locale.ROOT) : "";
            String prefix = switch (normalizedType) {
                case "MYSQL" -> "jdbc:mysql://";
                case "POSTGRESQL" -> "jdbc:postgresql://";
                case "ORACLE" -> "jdbc:oracle:thin:@";
                default -> "";
            };
            if (prefix.isEmpty() || !url.startsWith(prefix)) {
                return new DatabaseLocation("", "", "");
            }
            String remainder = url.substring(prefix.length());
            int query = remainder.indexOf('?');
            if (query >= 0) remainder = remainder.substring(0, query);
            int slash = remainder.indexOf('/');
            String endpoint = slash >= 0 ? remainder.substring(0, slash) : remainder;
            String database = slash >= 0 ? remainder.substring(slash + 1) : "";
            int colon = endpoint.lastIndexOf(':');
            String host = colon > 0 ? endpoint.substring(0, colon) : endpoint;
            String port = colon > 0 ? endpoint.substring(colon + 1) : "";
            return new DatabaseLocation(host, port, database);
        }
    }

    /** A setuphoz használt cél-adatbázis konzisztens bootstrap konfigurációja. */
    record DatabaseSetup(String type, String url, String username, String password, String driver, String dialect, String schema,
                         String flywayLocation, boolean h2Console) {
        static DatabaseSetup resolve(String typeValue, String hostValue, String portValue, String nameValue,
                                     String schemaValue, String usernameValue, String passwordValue, Path dataDir) {
            String type = StringUtils.hasText(typeValue) ? typeValue.trim().toUpperCase(Locale.ROOT) : "H2";
            String host = StringUtils.hasText(hostValue) ? hostValue.trim() : "localhost";
            String name = StringUtils.hasText(nameValue) ? nameValue.trim() : "nav_xsd_parser_tool";
            String username = StringUtils.hasText(usernameValue) ? usernameValue.trim() : ("H2".equals(type) ? "sa" : "nav_user");
            String password = passwordValue == null ? "" : passwordValue;
            return switch (type) {
                case "H2" -> new DatabaseSetup("H2",
                        "jdbc:h2:file:" + dataDir.resolve("database/schema-explorer").toAbsolutePath().normalize().toString().replace('\\', '/') + ";AUTO_SERVER=TRUE",
                        username, password, "org.h2.Driver", "org.hibernate.dialect.H2Dialect", "PUBLIC", "classpath:db/migration/H2", true);
                case "MYSQL" -> new DatabaseSetup("MYSQL",
                        "jdbc:mysql://" + host + ":" + portOrDefault(portValue, "3306") + "/" + name
                                + "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Europe/Budapest&allowPublicKeyRetrieval=true&useSSL=false",
                        requireValue(username, "A MySQL felhasználónév megadása kötelező."), password, "com.mysql.cj.jdbc.Driver",
                        "org.hibernate.dialect.MySQLDialect", StringUtils.hasText(schemaValue) ? schemaValue.trim() : name,
                        "classpath:db/migration/MYSQL", false);
                case "POSTGRESQL" -> new DatabaseSetup("POSTGRESQL",
                        "jdbc:postgresql://" + host + ":" + portOrDefault(portValue, "5432") + "/" + name + "?currentSchema="
                                + (StringUtils.hasText(schemaValue) ? schemaValue.trim() : "public"),
                        requireValue(username, "A PostgreSQL felhasználónév megadása kötelező."), password, "org.postgresql.Driver",
                        "org.hibernate.dialect.PostgreSQLDialect", StringUtils.hasText(schemaValue) ? schemaValue.trim() : "public",
                        "classpath:db/migration/POSTGRESQL", false);
                case "ORACLE" -> new DatabaseSetup("ORACLE",
                        "jdbc:oracle:thin:@" + host + ":" + portOrDefault(portValue, "1521") + "/" + name,
                        requireValue(username, "Az Oracle felhasználónév megadása kötelező."), password, "oracle.jdbc.OracleDriver",
                        "org.hibernate.dialect.OracleDialect", StringUtils.hasText(schemaValue) ? schemaValue.trim() : username.toUpperCase(Locale.ROOT),
                        "classpath:db/migration/ORACLE", false);
                default -> throw new IllegalArgumentException("Nem támogatott adatbázistípus: " + type);
            };
        }

        private static String requireValue(String value, String message) {
            if (!StringUtils.hasText(value)) throw new IllegalArgumentException(message);
            return value.trim();
        }

        private static String portOrDefault(String value, String defaultValue) {
            String port = StringUtils.hasText(value) ? value.trim() : defaultValue;
            try {
                int parsed = Integer.parseInt(port);
                if (parsed < 1 || parsed > 65535) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Az adatbázis-port 1 és 65535 közötti egész szám lehet.");
            }
            return port;
        }

        String fingerprint() {
            return String.join("\u001f", type, url, username, password, driver, dialect, schema, flywayLocation, Boolean.toString(h2Console));
        }

        boolean matches(Environment environment) {
            boolean passwordMatches = !StringUtils.hasText(password)
                    || Objects.equals(password, environment.getProperty("spring.datasource.password", ""));
            return type.equalsIgnoreCase(environment.getProperty("nav.xsdparsertool.database.type", "H2"))
                    && Objects.equals(url, environment.getProperty("spring.datasource.url", ""))
                    && Objects.equals(username, environment.getProperty("spring.datasource.username", ""))
                    && passwordMatches
                    && Objects.equals(driver, environment.getProperty("spring.datasource.driver-class-name", ""))
                    && Objects.equals(dialect, environment.getProperty("spring.jpa.database-platform", ""))
                    && Objects.equals(schema, environment.getProperty("nav.xsdparsertool.database.schema", ""))
                    && Objects.equals(flywayLocation, environment.getProperty("spring.flyway.locations", ""))
                    && (!"H2".equalsIgnoreCase(type)
                    || h2Console == environment.getProperty("spring.h2.console.enabled", Boolean.class, false));
        }

        void apply(Properties properties) {
            properties.put("nav.xsdparsertool.database.type", type);
            properties.put("nav.xsdparsertool.database.schema", schema);
            properties.put("nav.xsdparsertool.database.encoding", "UTF-8");
            properties.put("spring.datasource.url", url);
            properties.put("spring.datasource.username", username);
            properties.put("spring.datasource.password", password);
            properties.put("spring.datasource.driver-class-name", driver);
            properties.put("spring.jpa.database-platform", dialect);
            if ("ORACLE".equalsIgnoreCase(type)) {
                // Oracle DATE/TIMESTAMP oszlopokhoz az Instant értékeket JDBC TIMESTAMP-ként
                // kezeljük. Ez elkerüli, hogy Hibernate OffsetDateTime-ként próbálja kiolvasni
                // a timezone nélküli Oracle TIMESTAMP értékeket (ORA-18716).
                properties.put("spring.jpa.properties.hibernate.type.preferred_instant_jdbc_type", "TIMESTAMP");
            } else {
                properties.remove("spring.jpa.properties.hibernate.type.preferred_instant_jdbc_type");
            }
            properties.put("spring.flyway.locations", flywayLocation);
            properties.put("spring.h2.console.enabled", Boolean.toString(h2Console));
            if (h2Console) properties.put("spring.h2.console.path", "/h2-console");
        }
    }

}
