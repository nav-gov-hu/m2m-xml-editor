package hu.gov.nav.xsdparsertool.web.xpath.dto;
import io.swagger.v3.oas.annotations.media.Schema;
/**
 * A(z) XPathValidatorConfigDto record fő feladata a(z) model rétegben megvalósított funkciók biztosítása.
 * Az osztály a dto csomagból érhető el, és a projekt többi rétege innen hívja a publikus API-ját.
 * Spring regisztráció: Nincs közvetlen Spring bean regisztráció.
 * Interfész/implementáció kapcsolat: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *
 * Spring registration: Nincs közvetlen Spring bean regisztráció.
 * Interface/implementation relationship: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *

 */


public record XPathValidatorConfigDto(
        @Schema(description = "HU: defaultPageSize mező. EN: defaultPageSize field.") int defaultPageSize,
        @Schema(description = "HU: defaultAutoRefreshSeconds mező. EN: defaultAutoRefreshSeconds field.") int defaultAutoRefreshSeconds
) {}
