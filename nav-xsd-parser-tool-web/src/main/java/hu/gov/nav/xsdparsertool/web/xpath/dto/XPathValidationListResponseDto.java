package hu.gov.nav.xsdparsertool.web.xpath.dto;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
/**
 * A(z) XPathValidationListResponseDto record fő feladata a(z) model rétegben megvalósított funkciók biztosítása.
 * Az osztály a dto csomagból érhető el, és a projekt többi rétege innen hívja a publikus API-ját.
 * Spring regisztráció: Nincs közvetlen Spring bean regisztráció.
 * Interfész/implementáció kapcsolat: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *
 * Spring registration: Nincs közvetlen Spring bean regisztráció.
 * Interface/implementation relationship: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *

 */


public record XPathValidationListResponseDto(
        @Schema(description = "HU: items mező. EN: items field.") List<XPathValidationListItemDto> items,
        @Schema(description = "HU: limit mező. EN: limit field.") int limit,
        @Schema(description = "HU: query mező. EN: query field.") String query
) {}
