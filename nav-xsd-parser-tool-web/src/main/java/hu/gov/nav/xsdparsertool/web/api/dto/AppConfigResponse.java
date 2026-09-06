package hu.gov.nav.xsdparsertool.web.api.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
/**
 * A(z) AppConfigResponse record fő feladata a(z) controller rétegben megvalósított funkciók biztosítása.
 * Az osztály a dto csomagból érhető el, és a projekt többi rétege innen hívja a publikus API-ját.
 * Spring regisztráció: Nincs közvetlen Spring bean regisztráció.
 * Interfész/implementáció kapcsolat: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *
 * Spring registration: Nincs közvetlen Spring bean regisztráció.
 * Interface/implementation relationship: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *

 */


public record AppConfigResponse(
        @Schema(description = "HU: XSD séma gyökérkönyvtára. EN: XSD schema root directory.") String schemaDir,
        @Schema(description = "HU: generalXsdPath mező. EN: generalXsdPath field.") String generalXsdPath,
        @Schema(description = "HU: defaultXmlPath mező. EN: defaultXmlPath field.") String defaultXmlPath,
        @Schema(description = "HU: xpathRuleDir mező. EN: xpathRuleDir field.") String xpathRuleDir,
        @Schema(description = "HU: uiModelDir mező. EN: uiModelDir field.") String uiModelDir,
        @Schema(description = "HU: appVersion mező. EN: appVersion field.") String appVersion,
        @Schema(description = "HU: Alapértelmezett űrlapmegjelenítő: classic vagy uimodel. EN: Default form renderer: classic or uimodel.") String formRendererDefault,
        @Schema(description = "HU: Header menüpontok láthatósága. EN: Header menu item visibility.") Map<String, Boolean> headerMenuVisibility,
        @Schema(description = "HU: Az XPath/XSD drawer oldala: left vagy right. EN: XPath/XSD drawer side: left or right.") String validationDrawerSide
) {
}
