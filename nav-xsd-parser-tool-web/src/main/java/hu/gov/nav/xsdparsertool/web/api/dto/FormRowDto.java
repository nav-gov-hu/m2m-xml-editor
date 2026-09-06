package hu.gov.nav.xsdparsertool.web.api.dto;

import java.util.ArrayList;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
/**
 * Egy renderelhető űrlapsor definícióját és a sorhoz tartozó mezőket hordozó DTO.
 * Az osztály a dto csomagból érhető el, és a projekt többi rétege innen hívja a publikus API-ját.
 * Spring regisztráció: Nincs közvetlen Spring bean regisztráció.
 * Interfész/implementáció kapcsolat: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *
 * Spring registration: Nincs közvetlen Spring bean regisztráció.
 * Interface/implementation relationship: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *

 */


public class FormRowDto {
    @Schema(description = "HU: id mező. EN: id field.")
    private String id;
    @Schema(description = "HU: title mező. EN: title field.")
    private String title;
    @Schema(description = "HU: type mező. EN: type field.")
    private String type;
    @Schema(description = "HU: repeatable mező. EN: repeatable field.")
    private boolean repeatable;
    @Schema(description = "HU: xmlPath mező. EN: xmlPath field.")
    private String xmlPath;
    @Schema(description = "HU: fields mező. EN: fields field.")
    private List<FormFieldDto> fields = new ArrayList<>();
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
 * Visszaadja a {@code title} mező aktuális értékét.
 * @return a {@code title} mező értéke
 */
    public String getTitle() { return title; }
/**
 * Beállítja a {@code title} mező értékét.
 * @param title a beállítandó új érték
 */
    public void setTitle(String title) { this.title = title; }
/**
 * Visszaadja a {@code type} mező aktuális értékét.
 * @return a {@code type} mező értéke
 */
    public String getType() { return type; }
/**
 * Beállítja a {@code type} mező értékét.
 * @param type a beállítandó új érték
 */
    public void setType(String type) { this.type = type; }
/**
 * Megadja a {@code repeatable} logikai állapot aktuális értékét.
 * @return a {@code repeatable} mező értéke
 */
    public boolean isRepeatable() { return repeatable; }
/**
 * Beállítja a {@code repeatable} mező értékét.
 * @param repeatable a beállítandó új érték
 */
    public void setRepeatable(boolean repeatable) { this.repeatable = repeatable; }
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
 * Visszaadja a {@code fields} mező aktuális értékét.
 * @return a {@code fields} mező értéke
 */
    public List<FormFieldDto> getFields() { return fields; }
/**
 * Beállítja a {@code fields} mező értékét.
 * @param fields a beállítandó új érték
 */
    public void setFields(List<FormFieldDto> fields) { this.fields = fields; }
}
