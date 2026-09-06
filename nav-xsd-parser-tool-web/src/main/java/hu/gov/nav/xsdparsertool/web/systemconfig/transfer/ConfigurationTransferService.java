package hu.gov.nav.xsdparsertool.web.systemconfig.transfer;

import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;
import hu.gov.nav.xsdparsertool.core.support.SecureFileOperations;
import hu.gov.nav.xsdparsertool.web.audit.AuditLogService;
import hu.gov.nav.xsdparsertool.web.certificate.entity.TrustedCertificateEntity;
import hu.gov.nav.xsdparsertool.web.certificate.repository.TrustedCertificateRepository;
import hu.gov.nav.xsdparsertool.web.certificate.service.TrustedCertificateSslContextInitializer;
import hu.gov.nav.xsdparsertool.web.secret.entity.SystemSecretEntity;
import hu.gov.nav.xsdparsertool.web.secret.repository.SystemSecretRepository;
import hu.gov.nav.xsdparsertool.web.secret.service.MasterKeyService;
import hu.gov.nav.xsdparsertool.web.secret.service.RuntimeSecretBindingService;
import hu.gov.nav.xsdparsertool.web.support.RepositoryAccess;
import hu.gov.nav.xsdparsertool.web.systemconfig.entity.SystemConfigurationEntity;
import hu.gov.nav.xsdparsertool.web.systemconfig.service.ConfigurationCatalog;
import hu.gov.nav.xsdparsertool.web.systemconfig.repository.SystemConfigurationRepository;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/** Teljes konfigurációs snapshot exportja és MERGE importja. */
@Service
public class ConfigurationTransferService {
    public static final String FORMAT = "m2m-xml-editor-system-configuration-v2";
    private static final String LEGACY_FORMAT = "m2m-xml-editor-system-configuration-v1";

    private static final Set<String> PROTECTED_TARGET_KEYS = Set.of(
            "app.data.dir",
            "nav.xsdparsertool.data-directory",
            "nav.xsdparsertool.bootstrap-config-file",
            "m2m.xml.editor.secret.master-key-file",
            "nav.xsdparsertool.database.type",
            "nav.xsdparsertool.database.schema",
            "spring.datasource.url",
            "spring.datasource.username",
            "spring.datasource.password",
            "spring.datasource.driver-class-name",
            "spring.flyway.locations",
            "spring.h2.console.enabled",
            "spring.h2.console.path");

    private final SystemConfigurationRepository configurations;
    private final SystemSecretRepository secrets;
    private final TrustedCertificateRepository trustedCertificates;
    private final TrustedCertificateSslContextInitializer sslContextInitializer;
    private final Environment environment;
    private final RuntimeSecretBindingService runtimeSecretBindingService;
    private final MasterKeyService masterKeyService;
    private final AuditLogService audit;

    /** Létrehozza a konfigurációs export/import szolgáltatást.
     * @param configurations rendszerkonfiguráció repository
     * @param secrets titkosított secret repository
     * @param trustedCertificates megbízható tanúsítvány repository
     * @param sslContextInitializer a tanúsítványimport után újratöltendő TLS kontextus
     * @param environment aktuális Spring környezet
     * @param runtimeSecretBindingService runtime secret újrakötő szolgáltatás
     * @param masterKeyService a ciphertext master key szolgáltatása
     * @param audit audit naplózó szolgáltatás
     */
    public ConfigurationTransferService(SystemConfigurationRepository configurations,
                                        SystemSecretRepository secrets,
                                        TrustedCertificateRepository trustedCertificates,
                                        TrustedCertificateSslContextInitializer sslContextInitializer,
                                        Environment environment,
                                        RuntimeSecretBindingService runtimeSecretBindingService,
                                        MasterKeyService masterKeyService,
                                        AuditLogService audit) {
        this.configurations = configurations;
        this.secrets = secrets;
        this.trustedCertificates = trustedCertificates;
        this.sslContextInitializer = sslContextInitializer;
        this.environment = environment;
        this.runtimeSecretBindingService = runtimeSecretBindingService;
        this.masterKeyService = masterKeyService;
        this.audit = audit;
    }

    /** A rendszer teljes konfigurációs állapotát exportálja, a secreteket ciphertextként megtartva. */
    @Transactional(readOnly = true)
    public ConfigurationTransferDocument exportConfiguration() throws IOException {
        Map<String, String> bootstrap = propertiesToMap(loadProperties(bootstrapPath()));
        Map<String, String> values = new LinkedHashMap<>();
        for (SystemConfigurationEntity entity : RepositoryAccess.findAll(configurations)) {
            values.put(entity.getKey(), portable(entity.getValue()));
        }
        Map<String, ConfigurationTransferDocument.EncryptedSecret> secretValues = new LinkedHashMap<>();
        for (SystemSecretEntity entity : RepositoryAccess.findAll(secrets)) {
            secretValues.put(entity.getKey(), new ConfigurationTransferDocument.EncryptedSecret(
                    entity.getEncryptedValue(), entity.getEncryptionVersion()));
        }
        List<ConfigurationTransferDocument.TrustedCertificate> certificateValues = RepositoryAccess.findAll(trustedCertificates).stream()
                .map(this::exportCertificate)
                .toList();
        Map<String, Map<String, String>> propertyFiles = exportAdditionalPropertyFiles();
        Map<String, String> textFiles = exportAdditionalTextFiles();
        String fingerprint = secretValues.isEmpty() ? "" : masterKeyFingerprint();
        return new ConfigurationTransferDocument(FORMAT, Instant.now(),
                environment.getProperty("nav.xsdparsertool.database.type", "H2"),
                fingerprint, portableMap(bootstrap), values, secretValues, certificateValues, propertyFiles, textFiles);
    }

    /** Property-szintű MERGE import: meglévő kulcsot felülír, hiányzót beszúr. */
    @Transactional
    public ConfigurationImportResult importConfiguration(ConfigurationTransferDocument document, String username) throws IOException {
        if (document == null || (!FORMAT.equals(document.format()) && !LEGACY_FORMAT.equals(document.format()))) {
            throw new IllegalArgumentException("Nem támogatott konfigurációs exportformátum.");
        }
        boolean legacy = LEGACY_FORMAT.equals(document.format());
        Map<String, String> importedValues = new LinkedHashMap<>();
        Map<String, String> importedBootstrap = new LinkedHashMap<>(safe(document.bootstrap()));
        if (legacy) {
            Map<String, ConfigurationCatalog.Spec> specs = new LinkedHashMap<>();
            ConfigurationCatalog.ITEMS.forEach(spec -> specs.put(spec.key(), spec));
            for (Map.Entry<String, String> entry : safe(document.values()).entrySet()) {
                ConfigurationCatalog.Spec spec = specs.get(entry.getKey());
                if (spec == null || spec.sensitive()) continue;
                if ("BOOTSTRAP".equals(spec.storage())) importedBootstrap.put(entry.getKey(), entry.getValue());
                else if ("DATABASE".equals(spec.storage())) importedValues.put(entry.getKey(), entry.getValue());
            }
        } else {
            importedValues.putAll(safe(document.values()));
        }
        Map<String, ConfigurationTransferDocument.EncryptedSecret> importedSecrets = legacy ? Map.of() : safeSecrets(document.secrets());
        List<ConfigurationTransferDocument.TrustedCertificate> importedCertificates = legacy ? List.of() : safeCertificates(document.trustedCertificates());
        validateCertificates(importedCertificates);
        Map<Path, Map<String, String>> importedPropertyFiles = new LinkedHashMap<>();
        Map<Path, String> importedTextFiles = new LinkedHashMap<>();
        if (!legacy) {
            for (Map.Entry<String, Map<String, String>> entry : safeFiles(document.propertyFiles()).entrySet()) {
                importedPropertyFiles.put(resolveConfigRelativeFile(entry.getKey()), entry.getValue());
            }
            for (Map.Entry<String, String> entry : safe(document.textFiles()).entrySet()) {
                importedTextFiles.put(resolveConfigRelativeTextFile(entry.getKey()), entry.getValue());
            }
        }
        if (!importedSecrets.isEmpty()) {
            if (!StringUtils.hasText(document.secretKeyFingerprint())
                    || !MessageDigest.isEqual(document.secretKeyFingerprint().getBytes(StandardCharsets.UTF_8),
                    masterKeyFingerprint().getBytes(StandardCharsets.UTF_8))) {
                throw new IllegalArgumentException("A titkosított secretek másik master.key kulcshoz tartoznak. A konfiguráció importja előtt ugyanazt a master.key fájlt kell használni.");
            }
        }
        int dbUpdated = 0;
        int dbInserted = 0;
        int secretUpdated = 0;
        int secretInserted = 0;
        int certificateUpdated = 0;
        int certificateInserted = 0;
        Instant now = Instant.now();

        for (Map.Entry<String, String> entry : importedValues.entrySet()) {
            SystemConfigurationEntity entity = RepositoryAccess.findById(configurations, entry.getKey()).orElse(null);
            if (entity == null) {
                entity = new SystemConfigurationEntity();
                entity.setKey(entry.getKey());
                dbInserted++;
            } else {
                dbUpdated++;
            }
            entity.setValue(expandPortable(entry.getValue()));
            entity.setUpdatedAt(now);
            entity.setUpdatedBy(username);
            configurations.save(entity);
        }

        for (Map.Entry<String, ConfigurationTransferDocument.EncryptedSecret> entry : importedSecrets.entrySet()) {
            ConfigurationTransferDocument.EncryptedSecret imported = entry.getValue();
            if (imported == null || !StringUtils.hasText(imported.encryptedValue())) continue;
            SystemSecretEntity entity = RepositoryAccess.findById(secrets, entry.getKey()).orElse(null);
            if (entity == null) {
                entity = new SystemSecretEntity();
                entity.setKey(entry.getKey());
                secretInserted++;
            } else {
                secretUpdated++;
            }
            entity.setEncryptedValue(imported.encryptedValue());
            entity.setEncryptionVersion(imported.encryptionVersion());
            entity.setUpdatedAt(now);
            entity.setUpdatedBy(username);
            secrets.save(entity);
        }

        for (ConfigurationTransferDocument.TrustedCertificate imported : importedCertificates) {
            TrustedCertificateEntity entity = trustedCertificates.findBySha256Fingerprint(imported.sha256Fingerprint()).orElse(null);
            if (entity == null) {
                entity = new TrustedCertificateEntity();
                certificateInserted++;
            } else {
                certificateUpdated++;
            }
            applyCertificate(entity, imported);
            trustedCertificates.save(entity);
        }

        List<String> protectedKeys = new ArrayList<>();
        int bootstrapMerged = mergePropertiesFile(bootstrapPath(), importedBootstrap, true, protectedKeys);
        int propertyFilesMerged = 0;
        for (Map.Entry<Path, Map<String, String>> entry : importedPropertyFiles.entrySet()) {
            if (entry.getKey().equals(bootstrapPath())) continue;
            propertyFilesMerged += mergePropertiesFile(entry.getKey(), entry.getValue(), true, protectedKeys);
        }
        int textFilesReplaced = 0;
        for (Map.Entry<Path, String> entry : importedTextFiles.entrySet()) {
            storeTextFile(entry.getKey(), entry.getValue());
            textFilesReplaced++;
        }
        runtimeSecretBindingService.refresh();
        if (!importedCertificates.isEmpty()) sslContextInitializer.reload();
        audit.log("SYSTEM_CONFIGURATION_IMPORT", username, "SUCCESS",
                "MERGE import: dbUpdated=" + dbUpdated + ", dbInserted=" + dbInserted
                        + ", secretUpdated=" + secretUpdated + ", secretInserted=" + secretInserted
                        + ", certificateUpdated=" + certificateUpdated + ", certificateInserted=" + certificateInserted
                        + ", textFilesReplaced=" + textFilesReplaced);
        return new ConfigurationImportResult(dbUpdated, dbInserted, secretUpdated, secretInserted,
                certificateUpdated, certificateInserted, bootstrapMerged, propertyFilesMerged, textFilesReplaced,
                bootstrapMerged > 0 || propertyFilesMerged > 0 || textFilesReplaced > 0,
                List.copyOf(new LinkedHashSet<>(protectedKeys)));
    }

    /** Egy DB-ben tárolt megbízható tanúsítványt hordozható exportrekorddá alakít.
     * @param entity forrás tanúsítvány
     * @return exportálható tanúsítványrekord
     */
    private ConfigurationTransferDocument.TrustedCertificate exportCertificate(TrustedCertificateEntity entity) {
        return new ConfigurationTransferDocument.TrustedCertificate(
                entity.getAlias(), entity.getSubjectDn(), entity.getIssuerDn(), entity.getSerialNumber(),
                entity.getSha256Fingerprint(), entity.getValidFrom(), entity.getValidUntil(),
                entity.getSourceHost(), entity.getSourcePort(), entity.getStatus(),
                Base64.getEncoder().encodeToString(entity.getCertificateDer()), entity.getCreatedAt(), entity.getCreatedBy());
    }

    /** Még a MERGE előtt ellenőrzi a tanúsítványrekordok kötelező adatait és DER kódolását.
     * @param certificates importált tanúsítványok
     */
    private void validateCertificates(List<ConfigurationTransferDocument.TrustedCertificate> certificates) {
        for (ConfigurationTransferDocument.TrustedCertificate certificate : certificates) {
            if (certificate == null || !StringUtils.hasText(certificate.sha256Fingerprint())
                    || !StringUtils.hasText(certificate.certificateDerBase64())) {
                throw new IllegalArgumentException("A konfigurációs export hibás megbízható tanúsítványrekordot tartalmaz.");
            }
            try {
                Base64.getDecoder().decode(certificate.certificateDerBase64());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("A megbízható tanúsítvány DER tartalma nem érvényes Base64: " + certificate.sha256Fingerprint(), ex);
            }
        }
    }

    /** Az importált tanúsítvány teljes konfigurációs állapotát az entitásra másolja.
     * @param entity cél entitás
     * @param imported importált rekord
     */
    private void applyCertificate(TrustedCertificateEntity entity, ConfigurationTransferDocument.TrustedCertificate imported) {
        entity.setAlias(imported.alias());
        entity.setSubjectDn(imported.subjectDn());
        entity.setIssuerDn(imported.issuerDn());
        entity.setSerialNumber(imported.serialNumber());
        entity.setSha256Fingerprint(imported.sha256Fingerprint());
        entity.setValidFrom(imported.validFrom());
        entity.setValidUntil(imported.validUntil());
        entity.setSourceHost(imported.sourceHost());
        entity.setSourcePort(imported.sourcePort());
        entity.setStatus(imported.status());
        entity.setCertificateDer(Base64.getDecoder().decode(imported.certificateDerBase64()));
        entity.setCreatedAt(imported.createdAt() == null ? Instant.now() : imported.createdAt());
        entity.setCreatedBy(imported.createdBy());
    }

    /** Exportálja az adatkönyvtár config ágában található további properties fájlokat.
     * @return relatív fájlnév szerint csoportosított property-k
     * @throws IOException olvasási hiba esetén
     */
    private Map<String, Map<String, String>> exportAdditionalPropertyFiles() throws IOException {
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        Path root = configRoot();
        if (!Files.isDirectory(root)) return result;
        try (var stream = Files.walk(root, 3)) {
            for (Path file : stream.filter(Files::isRegularFile).filter(p -> p.getFileName().toString().endsWith(".properties")).toList()) {
                if (file.equals(bootstrapPath()) || file.getFileName().toString().equals("setup-integrations.properties")) continue;
                result.put(root.relativize(file).toString().replace('\\', '/'), portableMap(propertiesToMap(loadProperties(file))));
            }
        }
        return result;
    }

    /** Exportálja a config ág hordozható szöveges konfigurációs fájljait.
     * @return relatív fájlnév és teljes szöveges tartalom
     * @throws IOException olvasási hiba esetén
     */
    private Map<String, String> exportAdditionalTextFiles() throws IOException {
        Map<String, String> result = new LinkedHashMap<>();
        Path root = configRoot();
        if (!Files.isDirectory(root)) return result;
        try (var stream = Files.walk(root, 3)) {
            for (Path file : stream.filter(Files::isRegularFile).filter(this::isPortableTextConfigFile).toList()) {
                result.put(root.relativize(file).toString().replace('\\', '/'), Files.readString(file, StandardCharsets.UTF_8));
            }
        }
        return result;
    }

    /** Eldönti, hogy a fájl teljes szöveges rendszerkonfigurációként exportálható-e.
     * @param file vizsgált fájl
     * @return igaz támogatott szöveges config kiterjesztésnél
     */
    private boolean isPortableTextConfigFile(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        if (name.equals("setup-integrations.properties") || name.equals("master.key") || name.endsWith(".properties")) return false;
        return name.endsWith(".xml") || name.endsWith(".json") || name.endsWith(".yaml")
                || name.endsWith(".yml") || name.endsWith(".conf");
    }

    /** Property-szinten egyesíti az importot a célfájllal, a védett célkulcsokat kihagyva.
     * @param file cél properties fájl
     * @param imported importált kulcsok
     * @param protectTarget igaz esetben a célkörnyezet technikai kulcsai védettek
     * @param skipped kihagyott kulcsok gyűjtője
     * @return alkalmazott property-módosítások száma
     * @throws IOException fájlműveleti hiba esetén
     */
    private int mergePropertiesFile(Path file, Map<String, String> imported, boolean protectTarget, List<String> skipped) throws IOException {
        if (imported == null || imported.isEmpty()) return 0;
        Properties properties = loadProperties(file);
        int count = 0;
        for (Map.Entry<String, String> entry : imported.entrySet()) {
            if (protectTarget && PROTECTED_TARGET_KEYS.contains(entry.getKey())) {
                skipped.add(entry.getKey());
                continue;
            }
            properties.setProperty(entry.getKey(), expandPortable(entry.getValue()));
            count++;
        }
        storeProperties(file, properties);
        return count;
    }

    /** Biztonságosan felold egy config gyökérhez viszonyított properties fájlnevet.
     * @param relative exportban szereplő relatív fájlnév
     * @return normalizált célútvonal
     */
    private Path resolveConfigRelativeFile(String relative) {
        if (!StringUtils.hasText(relative)) throw new IllegalArgumentException("Üres konfigurációs fájlnév nem importálható.");
        Path target = configRoot().resolve(relative).normalize();
        if (!target.startsWith(configRoot()) || !target.getFileName().toString().endsWith(".properties")) {
            throw new IllegalArgumentException("Nem engedélyezett konfigurációs fájl: " + relative);
        }
        return target;
    }

    /** Biztonságosan felold egy importálandó szöveges config fájlt.
     * @param relative relatív config fájlnév
     * @return normalizált célútvonal
     */
    private Path resolveConfigRelativeTextFile(String relative) {
        Path target = configRoot().resolve(relative == null ? "" : relative).normalize();
        if (!target.startsWith(configRoot()) || !isPortableTextConfigFile(target)) {
            throw new IllegalArgumentException("Nem engedélyezett szöveges konfigurációs fájl: " + relative);
        }
        return target;
    }

    /** Privát jogosultsággal, ideiglenes fájlon keresztül cserél le egy szöveges config fájlt.
     * @param file célfájl
     * @param content új tartalom
     * @throws IOException fájlműveleti hiba esetén
     */
    private void storeTextFile(Path file, String content) throws IOException {
        ExceptionSafeOperations.createDirectories(file.getParent());
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try (Writer writer = SecureFileOperations.newPrivateBufferedWriter(temporary, StandardCharsets.UTF_8)) {
            writer.write(content == null ? "" : content);
        }
        SecureFileOperations.movePrivate(temporary, file, StandardCopyOption.REPLACE_EXISTING);
    }

    /** @return az aktív application-bootstrap.properties útvonala */
    private Path bootstrapPath() {
        String configured = environment.getProperty("nav.xsdparsertool.bootstrap-config-file");
        if (StringUtils.hasText(configured)) return Path.of(configured).toAbsolutePath().normalize();
        return configRoot().resolve("application-bootstrap.properties");
    }

    /** @return az aktív adatkönyvtár config gyökere */
    private Path configRoot() {
        String dataDir = environment.getProperty("app.data.dir", environment.getProperty("nav.xsdparsertool.data-directory", "."));
        return Path.of(dataDir).toAbsolutePath().normalize().resolve("config");
    }

    /** Az app.data.dir alatti abszolút értéket hordozható placeholderes alakra írja.
     * @param value konfigurációs érték
     * @return hordozható érték
     */
    private String portable(String value) {
        if (value == null) return "";
        String dataDir = Path.of(environment.getProperty("app.data.dir", environment.getProperty("nav.xsdparsertool.data-directory", ".")))
                .toAbsolutePath().normalize().toString();
        if (value.startsWith(dataDir)) return "${app.data.dir}" + value.substring(dataDir.length()).replace('\\', '/');
        return value;
    }

    /** A hordozható app.data.dir placeholdert a cél installáció gyökerére oldja.
     * @param value importált érték
     * @return célkörnyezetre feloldott érték
     */
    private String expandPortable(String value) {
        if (value == null) return "";
        String dataDir = Path.of(environment.getProperty("app.data.dir", environment.getProperty("nav.xsdparsertool.data-directory", ".")))
                .toAbsolutePath().normalize().toString();
        return value.replace("${app.data.dir}", dataDir);
    }

    /** Egy teljes property térkép útvonalértékeit hordozhatóvá teszi.
     * @param source forrásértékek
     * @return hordozható másolat
     */
    private Map<String, String> portableMap(Map<String, String> source) {
        Map<String, String> result = new LinkedHashMap<>();
        source.forEach((k, v) -> result.put(k, portable(v)));
        return result;
    }

    /** Beolvassa a properties fájlt, hiányzó fájlnál üres készlettel tér vissza.
     * @param file forrásfájl
     * @return beolvasott property-k
     * @throws IOException olvasási hiba esetén
     */
    private Properties loadProperties(Path file) throws IOException {
        Properties properties = new Properties();
        if (!ExceptionSafeOperations.isRegularFile(file)) return properties;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) { properties.load(reader); }
        return properties;
    }

    /** Determinisztikusan rendezett map reprezentációt készít.
     * @param properties forrás property-k
     * @return rendezett kulcs-érték térkép
     */
    private Map<String, String> propertiesToMap(Properties properties) {
        Map<String, String> values = new LinkedHashMap<>();
        properties.stringPropertyNames().stream().sorted().forEach(k -> values.put(k, properties.getProperty(k, "")));
        return values;
    }

    /** Privát jogosultságú ideiglenes fájlon keresztül atomikusan ment properties tartalmat.
     * @param file célfájl
     * @param properties mentendő property-k
     * @throws IOException fájlműveleti hiba esetén
     */
    private void storeProperties(Path file, Properties properties) throws IOException {
        ExceptionSafeOperations.createDirectories(file.getParent());
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try (Writer writer = SecureFileOperations.newPrivateBufferedWriter(temporary, StandardCharsets.UTF_8)) {
            properties.store(writer, "M2M XML EDITOR configuration import");
        }
        SecureFileOperations.movePrivate(temporary, file, StandardCopyOption.REPLACE_EXISTING);
    }


    /** A jelenlegi master.key SHA-256 ujjlenyomata; a ciphertext hordozhatóságának ellenőrzésére szolgál. */
    private String masterKeyFingerprint() {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(masterKeyService.getOrCreate().getEncoded());
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("A SHA-256 algoritmus nem érhető el a master.key ellenőrzéséhez.", ex);
        }
    }

    /** Null-safe map nézetet ad. @param value bemenet @return nem null map */
    private Map<String, String> safe(Map<String, String> value) { return value == null ? Map.of() : value; }
    /** Null-safe secret map nézetet ad. @param value bemenet @return nem null map */
    private Map<String, ConfigurationTransferDocument.EncryptedSecret> safeSecrets(Map<String, ConfigurationTransferDocument.EncryptedSecret> value) { return value == null ? Map.of() : value; }
    /** Null-safe fájlkonfiguráció map nézetet ad. @param value bemenet @return nem null map */
    private Map<String, Map<String, String>> safeFiles(Map<String, Map<String, String>> value) { return value == null ? Map.of() : value; }
    /** Null-safe tanúsítványlista nézetet ad. @param value bemenet @return nem null lista */
    private List<ConfigurationTransferDocument.TrustedCertificate> safeCertificates(List<ConfigurationTransferDocument.TrustedCertificate> value) { return value == null ? List.of() : value; }
}
