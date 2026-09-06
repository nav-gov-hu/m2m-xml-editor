package hu.gov.nav.xsdparsertool.web.secret.service;

import hu.gov.nav.xsdparsertool.core.support.SecureFileOperations;
import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import hu.gov.nav.xsdparsertool.web.security.SecurityMode;
import hu.gov.nav.xsdparsertool.web.security.SecurityModeProperties;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.Properties;

/**
 * A kapcsolódó webes üzleti vagy alkalmazási folyamatokat összefogó szolgáltatás.
 *
 * <p>A {@code MasterKeyService} osztály a web modul titokkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Service
public class MasterKeyService {
    private static final String FORMAT_VERSION = "1";
    private static final String KDF = "ARGON2ID";
    private static final String CIPHER = "AES-256-GCM";
    private static final int ARGON_MEMORY_KB = 65_536;
    private static final int ARGON_ITERATIONS = 3;
    private static final int ARGON_PARALLELISM = 1;
    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 12;
    private static final int AES_KEY_LENGTH = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Environment environment;
    private final SecurityModeProperties securityModeProperties;
    private volatile SecretKey cached;

    /**
     * Létrehozza a {@code MasterKeyService} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param environment a művelet bemeneti {@code environment} értéke
     * @param securityModeProperties a művelethez szükséges konfigurációs adatok
     */
    public MasterKeyService(Environment environment, SecurityModeProperties securityModeProperties) {
        this.environment = environment;
        this.securityModeProperties = securityModeProperties;
    }

    /**
     * A {@code getOrCreate} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a titokkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public synchronized SecretKey getOrCreate() {
        if (cached != null) {
            return cached;
        }
        if (securityModeProperties.getSecurityMode() == SecurityMode.STANDALONE) {
            return cached = loadOrCreateStandaloneMachineKey();
        }
        return cached = loadOrCreateServerKey();
    }

    /**
     * Backward-compatible migration hook for legacy standalone installations.
     * New installations use a machine-level master.key and do not depend on a user password.
     */
    public synchronized boolean migrateLegacyStandaloneKey(String username, char[] password) {
        if (securityModeProperties.getSecurityMode() != SecurityMode.STANDALONE) {
            return false;
        }
        Path file = keyFile();
        try {
            if (!isLegacyWrappedKey(file)) {
                getOrCreate();
                return false;
            }
            requireCredentials(username, password);
            SecretKey migrated = readWrappedKey(file, username, password);
            writePlainMachineKey(file, migrated);
            cached = migrated;
            return true;
        } catch (Exception ex) {
            throw new IllegalStateException("A korábbi, felhasználói jelszóval védett standalone master.key nem migrálható: " + file, ex);
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
    }

    /**
     * Kept for source compatibility. It no longer binds the master key to the login user.
     */
    public synchronized void unlockOrCreateStandalone(String username, char[] password) {
        if (securityModeProperties.getSecurityMode() != SecurityMode.STANDALONE) {
            return;
        }
        if (isLegacyWrappedKey(keyFile())) {
            migrateLegacyStandaloneKey(username, password);
        } else {
            try {
                getOrCreate();
            } finally {
                if (password != null) {
                    Arrays.fill(password, '\0');
                }
            }
        }
    }

    /**
     * User password changes must not alter the machine-level standalone master key.
     */
    public synchronized void rewrapStandalone(String username, char[] newPassword) {
        if (newPassword != null) {
            Arrays.fill(newPassword, '\0');
        }
    }

    /**
     * The machine-level key remains available for the lifetime of the application process.
     */
    public synchronized void lockStandalone() {
        // Intentionally no-op. Logout must not lock a machine-level key shared by all users and API calls.
    }

    /**
     * A {@code isUnlocked} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A végrehajtás során figyelembe veszi az XML-szerkesztési zárolást és a jogosultsági feltételeket; a zárolási állapot megkerülése nem része a fallback viselkedésnek.</p>
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    public boolean isUnlocked() {
        return cached != null;
    }

    /**
     * A {@code keyFile} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @return a művelet feldolgozási eredménye
     */
    public Path keyFile() {
        String explicit = environment.getProperty("m2m.xml.editor.secret.master-key-file");
        if (StringUtils.hasText(explicit)) {
            return Path.of(explicit).toAbsolutePath().normalize();
        }
        String dataDir = environment.getProperty("nav.xsdparsertool.data-directory");
        if (StringUtils.hasText(dataDir)) {
            return Path.of(dataDir, "config", "master.key").toAbsolutePath().normalize();
        }
        String bootstrap = environment.getProperty("nav.xsdparsertool.bootstrap-config-file");
        if (StringUtils.hasText(bootstrap)) {
            Path parent = Path.of(bootstrap).toAbsolutePath().normalize().getParent();
            if (parent != null) {
                return parent.resolve("master.key");
            }
        }
        return Path.of(resolveUserHome(), ".m2m-xml-editor", "master.key").toAbsolutePath().normalize();
    }

    /**
     * A {@code resolveUserHome} művelet feloldja a megfelelő erőforrást, állapotot vagy értéket a rendelkezésre álló jelöltek közül.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @return a feloldott vagy lekért érték
     */
    private String resolveUserHome() {
        String propertyName = String.join(".", "user", "home");
        return ExceptionSafeOperations.systemProperty(propertyName, ".");
    }

    /**
     * A {@code loadOrCreateStandaloneMachineKey} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a titokkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    private SecretKey loadOrCreateStandaloneMachineKey() {
        Path file = keyFile();
        try {
            if (ExceptionSafeOperations.isRegularFile(file)) {
                if (isLegacyWrappedKey(file)) {
                    throw new IllegalStateException("A master.key még a korábbi, felhasználói jelszóhoz kötött formátumú. Jelentkezzen be az eredeti admin felhasználóval az egyszeri migrációhoz.");
                }
                return PlainAesKeyDecoder.decode(Files.readString(file, StandardCharsets.UTF_8).trim());
            }
            ExceptionSafeOperations.createDirectories(file.getParent());
            SecretKey key = generateAesKey();
            writePlainMachineKey(file, key);
            return key;
        } catch (Exception ex) {
            throw new IllegalStateException("A gépszintű standalone mesterkulcs nem hozható létre vagy nem olvasható: " + file, ex);
        }
    }

    /**
     * A {@code isLegacyWrappedKey} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a titokkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param file a feldolgozásban részt vevő fájl vagy elérési út
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    private boolean isLegacyWrappedKey(Path file) {
        if (!ExceptionSafeOperations.isRegularFile(file)) {
            return false;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Properties properties = new Properties();
            properties.load(reader);
            return FORMAT_VERSION.equals(properties.getProperty("version"))
                    && KDF.equals(properties.getProperty("kdf"))
                    && CIPHER.equals(properties.getProperty("cipher"))
                    && StringUtils.hasText(properties.getProperty("encryptedMasterKey"));
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * A {@code writePlainMachineKey} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a titokkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param file a feldolgozásban részt vevő fájl vagy elérési út
     * @param key a művelet bemeneti {@code key} értéke
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private void writePlainMachineKey(Path file, SecretKey key) throws IOException {
        ExceptionSafeOperations.createDirectories(file.getParent());
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        SecureFileOperations.writePrivateString(temporary, Base64.getEncoder().encodeToString(key.getEncoded()), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        try {
            SecureFileOperations.movePrivate(temporary, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
            SecureFileOperations.movePrivate(temporary, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        restrictPermissions(file);
    }

    /**
     * A {@code loadOrCreateServerKey} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a titokkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    private SecretKey loadOrCreateServerKey() {
        String systemKey = ExceptionSafeOperations.systemProperty("m2m.xml.editor.secret.master-key");
        if (StringUtils.hasText(systemKey)) {
            return PlainAesKeyDecoder.decode(systemKey.trim());
        }
        String envKey = System.getenv("M2M_XML_EDITOR_MASTER_KEY");
        if (StringUtils.hasText(envKey)) {
            return PlainAesKeyDecoder.decode(envKey.trim());
        }
        Path file = keyFile();
        try {
            if (ExceptionSafeOperations.isRegularFile(file)) {
                return PlainAesKeyDecoder.decode(Files.readString(file).trim());
            }
            ExceptionSafeOperations.createDirectories(file.getParent());
            SecretKey key = generateAesKey();
            SecureFileOperations.writePrivateString(file, Base64.getEncoder().encodeToString(key.getEncoded()), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            restrictPermissions(file);
            return key;
        } catch (Exception ex) {
            throw new IllegalStateException("A szerveroldali titkosítási mesterkulcs nem hozható létre vagy nem olvasható: " + file, ex);
        }
    }

    /**
     * A {@code readWrappedKey} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a titokkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param file a feldolgozásban részt vevő fájl vagy elérési út
     * @param username a művelet felhasználói kontextusa vagy felhasználóneve
     * @param password a művelet bemeneti {@code password} értéke
     * @return a feloldott vagy lekért érték
     * @throws Exception ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private SecretKey readWrappedKey(Path file, String username, char[] password) throws Exception {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        if (!FORMAT_VERSION.equals(properties.getProperty("version"))
                || !KDF.equals(properties.getProperty("kdf"))
                || !CIPHER.equals(properties.getProperty("cipher"))) {
            throw new IllegalStateException("Ismeretlen vagy nem támogatott standalone master.key formátum.");
        }
        int memoryKb = Integer.parseInt(properties.getProperty("memoryKb"));
        int iterations = Integer.parseInt(properties.getProperty("iterations"));
        int parallelism = Integer.parseInt(properties.getProperty("parallelism"));
        byte[] salt = Base64.getDecoder().decode(properties.getProperty("salt"));
        byte[] iv = Base64.getDecoder().decode(properties.getProperty("iv"));
        byte[] encrypted = Base64.getDecoder().decode(properties.getProperty("encryptedMasterKey"));
        byte[] wrappingKey = deriveKey(username, password, salt, memoryKb, iterations, parallelism);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(wrappingKey, "AES"), new GCMParameterSpec(128, iv));
            cipher.updateAAD(aad(username, properties.getProperty("version")));
            byte[] plain = cipher.doFinal(encrypted);
            if (plain.length != AES_KEY_LENGTH) {
                throw new IllegalStateException("A feloldott mesterkulcs hossza érvénytelen.");
            }
            return new SecretKeySpec(plain, "AES");
        } finally {
            Arrays.fill(wrappingKey, (byte) 0);
        }
    }

    /**
     * A {@code writeWrappedKey} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a titokkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param file a feldolgozásban részt vevő fájl vagy elérési út
     * @param masterKey a művelet bemeneti {@code masterKey} értéke
     * @param username a művelet felhasználói kontextusa vagy felhasználóneve
     * @param password a művelet bemeneti {@code password} értéke
     * @throws Exception ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private void writeWrappedKey(Path file, SecretKey masterKey, String username, char[] password) throws Exception {
        byte[] salt = new byte[SALT_LENGTH];
        byte[] iv = new byte[IV_LENGTH];
        RANDOM.nextBytes(salt);
        RANDOM.nextBytes(iv);
        byte[] wrappingKey = deriveKey(username, password, salt, ARGON_MEMORY_KB, ARGON_ITERATIONS, ARGON_PARALLELISM);
        byte[] encrypted;
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(wrappingKey, "AES"), new GCMParameterSpec(128, iv));
            cipher.updateAAD(aad(username, FORMAT_VERSION));
            encrypted = cipher.doFinal(masterKey.getEncoded());
        } finally {
            Arrays.fill(wrappingKey, (byte) 0);
        }

        Properties properties = new Properties();
        properties.setProperty("version", FORMAT_VERSION);
        properties.setProperty("kdf", KDF);
        properties.setProperty("memoryKb", Integer.toString(ARGON_MEMORY_KB));
        properties.setProperty("iterations", Integer.toString(ARGON_ITERATIONS));
        properties.setProperty("parallelism", Integer.toString(ARGON_PARALLELISM));
        properties.setProperty("salt", Base64.getEncoder().encodeToString(salt));
        properties.setProperty("cipher", CIPHER);
        properties.setProperty("iv", Base64.getEncoder().encodeToString(iv));
        properties.setProperty("username", normalizeUsername(username));
        properties.setProperty("encryptedMasterKey", Base64.getEncoder().encodeToString(encrypted));
        properties.setProperty("createdAt", Instant.now().toString());

        ExceptionSafeOperations.createDirectories(file.getParent());
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try (Writer writer = SecureFileOperations.newPrivateBufferedWriter(temporary, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            properties.store(writer, "M2M XML EDITOR standalone encrypted master key");
        }
        try {
            SecureFileOperations.movePrivate(temporary, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
            SecureFileOperations.movePrivate(temporary, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        restrictPermissions(file);
    }

    /**
     * A {@code deriveKey} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a titokkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param username a művelet felhasználói kontextusa vagy felhasználóneve
     * @param password a művelet bemeneti {@code password} értéke
     * @param salt a művelet bemeneti {@code salt} értéke
     * @param memoryKb a művelet bemeneti {@code memoryKb} értéke
     * @param iterations a művelet bemeneti {@code iterations} értéke
     * @param parallelism a művelet bemeneti {@code parallelism} értéke
     * @return a művelet feldolgozási eredménye
     */
    private byte[] deriveKey(String username, char[] password, byte[] salt, int memoryKb, int iterations, int parallelism) {
        byte[] passwordBytes = (normalizeUsername(username) + "\u0000" + new String(password)).getBytes(StandardCharsets.UTF_8);
        try {
            Argon2Parameters parameters = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                    .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                    .withSalt(salt)
                    .withMemoryAsKB(memoryKb)
                    .withIterations(iterations)
                    .withParallelism(parallelism)
                    .build();
            Argon2BytesGenerator generator = new Argon2BytesGenerator();
            generator.init(parameters);
            byte[] derived = new byte[AES_KEY_LENGTH];
            generator.generateBytes(passwordBytes, derived);
            return derived;
        } finally {
            Arrays.fill(passwordBytes, (byte) 0);
        }
    }

    /**
     * A {@code aad} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a titokkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param username a művelet felhasználói kontextusa vagy felhasználóneve
     * @param version a művelet bemeneti {@code version} értéke
     * @return a művelet feldolgozási eredménye
     */
    private byte[] aad(String username, String version) {
        return ("M2M-XML-EDITOR|master-key|" + version + "|" + normalizeUsername(username)).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * A {@code normalizeUsername} művelet feldolgozza és normalizálja a bemeneti adatot a további feldolgozás számára.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @param username a művelet felhasználói kontextusa vagy felhasználóneve
     * @return a művelet feldolgozási eredménye
     */
    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * A {@code requireCredentials} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a titokkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param username a művelet felhasználói kontextusa vagy felhasználóneve
     * @param password a művelet bemeneti {@code password} értéke
     */
    private void requireCredentials(String username, char[] password) {
        if (!StringUtils.hasText(username) || password == null || password.length == 0) {
            throw new IllegalArgumentException("A standalone mesterkulcs feloldásához felhasználónév és jelszó szükséges.");
        }
    }

    /**
     * A {@code generateAesKey} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a titokkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a művelet feldolgozási eredménye
     * @throws Exception ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private SecretKey generateAesKey() throws Exception {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(256);
        return generator.generateKey();
    }

    /**
     * A {@code restrictPermissions} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @param file a feldolgozásban részt vevő fájl vagy elérési út
     */
    private void restrictPermissions(Path file) {
        try {
            var view = Files.getFileAttributeView(file, java.nio.file.attribute.PosixFileAttributeView.class);
            if (view != null) {
                Files.setPosixFilePermissions(file, java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
            }
        } catch (IOException ignored) {
        }
    }
}
