package hu.gov.nav.xsdparsertool.web.path;

import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * A verziószámozott könyvtárban tárolt űrlaperőforrások fájlneveit oldja fel.
 * Támogatja a korábbi, verziót is tartalmazó és az új, verzió nélküli fájlneveket.
 *
 * A nyomtatvány főverziója és a GitHub release-verzió eltérhet. Például az
 * 1.12 főverzióhoz az 1.12.1 release tartozhat. Ilyenkor az azonos főverziójú
 * release-k közül a legnagyobb patch verzió könyvtára az alapértelmezett.
 */
public final class VersionedArtifactPathResolver {
    private static final Pattern NUMERIC_VERSION = Pattern.compile("^(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?(?:[-+_].*)?$");

    /**
     * Létrehozza a {@code VersionedArtifactPathResolver} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     */
    private VersionedArtifactPathResolver() {
    }

    /**
     * A {@code resolveXpathRule} művelet feloldja a megfelelő erőforrást, állapotot vagy értéket a rendelkezésre álló jelöltek közül.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param ruleRoot a művelet bemeneti {@code ruleRoot} értéke
     * @param formType a művelet bemeneti {@code formType} értéke
     * @param formVersion a művelet bemeneti {@code formVersion} értéke
     * @return a feloldott vagy lekért érték
     */
    public static Path resolveXpathRule(Path ruleRoot, String formType, String formVersion) {
        Path formDirectory = ruleRoot.resolve(formType).toAbsolutePath().normalize();
        Path versionDirectory = resolvePreferredReleaseDirectory(formDirectory, formVersion);
        String releaseVersion = versionDirectory.getFileName() == null
                ? formVersion
                : versionDirectory.getFileName().toString();
        List<String> candidates = new ArrayList<>();
        candidates.add(formType + "_" + releaseVersion + "_xpath.xml");
        if (!releaseVersion.equals(formVersion)) {
            candidates.add(formType + "_" + formVersion + "_xpath.xml");
        }
        candidates.add(formType + "_xpath.xml");
        return firstExistingCandidate(versionDirectory, candidates);
    }

    /**
     * A {@code resolvePreferredReleaseDirectory} művelet feloldja a megfelelő erőforrást, állapotot vagy értéket a rendelkezésre álló jelöltek közül.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param formDirectory a művelet bemeneti {@code formDirectory} értéke
     * @param formVersion a művelet bemeneti {@code formVersion} értéke
     * @return a feloldott vagy lekért érték
     */
    public static Path resolvePreferredReleaseDirectory(Path formDirectory, String formVersion) {
        Path exact = formDirectory.resolve(formVersion).toAbsolutePath().normalize();
        if (!ExceptionSafeOperations.isDirectory(formDirectory)) return exact;

        try (Stream<Path> directories = Files.list(formDirectory)) {
            return directories
                    .filter(Files::isDirectory)
                    .filter(path -> hasSameFormVersion(path.getFileName().toString(), formVersion))
                    .max(Comparator.comparing(
                            path -> path.getFileName().toString(),
                            VersionedArtifactPathResolver::compareVersions))
                    .map(path -> path.toAbsolutePath().normalize())
                    .orElse(exact);
        } catch (IOException ignored) {
            return exact;
        }
    }

    /**
     * A {@code hasSameFormVersion} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param releaseVersion a művelet bemeneti {@code releaseVersion} értéke
     * @param formVersion a művelet bemeneti {@code formVersion} értéke
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    private static boolean hasSameFormVersion(String releaseVersion, String formVersion) {
        int[] release = numericParts(releaseVersion);
        int[] form = numericParts(formVersion);
        return release != null && form != null
                && release[0] == form[0]
                && release[1] == form[1];
    }

    /**
     * A {@code compareVersions} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param left a művelet bemeneti {@code left} értéke
     * @param right a művelet bemeneti {@code right} értéke
     * @return a művelet feldolgozási eredménye
     */
    private static int compareVersions(String left, String right) {
        int[] leftParts = numericParts(left);
        int[] rightParts = numericParts(right);
        if (leftParts == null || rightParts == null) {
            return left.compareToIgnoreCase(right);
        }
        for (int index = 0; index < leftParts.length; index++) {
            int compared = Integer.compare(leftParts[index], rightParts[index]);
            if (compared != 0) return compared;
        }
        return left.compareToIgnoreCase(right);
    }

    /**
     * A {@code numericParts} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param version a művelet bemeneti {@code version} értéke
     * @return a művelet feldolgozási eredménye
     */
    private static int[] numericParts(String version) {
        if (version == null) return null;
        Matcher matcher = NUMERIC_VERSION.matcher(version.trim());
        if (!matcher.matches()) return null;
        return new int[] {
                Integer.parseInt(matcher.group(1)),
                matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2)),
                matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3))
        };
    }

    /**
     * A {@code firstExistingCandidate} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param versionDirectory a művelet bemeneti {@code versionDirectory} értéke
     * @param candidateFileNames a feldolgozásban részt vevő fájl vagy elérési út
     * @return a művelet feldolgozási eredménye
     */
    private static Path firstExistingCandidate(Path versionDirectory, List<String> candidateFileNames) {
        for (String candidateFileName : candidateFileNames) {
            Path candidate = versionDirectory.resolve(candidateFileName).toAbsolutePath().normalize();
            if (ExceptionSafeOperations.isRegularFile(candidate)) {
                return candidate;
            }
        }

        if (ExceptionSafeOperations.isDirectory(versionDirectory)) {
            try (Stream<Path> files = Files.list(versionDirectory)) {
                List<String> normalizedCandidates = candidateFileNames.stream()
                        .map(name -> name.toLowerCase(Locale.ROOT))
                        .toList();
                Path caseInsensitiveMatch = files
                        .filter(Files::isRegularFile)
                        .filter(path -> normalizedCandidates.contains(
                                path.getFileName().toString().toLowerCase(Locale.ROOT)))
                        .findFirst()
                        .orElse(null);
                if (caseInsensitiveMatch != null) {
                    return caseInsensitiveMatch.toAbsolutePath().normalize();
                }
            } catch (IOException ignored) {
                // A hívó a preferált útvonal alapján ad részletes hibát vagy naplóbejegyzést.
            }
        }

        return versionDirectory.resolve(candidateFileNames.get(0)).toAbsolutePath().normalize();
    }
}
