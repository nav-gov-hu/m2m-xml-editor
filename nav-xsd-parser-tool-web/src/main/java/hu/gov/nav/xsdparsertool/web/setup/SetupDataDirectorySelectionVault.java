package hu.gov.nav.xsdparsertool.web.setup;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Short-lived, one-time bridge between the HTTP setup request and filesystem setup.
 *
 * The browser supplied path is never forwarded to filesystem code. The controller stores
 * the validated selection under a server-generated opaque token, and SetupService consumes
 * that token exactly once. Besides making the trust boundary explicit, this prevents request
 * data from becoming a filesystem path argument in the setup service call graph.
 */
final class SetupDataDirectorySelectionVault {
    private static final String TOKEN_PREFIX = "setup-dir:";
    private static final int MAX_PATH_LENGTH = 4096;
    private static final long MAX_AGE_SECONDS = 120;
    private static final Map<String, Selection> SELECTIONS = new ConcurrentHashMap<>();

    /**
     * Létrehozza a {@code SetupDataDirectorySelectionVault} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     */
    private SetupDataDirectorySelectionVault() {
    }

    /**
     * A {@code issue} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a kezdeti beállítási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param rawPath a feldolgozásban részt vevő fájl vagy elérési út
     * @return a művelet feldolgozási eredménye
     */
    static String issue(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            throw new IllegalArgumentException("Az adatkönyvtár megadása kötelező.");
        }
        String value = rawPath.trim();
        if (value.length() > MAX_PATH_LENGTH || value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Az adatkönyvtár elérési útja érvénytelen.");
        }

        final Path normalized;
        try {
            normalized = Path.of(value).toAbsolutePath().normalize();
        } catch (InvalidPathException ex) {
            throw new IllegalArgumentException("Az adatkönyvtár elérési útja érvénytelen.", ex);
        }

        String token = TOKEN_PREFIX + UUID.randomUUID();
        SELECTIONS.put(token, new Selection(normalized, Instant.now()));
        purgeExpired();
        return token;
    }

    /**
     * A {@code consume} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a kezdeti beállítási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param token a művelet bemeneti {@code token} értéke
     * @return a művelet feldolgozási eredménye
     */
    static Path consume(String token) {
        if (token == null || !token.startsWith(TOKEN_PREFIX)) {
            throw new IllegalArgumentException("Érvénytelen adatkönyvtár-kiválasztási token.");
        }
        Selection selection = SELECTIONS.remove(token);
        if (selection == null || selection.createdAt().plusSeconds(MAX_AGE_SECONDS).isBefore(Instant.now())) {
            throw new IllegalArgumentException("Az adatkönyvtár-kiválasztás lejárt. Ismételje meg a beállítást.");
        }
        return selection.path();
    }

    /**
     * A {@code purgeExpired} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a kezdeti beállítási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     */
    private static void purgeExpired() {
        Instant threshold = Instant.now().minusSeconds(MAX_AGE_SECONDS);
        SELECTIONS.entrySet().removeIf(entry -> entry.getValue().createdAt().isBefore(threshold));
    }

    /**
     * A web modul kezdeti beállítási területének közös alkalmazási típusa.
     *
     * <p>A {@code Selection} rekord a web modul kezdeti beállítási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    private record Selection(Path path, Instant createdAt) {
    }
}
