package hu.gov.nav.xsdparsertool.web.api.dto;
import io.swagger.v3.oas.annotations.media.Schema;
/**
 * A nyers XML és a navigálható XML-fa webes megjelenítéséhez szükséges adatokat hordozó DTO.
 * Az osztály a dto csomagból érhető el, és a projekt többi rétege innen hívja a publikus API-ját.
 * Spring regisztráció: Nincs közvetlen Spring bean regisztráció.
 * Interfész/implementáció kapcsolat: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *
 * Spring registration: Nincs közvetlen Spring bean regisztráció.
 * Interface/implementation relationship: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *

 */


public class XmlDocumentViewDto {
    @Schema(description = "HU: root mező. EN: root field.")
    private XmlNodeViewDto root;
    @Schema(description = "HU: rawXml mező. EN: rawXml field.")
    private String rawXml;
/**
 * Visszaadja a {@code root} mező aktuális értékét.
 * @return a {@code root} mező értéke
 */
    public XmlNodeViewDto getRoot() { return root; }
/**
 * Beállítja a {@code root} mező értékét.
 * @param root a beállítandó új érték
 */
    public void setRoot(XmlNodeViewDto root) { this.root = root; }
/**
 * Visszaadja a {@code rawXml} mező aktuális értékét.
 * @return a {@code rawXml} mező értéke
 */
    public String getRawXml() { return rawXml; }
/**
 * Beállítja a {@code rawXml} mező értékét.
 * @param rawXml a beállítandó új érték
 */
    public void setRawXml(String rawXml) { this.rawXml = rawXml; }
}
