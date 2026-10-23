package hu.gov.nav.xsdparsertool.web.setup;

import java.nio.file.Path;
import java.util.Set;


/**
 * A verziózott telepítések adatkönyvtárához tartozó, alkalmazás által kezelt útvonalak közös segédosztálya.
 *
 * <p>A felhasználó által külső helyre irányított útvonalakat nem módosítja. A korábbi verzió
 * adatkönyvtára alatti, alkalmazás által kezelt abszolút útvonalakat futásidőben az aktuális
 * {@code app.data.dir} alá képezi át, így a migráció során a tartalom másolható anélkül, hogy
 * a régi konfigurációs állományokat vagy az adatbázist át kellene írni.</p>
 */
public final class ManagedDataDirectoryPaths {

    public static final String LEGACY_DATA_DIRECTORY_PROPERTY = "m2m.xml.editor.migration.legacy-data-directory";

    private static final Set<String> RUNTIME_PATH_KEYS = Set.of(
            "nav.xsdparsertool.paths.schema-dir",
            "nav.xsdparsertool.paths.common-xsd-dir",
            "nav.xsdparsertool.paths.ui-model-dir",
            "nav.xsdparsertool.xpath-validator.xsl-root-dir",
            "nav.xsdparsertool.xpath-validator.rule-root-dir",
            "nav.xsdparsertool.xpath-validator.result-dir",
            "nav.xsdparsertool.xml-file.upload-dir",
            "nav.xsdparsertool.xml-file.backup-dir",
            "nav.xsdparsertool.xml-file.archive-dir",
            "nav.xsdparsertool.xml-file.xml-index-dir",
            "nav.xsdparsertool.xml-file.server-import.root-dir",
            "nav.xsdparsertool.xml-index.config-path",
            "nav.m2m.storage-directory"
    );

    private ManagedDataDirectoryPaths() {
    }

    /** Visszaadja, hogy a kulcs verziózott adatkönyvtár alatt kezelt futásidejű útvonal-e. */
    public static boolean isManagedRuntimePathKey(String key) {
        return RUNTIME_PATH_KEYS.contains(key);
    }

    /**
     * A régi adatkönyvtár alatti abszolút útvonalat az új adatkönyvtár alá képezi át.
     * Külső, placeholderes vagy nem útvonal jellegű érték változatlan marad.
     */
    public static String rebaseIfUnderLegacyRoot(String value, String legacyRoot, String currentRoot) {
        if (!hasText(value) || !hasText(legacyRoot) || !hasText(currentRoot)
                || value.contains("${")) {
            return value;
        }
        try {
            Path legacy = Path.of(legacyRoot).toAbsolutePath().normalize();
            Path current = Path.of(currentRoot).toAbsolutePath().normalize();
            Path candidate = Path.of(value).toAbsolutePath().normalize();
            if (!candidate.startsWith(legacy)) {
                return value;
            }
            Path relative = legacy.relativize(candidate);
            return normalize(current.resolve(relative));
        } catch (RuntimeException ignored) {
            return value;
        }
    }

    /**
     * Tetszőleges szövegben lecseréli a régi adatkönyvtár abszolút alakját az aktuálisra.
     * JDBC URL és spring.config.import esetén használható, ahol az útvonal nem önálló érték.
     */
    public static String rebaseEmbeddedLegacyRoot(String value, String legacyRoot, String currentRoot) {
        if (!hasText(value) || !hasText(legacyRoot) || !hasText(currentRoot)
                || value.contains("${")) {
            return value;
        }
        String normalizedLegacy = normalize(Path.of(legacyRoot).toAbsolutePath().normalize());
        String normalizedCurrent = normalize(Path.of(currentRoot).toAbsolutePath().normalize());
        String normalizedValue = value.replace('\\', '/');
        if (!normalizedValue.regionMatches(true, 0, normalizedLegacy, 0,
                Math.min(normalizedValue.length(), normalizedLegacy.length()))
                && !normalizedValue.toLowerCase(java.util.Locale.ROOT)
                .contains(normalizedLegacy.toLowerCase(java.util.Locale.ROOT))) {
            return value;
        }
        return replaceIgnoreCase(normalizedValue, normalizedLegacy, normalizedCurrent);
    }

    /** Az aktuális adatkönyvtár alatti útvonalat hordozható ${app.data.dir} alakra írja. */
    public static String toPortableValue(Path dataDirectory, Path path) {
        Path root = dataDirectory.toAbsolutePath().normalize();
        Path normalizedPath = path.toAbsolutePath().normalize();
        if (!normalizedPath.startsWith(root)) {
            return normalize(normalizedPath);
        }
        Path relative = root.relativize(normalizedPath);
        String suffix = normalize(relative);
        return suffix.isBlank() ? "${app.data.dir}" : "${app.data.dir}/" + suffix;
    }

    /** Normalizált, properties-ben hordozható perjeles útvonal. */
    public static String normalize(Path path) {
        return path.toString().replace('\\', '/');
    }


    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String replaceIgnoreCase(String source, String target, String replacement) {
        String lowerSource = source.toLowerCase(java.util.Locale.ROOT);
        String lowerTarget = target.toLowerCase(java.util.Locale.ROOT);
        StringBuilder result = new StringBuilder(source.length());
        int from = 0;
        int index;
        while ((index = lowerSource.indexOf(lowerTarget, from)) >= 0) {
            result.append(source, from, index).append(replacement);
            from = index + target.length();
        }
        result.append(source, from, source.length());
        return result.toString();
    }
}
