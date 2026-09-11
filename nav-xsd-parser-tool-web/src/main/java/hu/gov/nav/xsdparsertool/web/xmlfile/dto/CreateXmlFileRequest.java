package hu.gov.nav.xsdparsertool.web.xmlfile.dto;

/**
 * Új, generált XML állomány létrehozási kérelme.
 *
 * @param formType az űrlap technikai azonosítója
 * @param formVersion az űrlap verziója
 * @param fileName a felhasználó által megadott XML fájlnév
 * @param partnerId a hozzárendelendő partner azonosítója
 * @param userNote opcionális felhasználói megjegyzés
 */
public record CreateXmlFileRequest(
        String formType,
        String formVersion,
        String fileName,
        Long partnerId,
        String userNote) {
}
