package hu.gov.nav.xsdparsertool.web.xpath.dto;

import hu.gov.nav.xsdparsertool.web.xpath.model.ResultStatus;
import hu.gov.nav.xsdparsertool.web.xpath.model.ValidatorStatus;

import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;
/**
 * A(z) XPathValidationJournalDto record fő feladata a(z) model rétegben megvalósított funkciók biztosítása.
 * Az osztály a dto csomagból érhető el, és a projekt többi rétege innen hívja a publikus API-ját.
 * Spring regisztráció: Nincs közvetlen Spring bean regisztráció.
 * Interfész/implementáció kapcsolat: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *
 * Spring registration: Nincs közvetlen Spring bean regisztráció.
 * Interface/implementation relationship: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *

 */


public record XPathValidationJournalDto(
        @Schema(description = "HU: eventTimestampUtc mező. EN: eventTimestampUtc field.") Instant eventTimestampUtc,
        @Schema(description = "HU: oldValidatorStatus mező. EN: oldValidatorStatus field.") ValidatorStatus oldValidatorStatus,
        @Schema(description = "HU: newValidatorStatus mező. EN: newValidatorStatus field.") ValidatorStatus newValidatorStatus,
        @Schema(description = "HU: oldResultStatus mező. EN: oldResultStatus field.") ResultStatus oldResultStatus,
        @Schema(description = "HU: newResultStatus mező. EN: newResultStatus field.") ResultStatus newResultStatus,
        @Schema(description = "HU: message mező. EN: message field.") String message
) {}
