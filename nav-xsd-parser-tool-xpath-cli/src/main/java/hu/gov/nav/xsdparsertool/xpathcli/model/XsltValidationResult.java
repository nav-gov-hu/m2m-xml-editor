package hu.gov.nav.xsdparsertool.xpathcli.model;

import java.util.List;

/**
 * Egy XSLT-alapú validáció eredményét hordozó változtathatatlan adatrekord.
 *
 * <p>A {@code rawOutputXml} a Saxon transzformáció teljes, nyers XML kimenete.
 * Az {@code errorMessages} azoknak a {@code Hiba}/{@code hiba} elemeknek a
 * szöveges üzeneteit tartalmazza, amelyeket a validációs szolgáltatás az
 * eredmény-XML-ből ki tudott nyerni.</p>
 *
 * @param rawOutputXml a transzformáció teljes XML kimenete
 * @param errorMessages a kinyert validációs hibaüzenetek listája
 */
public record XsltValidationResult(String rawOutputXml, List<String> errorMessages) {

    /**
     * Jelzi, hogy a transzformáció eredményéből legalább egy validációs hiba
     * került-e kinyerésre.
     *
     * @return {@code true}, ha a hibalista nem {@code null} és nem üres;
     *         egyébként {@code false}
     */
    public boolean hasErrors() {
        return errorMessages != null && !errorMessages.isEmpty();
    }
}
