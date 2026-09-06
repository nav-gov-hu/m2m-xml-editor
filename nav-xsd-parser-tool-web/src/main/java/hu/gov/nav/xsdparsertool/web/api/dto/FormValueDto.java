package hu.gov.nav.xsdparsertool.web.api.dto;
import io.swagger.v3.oas.annotations.media.Schema;
/**
 * Egy űrlapmező XML-ből feloldott értékét és annak kötési metaadatait hordozó DTO.
 * Az osztály a dto csomagból érhető el, és a projekt többi rétege innen hívja a publikus API-ját.
 * Spring regisztráció: Nincs közvetlen Spring bean regisztráció.
 * Interfész/implementáció kapcsolat: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *
 * Spring registration: Nincs közvetlen Spring bean regisztráció.
 * Interface/implementation relationship: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *

 */


public class FormValueDto {
    @Schema(description = "HU: key mező. EN: key field.")
    private String key;
    @Schema(description = "HU: fieldId mező. EN: fieldId field.")
    private String fieldId;
    @Schema(description = "HU: xmlPath mező. EN: xmlPath field.")
    private String xmlPath;
    @Schema(description = "HU: value mező. EN: value field.")
    private String value;
    @Schema(description = "HU: present mező. EN: present field.")
    private boolean present;
/**
 * Visszaadja a {@code key} mező aktuális értékét.
 * @return a {@code key} mező értéke
 */

    public String getKey() { return key; }
/**
 * Beállítja a {@code key} mező értékét.
 * @param key a beállítandó új érték
 */
    public void setKey(String key) { this.key = key; }
/**
 * Visszaadja a {@code fieldId} mező aktuális értékét.
 * @return a {@code fieldId} mező értéke
 */
    public String getFieldId() { return fieldId; }
/**
 * Beállítja a {@code fieldId} mező értékét.
 * @param fieldId a beállítandó új érték
 */
    public void setFieldId(String fieldId) { this.fieldId = fieldId; }
/**
 * Visszaadja a {@code xmlPath} mező aktuális értékét.
 * @return a {@code xmlPath} mező értéke
 */
    public String getXmlPath() { return xmlPath; }
/**
 * Beállítja a {@code xmlPath} mező értékét.
 * @param xmlPath a beállítandó új érték
 */
    public void setXmlPath(String xmlPath) { this.xmlPath = xmlPath; }
/**
 * Visszaadja a {@code value} mező aktuális értékét.
 * @return a {@code value} mező értéke
 */
    public String getValue() { return value; }
/**
 * Beállítja a {@code value} mező értékét.
 * @param value a beállítandó új érték
 */
    public void setValue(String value) { this.value = value; }
/**
 * Megadja a {@code present} logikai állapot aktuális értékét.
 * @return a {@code present} mező értéke
 */
    public boolean isPresent() { return present; }
/**
 * Beállítja a {@code present} mező értékét.
 * @param present a beállítandó új érték
 */
    public void setPresent(boolean present) { this.present = present; }
}
