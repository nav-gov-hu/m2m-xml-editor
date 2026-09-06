package hu.gov.nav.xsdparsertool.web.xpath.util;

import java.security.SecureRandom;

/**
 * A web modul XPath-validációs területének közös alkalmazási típusa.
 *
 * <p>A {@code IdGenerator} osztály a web modul XPath-validációs területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public final class IdGenerator {
    private static final char[] ALPHANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Létrehozza a {@code IdGenerator} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     */
    private IdGenerator() {}
/**
 * Új, kérés/session-korrelációhoz használható technikai azonosítót generál.
 * @param length a {@code length} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 */

    public static String newSessionId(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUM[RANDOM.nextInt(ALPHANUM.length)]);
        }
        return sb.toString();
    }
}
