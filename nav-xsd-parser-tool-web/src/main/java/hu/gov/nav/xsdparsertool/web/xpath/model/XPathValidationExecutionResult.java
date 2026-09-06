package hu.gov.nav.xsdparsertool.web.xpath.model;

import java.util.List;
/**
 * A(z) XPathValidationExecutionResult record fő feladata a(z) model rétegben megvalósított funkciók biztosítása.
 * Az osztály a model csomagból érhető el, és a projekt többi rétege innen hívja a publikus API-ját.
 * Spring regisztráció: Nincs közvetlen Spring bean regisztráció.
 * Interfész/implementáció kapcsolat: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *
 * Spring registration: Nincs közvetlen Spring bean regisztráció.
 * Interface/implementation relationship: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *

 */


public record XPathValidationExecutionResult(String rawOutputXml, List<XPathValidationIssue> issues) {
/**
 * Megadja, hogy a {@code errors} feltétel teljesül-e.
 * @return a {@code errors} mező értéke
 */
    public boolean hasErrors() {
        return issues != null && !issues.isEmpty();
    }
}
