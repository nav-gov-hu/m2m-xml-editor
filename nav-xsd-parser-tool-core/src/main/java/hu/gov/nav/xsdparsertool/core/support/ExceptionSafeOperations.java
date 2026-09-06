package hu.gov.nav.xsdparsertool.core.support;

import java.nio.file.Files;
import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/**
 * Olyan platform- és fájlrendszer-műveleteket fog össze, amelyek biztonsági korlátozás miatt kivételt dobhatnak.
 *
 * <p>A lekérdező műveletek {@link SecurityException} esetén biztonságos alapértékkel
 * térnek vissza; a könyvtárlétrehozás a projekt tulajdonosra korlátozott fájlműveleteit használja.</p>
 */
public final class ExceptionSafeOperations {

    /**
     * Privát konstruktor; a ExceptionSafeOperations segédosztály példányosítását megakadályozza.
     */
    private ExceptionSafeOperations() {
    }

    /**
     * Ellenőrzi, hogy a megadott fájlrendszeri útvonal létezik-e.
     *
     * <p>{@link SecurityException} esetén nem engedi tovább a kivételt, hanem {@code false} értékkel tér vissza.</p>
     * @param path az ellenőrizendő útvonal
     * @param options a {@link Files#exists(Path, LinkOption...)} hívás linkkezelési opciói
     * @return true, ha az útvonal létezik és az ellenőrzés végrehajtható; különben false
     */
    public static boolean fileExists(Path path, LinkOption... options) {
        try {
            return path != null && Files.exists(path, options);
        } catch (SecurityException ex) {
            return false;
        }
    }

    /**
     * Ellenőrzi, hogy a megadott útvonal létező normál fájlra mutat-e.
     *
     * <p>{@link SecurityException} esetén biztonságos alapértékként {@code false} az eredmény.</p>
     * @param path az ellenőrizendő útvonal
     * @param options a linkkezelési opciók
     * @return true, ha az útvonal normál fájl; különben false
     */
    public static boolean isRegularFile(Path path, LinkOption... options) {
        try {
            return path != null && Files.isRegularFile(path, options);
        } catch (SecurityException ex) {
            return false;
        }
    }

    /**
     * Ellenőrzi, hogy a megadott útvonal könyvtárra mutat-e.
     *
     * <p>{@link SecurityException} esetén biztonságos alapértékként {@code false} az eredmény.</p>
     * @param path az ellenőrizendő útvonal
     * @param options a linkkezelési opciók
     * @return true, ha az útvonal könyvtár; különben false
     */
    public static boolean isDirectory(Path path, LinkOption... options) {
        try {
            return path != null && Files.isDirectory(path, options);
        } catch (SecurityException ex) {
            return false;
        }
    }

    /**
     * Létrehozza a megadott könyvtárhierarchiát tulajdonosra korlátozott jogosultságokkal.
     *
     * <p>A tényleges létrehozást a {@link SecureFileOperations#createPrivateDirectories(Path)} végzi; a biztonsági és I/O hibákat egységes {@link IOException} formában adja tovább.</p>
     * @param path a létrehozandó könyvtár útvonala
     * @return a normalizált/létrehozott könyvtár útvonala
     * @throws IOException ha a könyvtár nem hozható létre
     */
    public static Path createDirectories(Path path) throws IOException {
        try {
            return SecureFileOperations.createPrivateDirectories(path);
        } catch (IOException | SecurityException ex) {
            throw new IOException("A könyvtár nem hozható létre: " + path, ex);
        }
    }

    /**
     * Kiolvassa a JVM rendszerproperty értékét úgy, hogy a SecurityManager vagy más futtatási korlátozás ne szakítsa meg a hívót.
     *
     * <p>Ha a property olvasása biztonsági okból tiltott, a megadott alapértékkel tér vissza.</p>
     * @param name a rendszerproperty neve
     * @return a property értéke vagy az alapérték
     */
    public static String systemProperty(String name) {
        return systemProperty(name, null);
    }

    /**
     * Kiolvassa a JVM rendszerproperty értékét úgy, hogy a SecurityManager vagy más futtatási korlátozás ne szakítsa meg a hívót.
     *
     * <p>Ha a property olvasása biztonsági okból tiltott, a megadott alapértékkel tér vissza.</p>
     * @param name a rendszerproperty neve
     * @param defaultValue a tiltott vagy hiányzó property esetén használt alapérték
     * @return a property értéke vagy az alapérték
     */
    public static String systemProperty(String name, String defaultValue) {
        try {
            return System.getProperty(name, defaultValue);
        } catch (SecurityException ex) {
            return defaultValue;
        }
    }
}
