package hu.gov.nav.xsdparsertool.web.systemconfig.service;

import hu.gov.nav.xsdparsertool.core.support.SecureFileOperations;
import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import hu.gov.nav.xsdparsertool.web.support.RepositoryAccess;

import hu.gov.nav.xsdparsertool.web.systemconfig.dto.ConfigurationItemDto;
import hu.gov.nav.xsdparsertool.web.systemconfig.dto.ConfigurationSaveResponse;
import hu.gov.nav.xsdparsertool.web.systemconfig.entity.SystemConfigurationEntity;
import hu.gov.nav.xsdparsertool.web.systemconfig.repository.SystemConfigurationRepository;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import hu.gov.nav.xsdparsertool.web.secret.service.SystemSecretService;
import hu.gov.nav.xsdparsertool.web.audit.AuditLogService;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A kapcsolódó webes üzleti vagy alkalmazási folyamatokat összefogó szolgáltatás.
 *
 * <p>A {@code SystemConfigurationService} osztály a web modul alkalmazási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Service
public class SystemConfigurationService {
    private final SystemConfigurationRepository repository;
    private final Environment environment;
    private final SystemSecretService secrets;
    private final AuditLogService audit;
    private final hu.gov.nav.xsdparsertool.web.secret.service.RuntimeSecretBindingService runtimeSecretBindingService;

    /**
     * Létrehozza a {@code SystemConfigurationService} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param repository a művelet bemeneti {@code repository} értéke
     * @param environment a művelet bemeneti {@code environment} értéke
     * @param secrets a művelet bemeneti {@code secrets} értéke
     * @param audit a művelet bemeneti {@code audit} értéke
     * @param runtimeSecretBindingService a művelet bemeneti {@code runtimeSecretBindingService} értéke
     */
    public SystemConfigurationService(SystemConfigurationRepository repository, Environment environment,
                                      SystemSecretService secrets, AuditLogService audit,
                                      hu.gov.nav.xsdparsertool.web.secret.service.RuntimeSecretBindingService runtimeSecretBindingService) {
        this.repository = repository;
        this.environment = environment;
        this.secrets = secrets;
        this.audit = audit;
        this.runtimeSecretBindingService = runtimeSecretBindingService;
    }

    /**
     * A {@code list} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a művelet eredményeként előállított elemek listája
     */
    public List<ConfigurationItemDto> list() {
        Map<String, SystemConfigurationEntity> databaseValues = RepositoryAccess.findAll(repository).stream()
                .collect(Collectors.toMap(SystemConfigurationEntity::getKey, Function.identity()));
        Properties bootstrap = loadBootstrapProperties();
        return ConfigurationCatalog.ITEMS.stream().map(spec -> {
            String value;
            String source;
            if ("DATABASE".equals(spec.storage()) && isEncryptedSecret(spec.key()) && secrets.exists(spec.key())) {
                value = "";
                source = "ENCRYPTED_DATABASE";
            } else if ("DATABASE".equals(spec.storage())
                    && databaseValues.containsKey(spec.key())
                    && StringUtils.hasText(databaseValues.get(spec.key()).getValue())) {
                value = databaseValues.get(spec.key()).getValue();
                source = "DATABASE";
            } else if ("BOOTSTRAP".equals(spec.storage()) && bootstrap.containsKey(spec.key())) {
                value = bootstrap.getProperty(spec.key());
                source = "BOOTSTRAP_FILE";
            } else if (environment.containsProperty(spec.key())) {
                value = environment.getProperty(spec.key(), "");
                source = "ENVIRONMENT";
            } else {
                value = "";
                source = "MISSING";
            }
            boolean missing = "MISSING".equals(source)
                    || (!"ENCRYPTED_DATABASE".equals(source) && !StringUtils.hasText(value));
            boolean databasePersisted = "DATABASE".equals(spec.storage())
                    && (isEncryptedSecret(spec.key())
                    ? secrets.exists(spec.key())
                    : databaseValues.containsKey(spec.key()));
            if (spec.sensitive() && StringUtils.hasText(value)) value = "";
            return new ConfigurationItemDto(spec.key(), spec.label(), spec.description(), spec.category(),
                    spec.storage(), spec.type(), value, spec.defaultValue(), source, spec.sensitive(),
                    spec.restartRequired(), spec.advanced(), spec.required(), missing, databasePersisted, spec.options());
        }).toList();
    }

    /**
     * A {@code save} művelet létrehozza vagy tartósítja a kért állapotváltozást.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param values a művelet bemeneti {@code values} értéke
     * @param confirmedSensitiveKeys a feldolgozandó elemek kollekciója
     * @param username a művelet felhasználói kontextusa vagy felhasználóneve
     * @return a művelet feldolgozási eredménye
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @Transactional
    public ConfigurationSaveResponse save(Map<String, String> values, Set<String> confirmedSensitiveKeys, String username) throws IOException {
        if (values == null) values = Map.of();
        if (confirmedSensitiveKeys == null) confirmedSensitiveKeys = Set.of();
        Map<String, ConfigurationCatalog.Spec> specs = ConfigurationCatalog.ITEMS.stream()
                .collect(Collectors.toMap(ConfigurationCatalog.Spec::key, Function.identity()));
        Properties bootstrap = loadBootstrapProperties();
        int dbCount = 0;
        int bootstrapCount = 0;
        boolean restart = false;
        List<String> changed = new ArrayList<>();

        for (Map.Entry<String, String> entry : values.entrySet()) {
            ConfigurationCatalog.Spec spec = specs.get(entry.getKey());
            if (spec == null) continue;
            String rawValue = entry.getValue() == null ? "" : entry.getValue();
            String newValue = spec.sensitive() ? rawValue : rawValue.trim();
            if (spec.sensitive() && !confirmedSensitiveKeys.contains(spec.key())) continue;
            if (spec.sensitive() && newValue.isBlank()) continue;
            validate(spec, newValue);
            if ("BOOTSTRAP".equals(spec.storage())) {
                String old = bootstrap.containsKey(spec.key()) ? bootstrap.getProperty(spec.key()) : environment.getProperty(spec.key());
                if (!Objects.equals(old, newValue)) {
                    bootstrap.put(spec.key(), newValue);
                    bootstrapCount++;
                    changed.add(spec.key());
                    restart |= spec.restartRequired();
                }
            } else if (isEncryptedSecret(spec.key())) {
                secrets.save(spec.key(), newValue, username);
                dbCount++;
                changed.add(spec.key());
                restart |= spec.restartRequired();
            } else {
                SystemConfigurationEntity entity = RepositoryAccess.findById(repository, spec.key()).orElseGet(SystemConfigurationEntity::new);
                String old = entity.getValue();
                if (!Objects.equals(old, newValue)) {
                    entity.setKey(spec.key());
                    entity.setValue(newValue);
                    entity.setUpdatedAt(Instant.now());
                    entity.setUpdatedBy(username);
                    repository.save(entity);
                    bindRuntimeDatabaseValue(spec.key(), newValue);
                    dbCount++;
                    changed.add(spec.key());
                    restart |= spec.restartRequired();
                }
            }
        }
        if (values.containsKey("nav.xsdparsertool.database.type")) {
            harmonizeDatabaseBootstrap(bootstrap);
            bootstrapCount++;
            restart = true;
        }
        if (bootstrapCount > 0) storeBootstrapProperties(bootstrap);
        if (changed.stream().anyMatch(ConfigurationCatalog.ENCRYPTED_SECRET_KEYS::contains)) {
            runtimeSecretBindingService.refresh();
        }
        if (!changed.isEmpty()) audit.log("SYSTEM_CONFIGURATION_UPDATE", username, "SUCCESS", "Módosított kulcsok: " + String.join(",", changed));
        return new ConfigurationSaveResponse(dbCount, bootstrapCount, restart, changed, bootstrapPath().toString());
    }

    /**
     * A {@code isEncryptedSecret} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param key a művelet bemeneti {@code key} értéke
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    private boolean isEncryptedSecret(String key) {
        return ConfigurationCatalog.ENCRYPTED_SECRET_KEYS.contains(key);
    }

    /**
     * A {@code reset} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param keys a feldolgozandó elemek kollekciója
     * @param username a művelet felhasználói kontextusa vagy felhasználóneve
     * @return a művelet feldolgozási eredménye
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @Transactional
    public ConfigurationSaveResponse reset(List<String> keys, String username) throws IOException {
        if (keys == null || keys.isEmpty()) return new ConfigurationSaveResponse(0, 0, false, List.of(), bootstrapPath().toString());
        Map<String, ConfigurationCatalog.Spec> specs = ConfigurationCatalog.ITEMS.stream()
                .collect(Collectors.toMap(ConfigurationCatalog.Spec::key, Function.identity()));
        Properties bootstrap = loadBootstrapProperties();
        int dbCount = 0;
        int bootstrapCount = 0;
        boolean restart = false;
        List<String> changed = new ArrayList<>();
        for (String key : keys) {
            ConfigurationCatalog.Spec spec = specs.get(key);
            if (spec == null) continue;
            if ("BOOTSTRAP".equals(spec.storage())) {
                if (bootstrap.remove(key) != null) { bootstrapCount++; changed.add(key); restart = true; }
            } else if (isEncryptedSecret(key)) {
                if (secrets.exists(key)) { secrets.delete(key); dbCount++; changed.add(key); restart |= spec.restartRequired(); }
            } else if (repository.existsById(key)) {
                repository.deleteById(key);
                removeRuntimeDatabaseValue(key);
                dbCount++; changed.add(key); restart |= spec.restartRequired();
            }
        }
        if (bootstrapCount > 0) storeBootstrapProperties(bootstrap);
        if (changed.stream().anyMatch(ConfigurationCatalog.ENCRYPTED_SECRET_KEYS::contains)) {
            runtimeSecretBindingService.refresh();
        }
        if (!changed.isEmpty()) audit.log("SYSTEM_CONFIGURATION_RESET", username, "SUCCESS", "Alapértékre visszaállított kulcsok: " + String.join(",", changed));
        return new ConfigurationSaveResponse(dbCount, bootstrapCount, restart, changed, bootstrapPath().toString());
    }


    /**
     * A futásidőben mentett adatbázis-konfigurációt azonnal publikálja ugyanabba a
     * Spring property source-ba, amelyet induláskor a
     * {@link DatabaseConfigurationEnvironmentPostProcessor} épít fel. Így a
     * restartot nem igénylő DATABASE kulcsokat az Environment-alapú fogyasztók
     * ugyanabban a JVM-ben is azonnal látják.
     *
     * @param key a konfigurációs kulcs
     * @param value a mentett érték
     */
    public void bindRuntimeDatabaseValue(String key, String value) {
        if (!DatabaseConfigurationEnvironmentPostProcessor.isRuntimeConfigurationKey(key)
                || !(environment instanceof ConfigurableEnvironment configurableEnvironment)) {
            return;
        }
        PropertySource<?> existing = configurableEnvironment.getPropertySources()
                .get("databaseSystemConfiguration");
        if (existing instanceof MapPropertySource mapPropertySource) {
            if (StringUtils.hasText(value)) {
                mapPropertySource.getSource().put(key, value);
            } else {
                mapPropertySource.getSource().remove(key);
            }
            return;
        }
        if (StringUtils.hasText(value)) {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put(key, value);
            configurableEnvironment.getPropertySources().addFirst(
                    new MapPropertySource("databaseSystemConfiguration", values));
        }
    }

    /**
     * Eltávolítja a törölt adatbázis-konfiguráció futásidejű property-source
     * kötését, hogy az Environment ismét a következő alacsonyabb prioritású
     * forrásra essen vissza.
     *
     * @param key a törölt konfigurációs kulcs
     */
    public void removeRuntimeDatabaseValue(String key) {
        bindRuntimeDatabaseValue(key, null);
    }

    /**
     * A {@code validate} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>Az ellenőrzési eredményt a webes megjelenítés és a további üzleti döntések számára konzisztens formában állítja elő.</p>
     * @param spec a művelet bemeneti {@code spec} értéke
     * @param value a művelet bemeneti {@code value} értéke
     */
    private void validate(ConfigurationCatalog.Spec spec, String value) {
        if (!ConfigurationCatalog.isOptionalIntegrationKey(spec.key())
                && !("nav.xsdparsertool.api-key.value".equals(spec.key())
                && !environment.getProperty("nav.xsdparsertool.api-key.enabled", Boolean.class, false))
                && value.isBlank()) {
            throw new IllegalArgumentException(spec.label() + " megadása kötelező.");
        }
        if ("NUMBER".equals(spec.type()) && !value.isBlank()) {
            try { Long.parseLong(value); } catch (NumberFormatException ex) { throw new IllegalArgumentException(spec.label() + " csak egész szám lehet."); }
        }
        if ("BOOLEAN".equals(spec.type()) && !value.isBlank() && !List.of("true", "false").contains(value.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException(spec.label() + " értéke true vagy false lehet.");
        }
        if ("SELECT".equals(spec.type()) && !value.isBlank() && !spec.options().contains(value)) {
            throw new IllegalArgumentException(spec.label() + " értéke nem támogatott: " + value);
        }
    }

    /**
     * A {@code loadBootstrapProperties} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    private Properties loadBootstrapProperties() {
        Properties properties = new Properties();
        Path path = bootstrapPath();
        if (ExceptionSafeOperations.isRegularFile(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                properties.load(reader);
            } catch (IOException ex) {
                throw new IllegalStateException("A bootstrap konfiguráció nem olvasható: " + path, ex);
            }
        }
        return properties;
    }

    /**
     * A {@code storeBootstrapProperties} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param properties a művelethez szükséges konfigurációs adatok
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private void storeBootstrapProperties(Properties properties) throws IOException {
        Path path = bootstrapPath();
        ExceptionSafeOperations.createDirectories(path.toAbsolutePath().getParent());
        if (ExceptionSafeOperations.fileExists(path)) {
            SecureFileOperations.copyPrivate(path, path.resolveSibling(path.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
        }
        Path temp = path.resolveSibling(path.getFileName() + ".tmp");
        try (Writer writer = SecureFileOperations.newPrivateBufferedWriter(temp, StandardCharsets.UTF_8)) {
            properties.store(writer, "M2M XML EDITOR bootstrap configuration");
        }
        try {
            SecureFileOperations.movePrivate(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException | AccessDeniedException atomicMoveFailure) {
            // Windows may reject ATOMIC_MOVE when replacing an existing file even on the
            // same volume. Fall back to a normal replace; the .bak file above preserves
            // the previous bootstrap configuration if the fallback itself fails.
            try {
                SecureFileOperations.movePrivate(temp, path, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException fallbackFailure) {
                fallbackFailure.addSuppressed(atomicMoveFailure);
                throw fallbackFailure;
            }
        }
    }

    /**
     * A {@code bootstrapPath} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @return a művelet feldolgozási eredménye
     */
    public Path bootstrapPath() {
        String explicit = environment.getProperty("nav.xsdparsertool.bootstrap-config-file");
        if (StringUtils.hasText(explicit)) return Path.of(explicit).toAbsolutePath().normalize();
        String imported = environment.getProperty("spring.config.import", "");
        for (String item : imported.split(",")) {
            String candidate = item.trim().replace("optional:", "");
            if (candidate.startsWith("file:")) {
                return Path.of(candidate.substring("file:".length())).toAbsolutePath().normalize();
            }
        }
        return Path.of("config", "application-bootstrap.properties").toAbsolutePath().normalize();
    }
    /** A kiválasztott DB-típushoz igazítja a technikai bootstrap kulcsokat. */
    private void harmonizeDatabaseBootstrap(Properties bootstrap) {
        String type = bootstrap.getProperty("nav.xsdparsertool.database.type", "H2").trim().toUpperCase(Locale.ROOT);
        switch (type) {
            case "H2" -> {
                bootstrap.remove("spring.jpa.properties.hibernate.type.preferred_instant_jdbc_type");
                bootstrap.setProperty("spring.datasource.driver-class-name", "org.h2.Driver");
                bootstrap.setProperty("spring.jpa.database-platform", "org.hibernate.dialect.H2Dialect");
                bootstrap.setProperty("spring.flyway.locations", "classpath:db/migration/H2");
                bootstrap.setProperty("spring.h2.console.enabled", "true");
            }
            case "MYSQL" -> {
                bootstrap.remove("spring.jpa.properties.hibernate.type.preferred_instant_jdbc_type");
                bootstrap.setProperty("spring.datasource.driver-class-name", "com.mysql.cj.jdbc.Driver");
                bootstrap.setProperty("spring.jpa.database-platform", "org.hibernate.dialect.MySQLDialect");
                bootstrap.setProperty("spring.flyway.locations", "classpath:db/migration/MYSQL");
                bootstrap.setProperty("spring.h2.console.enabled", "false");
            }
            case "POSTGRESQL" -> {
                bootstrap.remove("spring.jpa.properties.hibernate.type.preferred_instant_jdbc_type");
                bootstrap.setProperty("spring.datasource.driver-class-name", "org.postgresql.Driver");
                bootstrap.setProperty("spring.jpa.database-platform", "org.hibernate.dialect.PostgreSQLDialect");
                bootstrap.setProperty("spring.flyway.locations", "classpath:db/migration/POSTGRESQL");
                bootstrap.setProperty("spring.h2.console.enabled", "false");
            }
            case "ORACLE" -> {
                bootstrap.setProperty("spring.jpa.properties.hibernate.type.preferred_instant_jdbc_type", "TIMESTAMP");
                bootstrap.setProperty("spring.datasource.driver-class-name", "oracle.jdbc.OracleDriver");
                bootstrap.setProperty("spring.jpa.database-platform", "org.hibernate.dialect.OracleDialect");
                bootstrap.setProperty("spring.flyway.locations", "classpath:db/migration/ORACLE");
                bootstrap.setProperty("spring.h2.console.enabled", "false");
            }
            default -> throw new IllegalArgumentException("Nem támogatott adatbázistípus: " + type);
        }
    }

}
