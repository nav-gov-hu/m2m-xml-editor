package hu.gov.nav.xsdparsertool.web.xpath.util;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * A web modul XPath-validációs területének közös alkalmazási típusa.
 *
 * <p>A {@code UuidV7Generator} osztály a web modul XPath-validációs területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public final class UuidV7Generator {
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Létrehozza a {@code UuidV7Generator} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     */
    private UuidV7Generator() {}
/**
 * Új UUIDv7 azonosítót generál és szabványos szöveges formában adja vissza.
 * @return a metódus által előállított eredmény
 */

    public static String newUuidV7String() {
        return newUuidV7().toString();
    }
/**
 * Időrendezhető UUIDv7 értéket állít elő az aktuális idő és véletlen komponensek felhasználásával.
 * @return a metódus által előállított eredmény
 */

    public static UUID newUuidV7() {
        long timestamp = System.currentTimeMillis();
        long msb = 0L;
        msb |= (timestamp & 0xFFFFFFFFFFFFL) << 16;
        msb |= 0x7000L;
        msb |= (RANDOM.nextInt(1 << 12) & 0x0FFFL);

        long lsb = RANDOM.nextLong();
        lsb &= 0x3FFFFFFFFFFFFFFFL;
        lsb |= 0x8000000000000000L;
        return new UUID(msb, lsb);
    }
}
