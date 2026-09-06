package hu.gov.nav.xsdparsertool.web.xpath.model;
/**
 * A(z) CreateResultMode enum fő feladata a(z) model rétegben megvalósított funkciók biztosítása.
 * Az osztály a model csomagból érhető el, és a projekt többi rétege innen hívja a publikus API-ját.
 * Spring regisztráció: Nincs közvetlen Spring bean regisztráció.
 * Interfész/implementáció kapcsolat: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *
 * Spring registration: Nincs közvetlen Spring bean regisztráció.
 * Interface/implementation relationship: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *

 */


public enum CreateResultMode {
    SYNC,
    ASYNC
}
