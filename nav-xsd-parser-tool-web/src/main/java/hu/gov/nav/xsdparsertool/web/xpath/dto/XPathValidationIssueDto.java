package hu.gov.nav.xsdparsertool.web.xpath.dto;
import io.swagger.v3.oas.annotations.media.Schema;
/**
 * A(z) XPathValidationIssueDto record fő feladata a(z) model rétegben megvalósított funkciók biztosítása.
 * Az osztály a dto csomagból érhető el, és a projekt többi rétege innen hívja a publikus API-ját.
 * Spring regisztráció: Nincs közvetlen Spring bean regisztráció.
 * Interfész/implementáció kapcsolat: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *
 * Spring registration: Nincs közvetlen Spring bean regisztráció.
 * Interface/implementation relationship: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *

 */


public record XPathValidationIssueDto(
        @Schema(description = "HU: errorCode mező. EN: errorCode field.") String errorCode,
        @Schema(description = "HU: errorMessage mező. EN: errorMessage field.") String errorMessage,
        @Schema(description = "HU: severity mező. EN: severity field.") String severity,
        @Schema(description = "HU: dynamicPageIndex mező. EN: dynamicPageIndex field.") String dynamicPageIndex,
        @Schema(description = "HU: elementId mező. EN: elementId field.") String elementId,
        @Schema(description = "HU: ruleId mező. EN: ruleId field.") String ruleId,
        @Schema(description = "HU: path mező. EN: path field.") String path
) {}
