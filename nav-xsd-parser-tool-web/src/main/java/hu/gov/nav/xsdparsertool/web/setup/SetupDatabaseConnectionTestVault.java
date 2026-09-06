package hu.gov.nav.xsdparsertool.web.setup;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rövid élettartamú szerveroldali bizonyítékot kezel a setup adatbázis-kapcsolati tesztjéhez.
 *
 * <p>A kliens csak egy véletlen tokent kap vissza. A tényleges adatbázis-paraméterek lenyomata
 * kizárólag szerveroldali memóriában marad, ezért a setup véglegesítésekor ellenőrizhető, hogy
 * pontosan ugyanazt a kapcsolatot tesztelték-e sikeresen, amelyet menteni kívánnak.</p>
 */
final class SetupDatabaseConnectionTestVault {

    private static final Duration VALIDITY = Duration.ofMinutes(15);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Map<String, Entry> ENTRIES = new ConcurrentHashMap<>();

    private SetupDatabaseConnectionTestVault() {
    }

    static String issue(String fingerprint) {
        purgeExpired();
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        ENTRIES.put(token, new Entry(digest(fingerprint), Instant.now().plus(VALIDITY)));
        return token;
    }

    static boolean matches(String token, String fingerprint) {
        if (token == null || token.isBlank() || fingerprint == null) {
            return false;
        }
        Entry entry = ENTRIES.get(token);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            ENTRIES.remove(token);
            return false;
        }
        return entry.fingerprintDigest().equals(digest(fingerprint));
    }

    static void revoke(String token) {
        if (token != null) {
            ENTRIES.remove(token);
        }
    }

    private static void purgeExpired() {
        Instant now = Instant.now();
        ENTRIES.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private static String digest(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("A SHA-256 lenyomatképzés nem érhető el.", ex);
        }
    }

    private record Entry(String fingerprintDigest, Instant expiresAt) {
    }
}
