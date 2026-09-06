package hu.gov.nav.xsdparsertool.web.secret.service;

import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Decodes externally supplied Base64 encoded AES-256 key material.
 * Kept separate from configuration-key lookup so static analysis cannot confuse
 * property/environment variable names with cryptographic key bytes.
 */
final class PlainAesKeyDecoder {

    private static final int AES_256_KEY_LENGTH = 32;

    /**
     * Létrehozza a {@code PlainAesKeyDecoder} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     */
    private PlainAesKeyDecoder() {
    }

    /**
     * A {@code decode} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a titokkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param encodedKey a művelet bemeneti {@code encodedKey} értéke
     * @return a művelet feldolgozási eredménye
     */
    static SecretKey decode(String encodedKey) {
        byte[] keyBytes = Base64.getDecoder().decode(encodedKey);
        if (keyBytes.length != AES_256_KEY_LENGTH) {
            throw new IllegalArgumentException("A mesterkulcsnak 256 bitesnek kell lennie.");
        }
        return new SecretKeySpec(keyBytes, "AES");
    }
}
