package hu.gov.nav.xsdparsertool.web.security;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * A kapcsolódó folyamat lehetséges állapotait vagy működési módjait rögzítő típus.
 *
 * <p>A {@code SecurityMode} felsorolás a web modul biztonsági és jogosultságkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public enum SecurityMode {
    STANDALONE,
    MULTI_USER;

    /**
     * A {@code parse} művelet feldolgozza és normalizálja a bemeneti adatot a további feldolgozás számára.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param rawValue a művelet bemeneti {@code rawValue} értéke
     * @return a művelet feldolgozási eredménye
     */
    public static SecurityMode parse(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return STANDALONE;
        }
        String normalized = rawValue.trim().toUpperCase(Locale.ROOT);
        for (SecurityMode securityMode : values()) {
            if (securityMode.name().equals(normalized)) {
                return securityMode;
            }
        }
        throw new IllegalArgumentException("Nem támogatott security mód: " + rawValue
                + ". Támogatott értékek: " + supportedValues() + ".");
    }

    /**
     * A {@code supportedValues} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a művelet feldolgozási eredménye
     */
    public static String supportedValues() {
        return Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", "));
    }
}
