package hu.gov.nav.xsdparsertool.web.githubupdater.service;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.text.Normalizer;


/**
 * A GitHub repository/release azonosítókat biztonságos, egyetlen lokális
 * fájlrendszeri szegmensre képezi, és rooton belüli feloldást biztosít.
 */
final class GitHubPathSafety {

    /**
     * Létrehozza a(z) {@code GitHubPathSafety} példányt a működéshez szükséges kezdeti állapottal és függőségekkel.
     */
    private GitHubPathSafety() {
    }

    /**
     * Egy külső repository/tag azonosítót egyetlen biztonságos fájlrendszeri szegmenssé alakít. NFC-normalizálást végez, tiltott és vezérlő karaktereket helyettesít, hosszkorlátot alkalmaz, és elutasítja az abszolút vagy több szegmensű eredményt.
     *
     * @param value a feldolgozandó érték
     * @return a művelet eredménye
     */
    static String safeSegment(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("A fájlrendszeri útvonalszegmens nem lehet üres.");
        }
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFC);
        if (normalized.length() > 160) {
            throw new IllegalArgumentException("A fájlrendszeri útvonalszegmens túl hosszú.");
        }
        String safe = normalized.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_")
                .replaceFirst("[. ]+$", "")
                .trim();
        if (safe.isEmpty() || ".".equals(safe) || "..".equals(safe)) {
            throw new IllegalArgumentException("Érvénytelen fájlrendszeri útvonalszegmens: " + value);
        }
        try {
            Path segment = Path.of(safe);
            if (segment.isAbsolute() || segment.getNameCount() != 1 || !safe.equals(segment.getFileName().toString())) {
                throw new IllegalArgumentException("Érvénytelen fájlrendszeri útvonalszegmens: " + value);
            }
        } catch (InvalidPathException ex) {
            throw new IllegalArgumentException("Érvénytelen fájlrendszeri útvonalszegmens: " + value, ex);
        }
        return safe;
    }

    /**
     * Relatív útvonalat old fel a megadott root alatt. Abszolút, üres vagy felfelé kilépő útvonalat elutasít, majd a normalizált célról külön is ellenőrzi, hogy a rooton belül maradt-e.
     *
     * @param root a művelet gyökérkönyvtára
     * @param relative a művelethez átadott {@code relative} érték
     * @return a művelet eredménye
     */
    static Path resolveRelativeInside(Path root, Path relative) {
        if (root == null || relative == null) {
            throw new IllegalArgumentException("A root és a relatív útvonal megadása kötelező.");
        }
        if (relative.isAbsolute()) {
            throw new IllegalArgumentException("Abszolút útvonal nem engedélyezett: " + relative);
        }
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path normalizedRelative = relative.normalize();
        if (normalizedRelative.getNameCount() == 0
                || normalizedRelative.startsWith("..")
                || normalizedRelative.toString().isBlank()) {
            throw new IllegalArgumentException("A relatív útvonal a megengedett rooton kívülre mutat: " + relative);
        }
        Path target = normalizedRoot.resolve(normalizedRelative).normalize();
        if (!target.startsWith(normalizedRoot)) {
            throw new IllegalArgumentException("A célútvonal a megengedett root könyvtáron kívülre mutat: " + target);
        }
        return target;
    }

    /**
     * A megadott külső szegmenseket egyenként {@link #safeSegment(String)} segítségével tisztítja, majd a normalizált root alatt fűzi össze. A végső startsWith ellenőrzés megakadályozza a rooton kívülre mutató célútvonalat.
     *
     * @param root a művelet gyökérkönyvtára
     * @param segments a művelethez átadott {@code segments} érték
     * @return a művelet eredménye
     */
    static Path resolveInside(Path root, String... segments) {
        if (root == null) {
            throw new IllegalArgumentException("A fájlrendszeri root könyvtár nem lehet null.");
        }
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path target = normalizedRoot;
        if (segments != null) {
            for (String segment : segments) {
                target = target.resolve(safeSegment(segment));
            }
        }
        target = target.normalize();
        if (!target.startsWith(normalizedRoot)) {
            throw new IllegalArgumentException("A célútvonal a megengedett root könyvtáron kívülre mutat: " + target);
        }
        return target;
    }
}
