package hu.gov.nav.xsdparsertool.web.xpath.dto;

import hu.gov.nav.xsdparsertool.web.xpath.model.ResultStatus;
import hu.gov.nav.xsdparsertool.web.xpath.model.ValidatorStatus;

import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;
/**
 * A(z) XPathValidationListItemDto record fő feladata a(z) model rétegben megvalósított funkciók biztosítása.
 * Az osztály a dto csomagból érhető el, és a projekt többi rétege innen hívja a publikus API-ját.
 * Spring regisztráció: Nincs közvetlen Spring bean regisztráció.
 * Interfész/implementáció kapcsolat: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *
 * Spring registration: Nincs közvetlen Spring bean regisztráció.
 * Interface/implementation relationship: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *

 */


public record XPathValidationListItemDto(
        @Schema(description = "HU: requestId mező. EN: requestId field.") String requestId,
        @Schema(description = "HU: requestTimestampUtc mező. EN: requestTimestampUtc field.") Instant requestTimestampUtc,
        @Schema(description = "HU: formName mező. EN: formName field.") String formName,
        @Schema(description = "HU: formVersion mező. EN: formVersion field.") String formVersion,
        @Schema(description = "HU: validatorStatus mező. EN: validatorStatus field.") ValidatorStatus validatorStatus,
        @Schema(description = "HU: resultStatus mező. EN: resultStatus field.") ResultStatus resultStatus,
        @Schema(description = "HU: errorCount mező. EN: errorCount field.") Integer errorCount,
        @Schema(description = "HU: createdAt mező. EN: createdAt field.") Instant createdAt,
        @Schema(description = "HU: updatedAt mező. EN: updatedAt field.") Instant updatedAt,
        @Schema(description = "HU: resultAvailable mező. EN: resultAvailable field.") boolean resultAvailable
) {}
