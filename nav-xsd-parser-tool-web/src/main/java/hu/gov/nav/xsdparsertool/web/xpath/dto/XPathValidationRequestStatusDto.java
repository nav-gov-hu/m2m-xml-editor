package hu.gov.nav.xsdparsertool.web.xpath.dto;

import hu.gov.nav.xsdparsertool.web.xpath.model.CreateResultMode;
import hu.gov.nav.xsdparsertool.web.xpath.model.ResultStatus;
import hu.gov.nav.xsdparsertool.web.xpath.model.ValidatorStatus;

import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;
/**
 * A(z) XPathValidationRequestStatusDto record fő feladata a(z) model rétegben megvalósított funkciók biztosítása.
 * Az osztály a dto csomagból érhető el, és a projekt többi rétege innen hívja a publikus API-ját.
 * Spring regisztráció: Nincs közvetlen Spring bean regisztráció.
 * Interfész/implementáció kapcsolat: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *
 * Spring registration: Nincs közvetlen Spring bean regisztráció.
 * Interface/implementation relationship: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *

 */


public record XPathValidationRequestStatusDto(
        @Schema(description = "HU: requestId mező. EN: requestId field.") String requestId,
        @Schema(description = "HU: sessionId mező. EN: sessionId field.") String sessionId,
        @Schema(description = "HU: requestTimestampUtc mező. EN: requestTimestampUtc field.") Instant requestTimestampUtc,
        @Schema(description = "HU: formName mező. EN: formName field.") String formName,
        @Schema(description = "HU: formVersion mező. EN: formVersion field.") String formVersion,
        @Schema(description = "HU: mode mező. EN: mode field.") CreateResultMode mode,
        @Schema(description = "HU: validatorStatus mező. EN: validatorStatus field.") ValidatorStatus validatorStatus,
        @Schema(description = "HU: resultStatus mező. EN: resultStatus field.") ResultStatus resultStatus,
        @Schema(description = "HU: errorCount mező. EN: errorCount field.") Integer errorCount,
        @Schema(description = "HU: resultAvailable mező. EN: resultAvailable field.") boolean resultAvailable,
        @Schema(description = "HU: resultDownloadUrl mező. EN: resultDownloadUrl field.") String resultDownloadUrl,
        @Schema(description = "HU: timedOut mező. EN: timedOut field.") boolean timedOut,
        @Schema(description = "HU: createdAt mező. EN: createdAt field.") Instant createdAt,
        @Schema(description = "HU: updatedAt mező. EN: updatedAt field.") Instant updatedAt,
        @Schema(description = "HU: technicalErrorMessage mező. EN: technicalErrorMessage field.") String technicalErrorMessage,
        @Schema(description = "HU: resultXml mező. EN: resultXml field.") String resultXml,
        @Schema(description = "HU: processingJobId mező. EN: processingJobId field.") String processingJobId
) {}
