package hu.gov.nav.xsdparsertool.web.api.dto;
import io.swagger.v3.oas.annotations.media.Schema;
/**
 * Egy validációs hiba vagy figyelmeztetés kódját, súlyosságát, üzenetét és XML-útvonalát hordozó DTO.
 * Az osztály a dto csomagból érhető el, és a projekt többi rétege innen hívja a publikus API-ját.
 * Spring regisztráció: Nincs közvetlen Spring bean regisztráció.
 * Interfész/implementáció kapcsolat: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *
 * Spring registration: Nincs közvetlen Spring bean regisztráció.
 * Interface/implementation relationship: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *

 */


public class ValidationIssueDto {
    @Schema(description = "HU: code mező. EN: code field.")
    private String code;
    @Schema(description = "HU: path mező. EN: path field.")
    private String path;
    @Schema(description = "HU: message mező. EN: message field.")
    private String message;
    @Schema(description = "HU: severity mező. EN: severity field.")
    private String severity;
/**
 * Visszaadja a {@code code} mező aktuális értékét.
 * @return a {@code code} mező értéke
 */
    public String getCode() { return code; }
/**
 * Beállítja a {@code code} mező értékét.
 * @param code a beállítandó új érték
 */
    public void setCode(String code) { this.code = code; }
/**
 * Visszaadja a {@code path} mező aktuális értékét.
 * @return a {@code path} mező értéke
 */
    public String getPath() { return path; }
/**
 * Beállítja a {@code path} mező értékét.
 * @param path a beállítandó új érték
 */
    public void setPath(String path) { this.path = path; }
/**
 * Visszaadja a {@code message} mező aktuális értékét.
 * @return a {@code message} mező értéke
 */
    public String getMessage() { return message; }
/**
 * Beállítja a {@code message} mező értékét.
 * @param message a beállítandó új érték
 */
    public void setMessage(String message) { this.message = message; }
/**
 * Visszaadja a {@code severity} mező aktuális értékét.
 * @return a {@code severity} mező értéke
 */
    public String getSeverity() { return severity; }
/**
 * Beállítja a {@code severity} mező értékét.
 * @param severity a beállítandó új érték
 */
    public void setSeverity(String severity) { this.severity = severity; }
}
