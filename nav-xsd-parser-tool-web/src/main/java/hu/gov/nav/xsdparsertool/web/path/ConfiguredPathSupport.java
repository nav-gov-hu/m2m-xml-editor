package hu.gov.nav.xsdparsertool.web.path;

import java.io.File;
import java.nio.file.Path;

/**
 * A konfigurációból érkező fájlrendszer-útvonalak egységes feldolgozása.
 *
 * <p>A Windows telepítő által generált konfigurációk használhatnak perjelet,
 * míg a kézzel készített konfigurációk gyakran visszaperjelet tartalmaznak.
 * A fájlrendszer-útvonalakat ezért nem szövegként daraboljuk, hanem előbb az
 * aktuális operációs rendszer elválasztójára normalizáljuk, majd {@link Path}
 * objektummal dolgozzuk fel.</p>
 */
public final class ConfiguredPathSupport {

    /**
     * Létrehozza a {@code ConfiguredPathSupport} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     */
    private ConfiguredPathSupport() {
    }

    /**
     * A megadott konfigurációs útvonalat elválasztófüggetlen módon abszolút,
     * normalizált {@link Path} objektummá alakítja.
     *
     * @param value konfigurációból érkező útvonal
     * @return abszolút, normalizált útvonal
     * @throws IllegalArgumentException ha az érték üres
     */
    public static Path toAbsoluteNormalizedPath(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Az útvonal nem lehet üres.");
        }
        return Path.of(normalizeSeparators(value.trim())).toAbsolutePath().normalize();
    }

    /**
     * Mindkét elválasztó karaktert az aktuális operációs rendszer natív
     * elválasztójára cseréli. Ez a meghajtóbetűt, UNC előtagot és a relatív
     * útvonalrészeket nem módosítja.
     */
    public static String normalizeSeparators(String value) {
        if (value == null) {
            return null;
        }
        char separator = File.separatorChar;
        return value.replace('\\', separator).replace('/', separator);
    }
}
