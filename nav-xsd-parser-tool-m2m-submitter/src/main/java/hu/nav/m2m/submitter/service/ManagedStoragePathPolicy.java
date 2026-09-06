package hu.nav.m2m.submitter.service;

import hu.nav.m2m.submitter.config.NavM2mProperties;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Központi policy az M2M helyi tárban tárolt, adatbázisból visszaolvasott fájlutakhoz.
 *
 * A fájlművelet kizárólag canonical, a konfigurált storage root alatt lévő reguláris
 * állományon történhet. A canonicalizálás és a sink ugyanebben az osztályban marad,
 * hogy a biztonsági határ statikus elemző számára is egyértelmű legyen.
 */
@Service
public class ManagedStoragePathPolicy {
    private final File storageRoot;

    /**
     * Létrehozza a(z) {@code ManagedStoragePathPolicy} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
     *
     * @param properties az M2M külső konfiguráció
     */
    public ManagedStoragePathPolicy(NavM2mProperties properties) {
        try {
            this.storageRoot = Path.of(properties.getStorageDirectory()).toAbsolutePath().normalize().toFile().getCanonicalFile();
        } catch (IOException ex) {
            throw new IllegalArgumentException("Az M2M tárolási gyökérkönyvtár nem canonicalizálható.", ex);
        }
    }

    /**
     * A menedzselt storage szabályain át ellenőrzött fájl teljes tartalmát beolvassa.
     *
     * @param storedPath a művelethez átadott {@code storedPath} érték
     * @return a művelet eredménye
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    public byte[] readAllBytes(String storedPath) throws IOException {
        if (storedPath == null || storedPath.isBlank()) {
            throw new IOException("Hiányzó M2M tárolási fájlútvonal.");
        }
        return readCanonicalFile(new File(storedPath));
    }


    /**
     * A bemeneti struktúrából biztonságosan kiolvassa a művelethez szükséges értéket, és hiányzó adat esetén a metódus szerinti fallbacket alkalmazza.
     *
     * @param submissionId a cél M2M beküldés azonosítója
     * @param storedPath a művelethez átadott {@code storedPath} érték
     * @return a művelet eredménye
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    public byte[] readSubmissionAttachment(java.util.UUID submissionId, String storedPath) throws IOException {
        if (submissionId == null) {
            throw new IOException("Hiányzó beküldési azonosító.");
        }
        String storedFileName = storedFileName(storedPath);
        if (!storedFileName.startsWith("filestore_")) {
            throw new IOException("A beküldési csatolmány fájlneve nem a kezelt névtérből származik.");
        }
        File trustedDirectory = new File(storageRoot, submissionId.toString()).getCanonicalFile();
        return readWithinTrustedDirectory(trustedDirectory, storedFileName);
    }

    /**
     * A bemeneti struktúrából biztonságosan kiolvassa a művelethez szükséges értéket, és hiányzó adat esetén a metódus szerinti fallbacket alkalmazza.
     *
     * @param xmlFileId az érintett XML-fájl adatbázis-azonosítója
     * @param attachmentId a cél csatolmány azonosítója
     * @param storedPath a művelethez átadott {@code storedPath} érték
     * @return a művelet eredménye
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    public byte[] readXmlFileAttachment(Long xmlFileId, java.util.UUID attachmentId, String storedPath) throws IOException {
        if (xmlFileId == null || xmlFileId <= 0 || attachmentId == null) {
            throw new IOException("Hiányzó csatolmány azonosító.");
        }
        String storedFileName = storedFileName(storedPath);
        File trustedDirectory = new File(new File(new File(new File(storageRoot, "xml-files"),
                Long.toString(xmlFileId)), "attachments"), attachmentId.toString()).getCanonicalFile();
        return readWithinTrustedDirectory(trustedDirectory, storedFileName);
    }

    /**
     * Kanonizálás után csak akkor olvassa be a fájlt, ha az a megadott megbízható gyökérkönyvtáron belül található.
     *
     * @param trustedDirectory a művelethez átadott {@code trustedDirectory} érték
     * @param fileName a művelethez átadott {@code fileName} érték
     * @return a művelet eredménye
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private byte[] readWithinTrustedDirectory(File trustedDirectory, String fileName) throws IOException {
        File safeDirectory = trustedDirectory.getCanonicalFile();
        requireWithinStorageRoot(safeDirectory);
        File candidate = new File(safeDirectory, fileName).getCanonicalFile();
        if (!safeDirectory.equals(candidate.getParentFile()) || !candidate.isFile()) {
            throw new IOException("Az M2M csatolmány nem a várt helyi tárolási könyvtárban található.");
        }
        try (BufferedInputStream input = new BufferedInputStream(new FileInputStream(candidate))) {
            return input.readAllBytes();
        }
    }

    /**
     * Az M2M életciklus vagy feldolgozási eredmény alapján frissíti a kezelt domain/runtime állapotot; a változás a hívó tranzakciójának része lehet.
     *
     * @param storedPath a művelethez átadott {@code storedPath} érték
     * @return a művelet eredménye
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private String storedFileName(String storedPath) throws IOException {
        if (storedPath == null || storedPath.isBlank()) {
            throw new IOException("Hiányzó M2M tárolási fájlútvonal.");
        }
        String name = Path.of(storedPath).getFileName().toString();
        if (name.isBlank() || name.equals(".") || name.equals("..") || name.contains("/") || name.contains("\\")) {
            throw new IOException("Érvénytelen M2M tárolási fájlnév.");
        }
        return name;
    }

    /**
     * Kanonizálja a kért fájlt és ellenőrzi, hogy a konfigurált menedzselt storage gyökéren belül marad-e; útvonal-kitörés esetén megtagadja a hozzáférést.
     *
     * @param candidate a művelethez átadott {@code candidate} érték
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private void requireWithinStorageRoot(File candidate) throws IOException {
        String rootPath = storageRoot.getPath();
        String candidatePath = candidate.getCanonicalPath();
        String requiredPrefix = rootPath.endsWith(File.separator) ? rootPath : rootPath + File.separator;
        if (!candidatePath.startsWith(requiredPrefix)) {
            throw new IOException("Az M2M fájlútvonal nem a konfigurált helyi tároló alatt található.");
        }
    }

    /**
     * A jelenlegi állapot és az M2M életciklusszabályok alapján eldönti, hogy a vizsgált feltétel teljesül-e.
     *
     * @param storedPath a művelethez átadott {@code storedPath} érték
     * @return igaz, ha a vizsgált feltétel teljesül; egyébként hamis
     */
    public boolean isReadableFile(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) return false;
        try {
            requireReadableFileInternal(new File(storedPath));
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    /**
     * A bemeneti struktúrából biztonságosan kiolvassa a művelethez szükséges értéket, és hiányzó adat esetén a metódus szerinti fallbacket alkalmazza.
     *
     * @param candidate a művelethez átadott {@code candidate} érték
     * @return a művelet eredménye
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private byte[] readCanonicalFile(File candidate) throws IOException {
        File safeFile = requireReadableFileInternal(candidate);
        try (BufferedInputStream input = new BufferedInputStream(new FileInputStream(safeFile))) {
            return input.readAllBytes();
        }
    }

    /**
     * Ellenőrzi, hogy a megadott útvonal a kezelt tárolási határon belüli, létező és olvasható fájlra mutat-e.
     *
     * <p>A tényleges kanonizálási és storage-boundary ellenőrzést a közös belső fájlellenőrző végzi. A metódus
     * {@link Path} alapú belépési pontot ad azoknak a hívóknak, amelyek nem {@link java.io.File} objektummal dolgoznak.</p>
     *
     * @param candidate az ellenőrizendő fájlútvonal
     * @return a biztonságosan feloldott és olvasható fájl normalizált útvonala
     * @throws IOException ha az útvonal hiányzik, kilép a kezelt tárolási gyökérből, nem fájl vagy nem olvasható
     */
    Path requireReadableFile(Path candidate) throws IOException {
        if (candidate == null) {
            throw new IOException("Hiányzó M2M tárolási fájlútvonal.");
        }
        return requireReadableFileInternal(candidate.toFile()).toPath();
    }

    /**
     * A fájl kanonikus útvonalát, storage-határát és olvashatóságát ellenőrzi, majd a biztonságosan olvasható fájlt adja vissza.
     *
     * @param candidate a művelethez átadott {@code candidate} érték
     * @return a művelet eredménye
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private File requireReadableFileInternal(File candidate) throws IOException {
        if (candidate == null) {
            throw new IOException("Hiányzó M2M tárolási fájlútvonal.");
        }
        File canonical = candidate.getCanonicalFile();
        String rootPath = storageRoot.getPath();
        String candidatePath = canonical.getPath();
        String requiredPrefix = rootPath.endsWith(File.separator) ? rootPath : rootPath + File.separator;
        if (!candidatePath.startsWith(requiredPrefix) || !canonical.isFile()) {
            throw new IOException("Az M2M fájlútvonal nem a konfigurált helyi tároló olvasható állományára mutat.");
        }
        return canonical;
    }
}
