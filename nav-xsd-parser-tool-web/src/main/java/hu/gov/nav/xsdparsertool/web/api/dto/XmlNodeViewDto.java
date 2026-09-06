package hu.gov.nav.xsdparsertool.web.api.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;
/**
 * Egy XML-fa csomópont nevét, teljes indexelt útvonalát, értékét és gyermekcsomópontjait hordozó DTO.
 * Az osztály a dto csomagból érhető el, és a projekt többi rétege innen hívja a publikus API-ját.
 * Spring regisztráció: Nincs közvetlen Spring bean regisztráció.
 * Interfész/implementáció kapcsolat: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *
 * Spring registration: Nincs közvetlen Spring bean regisztráció.
 * Interface/implementation relationship: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *

 */


public class XmlNodeViewDto {
    @Schema(description = "HU: name mező. EN: name field.")
    private String name;
    @Schema(description = "HU: path mező. EN: path field.")
    private String path;
    @Schema(description = "HU: textValue mező. EN: textValue field.")
    private String textValue;
    @Schema(description = "HU: element mező. EN: element field.")
    private boolean element;
    @Schema(description = "HU: attributes mező. EN: attributes field.")
    private Map<String, String> attributes = new LinkedHashMap<>();
    @Schema(description = "HU: children mező. EN: children field.")
    private List<XmlNodeViewDto> children = new ArrayList<>();
/**
 * Visszaadja a {@code name} mező aktuális értékét.
 * @return a {@code name} mező értéke
 */
    public String getName() { return name; }
/**
 * Beállítja a {@code name} mező értékét.
 * @param name a beállítandó új érték
 */
    public void setName(String name) { this.name = name; }
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
 * Visszaadja a {@code textValue} mező aktuális értékét.
 * @return a {@code textValue} mező értéke
 */
    public String getTextValue() { return textValue; }
/**
 * Beállítja a {@code textValue} mező értékét.
 * @param textValue a beállítandó új érték
 */
    public void setTextValue(String textValue) { this.textValue = textValue; }
/**
 * Megadja a {@code element} logikai állapot aktuális értékét.
 * @return a {@code element} mező értéke
 */
    public boolean isElement() { return element; }
/**
 * Beállítja a {@code element} mező értékét.
 * @param element a beállítandó új érték
 */
    public void setElement(boolean element) { this.element = element; }
/**
 * Visszaadja a {@code attributes} mező aktuális értékét.
 * @return a {@code attributes} mező értéke
 */
    public Map<String, String> getAttributes() { return attributes; }
/**
 * Beállítja a {@code attributes} mező értékét.
 * @param attributes a beállítandó új érték
 */
    public void setAttributes(Map<String, String> attributes) { this.attributes = attributes; }
/**
 * Visszaadja a {@code children} mező aktuális értékét.
 * @return a {@code children} mező értéke
 */
    public List<XmlNodeViewDto> getChildren() { return children; }
/**
 * Beállítja a {@code children} mező értékét.
 * @param children a beállítandó új érték
 */
    public void setChildren(List<XmlNodeViewDto> children) { this.children = children; }
}
