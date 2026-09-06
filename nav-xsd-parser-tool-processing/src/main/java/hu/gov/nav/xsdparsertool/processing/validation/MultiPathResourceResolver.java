package hu.gov.nav.xsdparsertool.processing.validation;

import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


/**
 * XSD include/import hivatkozások lokális fájlrendszeri feloldását végző resolver.
 *
 * <p>A systemId és baseURI mellett az elsődleges XSD könyvtárat és az opcionális general XSD
 * könyvtárat is figyelembe veszi.</p>
 */
public class MultiPathResourceResolver implements LSResourceResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(MultiPathResourceResolver.class);

    private final Path primaryXsdDir;
    private final Path generalXsdDir;

/**
 * Létrehozza a resolvert a megadott séma-gyökerekkel.
 * @param primaryXsdDir az elsődleges dokumentumspecifikus XSD könyvtár
 * @param generalXsdDir az általános XSD-k könyvtára, vagy {@code null}
 */
    public MultiPathResourceResolver(Path primaryXsdDir, Path generalXsdDir) {
        this.primaryXsdDir = normalize(primaryXsdDir);
        this.generalXsdDir = normalize(generalXsdDir);
    }

    /**
     * Feloldja az XSD {@code include}, {@code import} és {@code redefine} hivatkozásait kizárólag helyi fájlokra.
     *
     * <p>A feloldás a {@code systemId} és a {@code baseURI} alapján indul, majd az elsődleges XSD-könyvtár és az
     * általános XSD-könyvtár felé esik vissza. Ha a közvetlen jelöltek nem léteznek, a resolver a fájlnév alapján
     * rekurzív keresést is végez a konfigurált helyi gyökerek alatt. Hálózati erőforrást nem tölt le.</p>
     *
     * @param type a feloldandó XML-erőforrás típusa
     * @param namespaceURI a hivatkozott névtér URI-ja
     * @param publicId a publikus azonosító, ha rendelkezésre áll
     * @param systemId a hivatkozott erőforrás rendszerazonosítója
     * @param baseURI a hivatkozás kiinduló URI-ja
     * @return a helyi fájlra épített {@link LSInput}, vagy {@code null}, ha biztonságosan nem oldható fel
     */
    @Override
    public LSInput resolveResource(
            String type,
            String namespaceURI,
            String publicId,
            String systemId,
            String baseURI) {

        try {
            Resolution resolution = resolvePath(systemId, baseURI);

            if (resolution.path() == null || !ExceptionSafeOperations.fileExists(resolution.path())) {
                LOGGER.warn(
                        "XSD resource not found. systemId={}, baseURI={}, primaryXsdDir={}, generalXsdDir={}, tried={}",
                        systemId,
                        baseURI,
                        primaryXsdDir,
                        generalXsdDir,
                        resolution.triedLocations()
                );
                return null;
            }

            if (!ExceptionSafeOperations.isRegularFile(resolution.path())) {
                throw new IllegalStateException(
                        "Resolved XSD resource is not a regular file: " + resolution.path()
                );
            }

            LOGGER.debug(
                    "Resolved XSD resource. systemId={}, baseURI={}, resolvedPath={}",
                    systemId,
                    baseURI,
                    resolution.path()
            );

            InputStream inputStream = Files.newInputStream(resolution.path());
            return new SimpleLsInput(publicId, resolution.path().toUri().toString(), inputStream);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to resolve XSD resource: systemId=" + systemId + ", baseURI=" + baseURI,
                    e
            );
        }
    }

    /**
     * A konfigurált keresési stratégiák sorrendjében feloldja az XSD-erőforrás tényleges fájlját.
     *
     * <p>A feloldási sorrend: {@code file:} URI, abszolút fájlrendszeri útvonal,
     * {@code baseURI}-hoz viszonyított hivatkozás, a primer XSD-könyvtár, az általános
     * XSD-könyvtár, végül fájlnév szerinti rekurzív keresés előbb a primer, majd az
     * általános könyvtárban.</p>
     *
     * <p>Minden értelmezhető próbálkozás bekerül a diagnosztikai listába, amely sikertelen
     * feloldáskor segít megállapítani, hol kereste a rendszer az include/import állományt.</p>
     *
     * @param systemId az XSD include/import hivatkozás rendszerazonosítója
     * @param baseURI a hivatkozó XSD bázis URI-ja
     * @return a feloldott útvonal és a megpróbált helyek listája; az útvonal
     *         {@code null}, ha egyik stratégia sem talált fájlt
     */
    private Resolution resolvePath(String systemId, String baseURI) {
        List<String> tried = new ArrayList<>();

        if (systemId == null || systemId.isBlank()) {
            return new Resolution(null, tried);
        }

        String trimmedSystemId = systemId.trim();

        Path fromSystemUri = tryResolveFileUri(trimmedSystemId, tried, "systemId-uri");
        if (exists(fromSystemUri)) {
            return new Resolution(fromSystemUri, tried);
        }

        Path absoluteSystemPath = tryResolveAbsolutePath(trimmedSystemId, tried, "systemId-absolute-path");
        if (exists(absoluteSystemPath)) {
            return new Resolution(absoluteSystemPath, tried);
        }

        Path fromBaseUri = tryResolveAgainstBaseUri(baseURI, trimmedSystemId, tried);
        if (exists(fromBaseUri)) {
            return new Resolution(fromBaseUri, tried);
        }

        Path fromPrimary = resolveAgainstRoot(primaryXsdDir, trimmedSystemId, tried, "primaryXsdDir");
        if (exists(fromPrimary)) {
            return new Resolution(fromPrimary, tried);
        }

        Path fromGeneral = resolveAgainstRoot(generalXsdDir, trimmedSystemId, tried, "generalXsdDir");
        if (exists(fromGeneral)) {
            return new Resolution(fromGeneral, tried);
        }

        Path fromPrimaryWalk = findByFileName(primaryXsdDir, extractFileName(trimmedSystemId), tried, "primaryXsdDir-walk");
        if (exists(fromPrimaryWalk)) {
            return new Resolution(fromPrimaryWalk, tried);
        }

        Path fromGeneralWalk = findByFileName(generalXsdDir, extractFileName(trimmedSystemId), tried, "generalXsdDir-walk");
        if (exists(fromGeneralWalk)) {
            return new Resolution(fromGeneralWalk, tried);
        }

        return new Resolution(null, tried);
    }

    /**
     * Megpróbálja a megadott értéket {@code file:} URI-ként fájlrendszeri útvonallá alakítani.
     *
     * @param value a vizsgált URI-szöveg
     * @param tried a diagnosztikai próbálkozások gyűjtőlistája
     * @param label a próbálkozás naplózási címkéje
     * @return a normalizált fájlútvonal, vagy {@code null}, ha az érték nem használható file URI-ként
     */
    private Path tryResolveFileUri(String value, List<String> tried, String label) {
        try {
            URI uri = URI.create(value);
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                Path path = Path.of(uri).normalize();
                tried.add(label + "=" + path);
                return path;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Megpróbálja a rendszerazonosítót közvetlen abszolút fájlrendszeri útvonalként értelmezni.
     *
     * @param value a vizsgált útvonalszöveg
     * @param tried a diagnosztikai próbálkozások gyűjtőlistája
     * @param label a próbálkozás naplózási címkéje
     * @return a normalizált abszolút útvonal, vagy {@code null}, ha a szöveg nem abszolút útvonal
     */
    private Path tryResolveAbsolutePath(String value, List<String> tried, String label) {
        try {
            Path path = Path.of(value);
            if (path.isAbsolute()) {
                Path normalized = path.normalize();
                tried.add(label + "=" + normalized);
                return normalized;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * A hivatkozó XSD bázis URI-jához képest próbálja feloldani a relatív rendszerazonosítót.
     *
     * <p>Elsőként URI-feloldást használ. Ha ez nem ad helyi fájlt, a bázis URI-ból
     * képzett fájlrendszeri útvonal könyvtárához viszonyítva is megpróbálja a feloldást.</p>
     *
     * @param baseURI a hivatkozó XSD bázis URI-ja
     * @param systemId a relatív include/import hivatkozás
     * @param tried a diagnosztikai próbálkozások gyűjtőlistája
     * @return a normalizált jelölt útvonal, vagy {@code null}, ha a bázis alapján nem oldható fel
     */
    private Path tryResolveAgainstBaseUri(String baseURI, String systemId, List<String> tried) {
        if (baseURI == null || baseURI.isBlank()) {
            return null;
        }

        try {
            URI base = URI.create(baseURI);
            URI resolved = base.resolve(systemId);

            if ("file".equalsIgnoreCase(resolved.getScheme())) {
                Path path = Path.of(resolved).normalize();
                tried.add("baseURI-resolve=" + path);
                return path;
            }
        } catch (Exception ignored) {
        }

        try {
            Path basePath = Path.of(URI.create(baseURI));
            Path parent = ExceptionSafeOperations.isDirectory(basePath) ? basePath : basePath.getParent();
            if (parent != null) {
                Path candidate = parent.resolve(systemId).normalize();
                tried.add("baseURI-parent-resolve=" + candidate);
                return candidate;
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    /**
     * Egy konfigurált XSD-gyökérkönyvtárhoz viszonyítva képez jelölt útvonalat.
     *
     * @param root a primer vagy általános XSD-gyökér
     * @param systemId a feloldandó relatív rendszerazonosító
     * @param tried a diagnosztikai próbálkozások gyűjtőlistája
     * @param label a gyökér megnevezése a diagnosztikában
     * @return a normalizált jelölt útvonal, vagy {@code null}, ha nem képezhető útvonal
     */
    private Path resolveAgainstRoot(Path root, String systemId, List<String> tried, String label) {
        if (root == null) {
            return null;
        }
        try {
            Path candidate = root.resolve(systemId).normalize();
            tried.add(label + "=" + candidate);
            return candidate;
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Végső fallbackként rekurzívan megkeresi a fájlnevet a megadott XSD-gyökér alatt.
     *
     * <p>A keresés csak létező könyvtáron indul el, és az első kis- és nagybetűtől
     * független fájlnév-egyezést adja vissza.</p>
     *
     * @param root a bejárandó gyökérkönyvtár
     * @param targetFileName a keresett fájlnév
     * @param tried a diagnosztikai próbálkozások gyűjtőlistája
     * @param label a keresés naplózási címkéje
     * @return az első megtalált fájl, vagy {@code null}, ha nincs találat
     */
    private Path findByFileName(Path root, String targetFileName, List<String> tried, String label) {
        if (root == null || targetFileName == null || targetFileName.isBlank() || !ExceptionSafeOperations.isDirectory(root)) {
            return null;
        }

        tried.add(label + "=" + root + "/**/" + targetFileName);

        try (var stream = Files.walk(root)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equalsIgnoreCase(targetFileName))
                    .findFirst()
                    .orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Ellenőrzi, hogy a jelölt útvonal létező fájlrendszeri erőforrásra mutat-e.
     *
     * @param path a vizsgált útvonal
     * @return {@code true}, ha az útvonal nem null és létezik
     */
    private boolean exists(Path path) {
        return path != null && ExceptionSafeOperations.fileExists(path);
    }

    /**
     * Normalizálja az útvonalat a redundáns szegmensek eltávolításával.
     *
     * @param path a normalizálandó útvonal
     * @return a normalizált útvonal, vagy {@code null}, ha a bemenet is {@code null}
     */
    private Path normalize(Path path) {
        return path == null ? null : path.normalize();
    }

    /**
     * Kinyeri a rendszerazonosítóból a fájlnév részt a rekurzív fallback kereséshez.
     *
     * <p>Először URI-ként próbálja értelmezni az értéket; sikertelen értelmezés esetén
     * a perjeleket egységesíti, és az utolsó útvonalszegmenst használja.</p>
     *
     * @param systemId az include/import rendszerazonosítója
     * @return a rendszerazonosítóból kinyert fájlnév
     */
    private String extractFileName(String systemId) {
        try {
            URI uri = URI.create(systemId);
            if (uri.getPath() != null && !uri.getPath().isBlank()) {
                String p = uri.getPath().replace('\\', '/');
                int idx = p.lastIndexOf('/');
                return idx >= 0 ? p.substring(idx + 1) : p;
            }
        } catch (Exception ignored) {
        }

        String normalized = systemId.replace('\\', '/');
        int idx = normalized.lastIndexOf('/');
        return idx >= 0 ? normalized.substring(idx + 1) : normalized;
    }

    /**
     * Egy XSD-erőforrás feloldási kísérlet eredményét fogja össze.
     *
     * @param path a sikeresen feloldott helyi útvonal, vagy {@code null}
     * @param triedLocations a feloldás során megvizsgált jelöltek diagnosztikai listája
     */
    private record Resolution(Path path, List<String> triedLocations) {
    }
}
