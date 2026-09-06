package hu.gov.nav.xsdparsertool.web.api.dto;

import java.util.LinkedHashMap;
import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;
/**
 * Egy ismétlődő űrlapsor konkrét XML-előfordulásának indexét és mezőértékeit hordozó DTO.
 * Az osztály a dto csomagból érhető el, és a projekt többi rétege innen hívja a publikus API-ját.
 * Spring regisztráció: Nincs közvetlen Spring bean regisztráció.
 * Interfész/implementáció kapcsolat: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *
 * Spring registration: Nincs közvetlen Spring bean regisztráció.
 * Interface/implementation relationship: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *

 */


public class FormRowInstanceDto {
    @Schema(description = "HU: id mező. EN: id field.")
    private String id;
    @Schema(description = "HU: xmlPath mező. EN: xmlPath field.")
    private String xmlPath;
    @Schema(description = "HU: valuesByFieldId mező. EN: valuesByFieldId field.")
    private Map<String, FormValueDto> valuesByFieldId = new LinkedHashMap<>();
/**
 * Visszaadja a {@code id} mező aktuális értékét.
 * @return a {@code id} mező értéke
 */

    public String getId() { return id; }
/**
 * Beállítja a {@code id} mező értékét.
 * @param id a beállítandó új érték
 */
    public void setId(String id) { this.id = id; }
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
 * Visszaadja a {@code valuesByFieldId} mező aktuális értékét.
 * @return a {@code valuesByFieldId} mező értéke
 */
    public Map<String, FormValueDto> getValuesByFieldId() { return valuesByFieldId; }
/**
 * Beállítja a {@code valuesByFieldId} mező értékét.
 * @param valuesByFieldId a beállítandó új érték
 */
    public void setValuesByFieldId(Map<String, FormValueDto> valuesByFieldId) { this.valuesByFieldId = valuesByFieldId; }
}
