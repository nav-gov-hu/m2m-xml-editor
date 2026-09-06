package hu.gov.nav.xsdparsertool.web.api.dto;

import java.util.ArrayList;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
/**
 * Egy renderelhető űrlapmező technikai azonosítóit, teljes XML-útvonalát, címkéit, típusát és megjelenítési metaadatait hordozó DTO.
 * Az osztály a dto csomagból érhető el, és a projekt többi rétege innen hívja a publikus API-ját.
 * Spring regisztráció: Nincs közvetlen Spring bean regisztráció.
 * Interfész/implementáció kapcsolat: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *
 * Spring registration: Nincs közvetlen Spring bean regisztráció.
 * Interface/implementation relationship: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *

 */


public class FormFieldDto {
    @Schema(description = "HU: id mező. EN: id field.")
    private String id;
    @Schema(description = "HU: xmlName mező. EN: xmlName field.")
    private String xmlName;
    @Schema(description = "HU: xmlPath mező. EN: xmlPath field.")
    private String xmlPath;
    @Schema(description = "HU: label mező. EN: label field.")
    private String label;
    @Schema(description = "HU: uiLabel mező. EN: uiLabel field.")
    private String uiLabel;
    @Schema(description = "HU: xsdLabel mező. EN: xsdLabel field.")
    private String xsdLabel;
    @Schema(description = "HU: type mező. EN: type field.")
    private String type;
    @Schema(description = "HU: required mező. EN: required field.")
    private boolean required;
    @Schema(description = "HU: repeatable mező. EN: repeatable field.")
    private boolean repeatable;
    @Schema(description = "HU: visible mező. EN: visible field.")
    private boolean visible;
    @Schema(description = "HU: readonly mező. EN: readonly field.")
    private boolean readonly;
    @Schema(description = "HU: mask mező. EN: mask field.")
    private String mask;
    @Schema(description = "HU: maxLength mező. EN: maxLength field.")
    private Integer maxLength;
    @Schema(description = "HU: layoutWidth mező. EN: layoutWidth field.")
    private Integer layoutWidth;
    @Schema(description = "HU: enumValues mező. EN: enumValues field.")
    private List<String> enumValues = new ArrayList<>();
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
 * Visszaadja a {@code xmlName} mező aktuális értékét.
 * @return a {@code xmlName} mező értéke
 */
    public String getXmlName() { return xmlName; }
/**
 * Beállítja a {@code xmlName} mező értékét.
 * @param xmlName a beállítandó új érték
 */
    public void setXmlName(String xmlName) { this.xmlName = xmlName; }
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
 * Visszaadja a {@code label} mező aktuális értékét.
 * @return a {@code label} mező értéke
 */
    public String getLabel() { return label; }
/**
 * Beállítja a {@code label} mező értékét.
 * @param label a beállítandó új érték
 */
    public void setLabel(String label) { this.label = label; }
/**
 * Visszaadja a {@code uiLabel} mező aktuális értékét.
 * @return a {@code uiLabel} mező értéke
 */
    public String getUiLabel() { return uiLabel; }
/**
 * Beállítja a {@code uiLabel} mező értékét.
 * @param uiLabel a beállítandó új érték
 */
    public void setUiLabel(String uiLabel) { this.uiLabel = uiLabel; }
/**
 * Visszaadja a {@code xsdLabel} mező aktuális értékét.
 * @return a {@code xsdLabel} mező értéke
 */
    public String getXsdLabel() { return xsdLabel; }
/**
 * Beállítja a {@code xsdLabel} mező értékét.
 * @param xsdLabel a beállítandó új érték
 */
    public void setXsdLabel(String xsdLabel) { this.xsdLabel = xsdLabel; }
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
 * Megadja a {@code required} logikai állapot aktuális értékét.
 * @return a {@code required} mező értéke
 */
    public boolean isRequired() { return required; }
/**
 * Beállítja a {@code required} mező értékét.
 * @param required a beállítandó új érték
 */
    public void setRequired(boolean required) { this.required = required; }
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
 * Megadja a {@code visible} logikai állapot aktuális értékét.
 * @return a {@code visible} mező értéke
 */
    public boolean isVisible() { return visible; }
/**
 * Beállítja a {@code visible} mező értékét.
 * @param visible a beállítandó új érték
 */
    public void setVisible(boolean visible) { this.visible = visible; }
/**
 * Megadja a {@code readonly} logikai állapot aktuális értékét.
 * @return a {@code readonly} mező értéke
 */
    public boolean isReadonly() { return readonly; }
/**
 * Beállítja a {@code readonly} mező értékét.
 * @param readonly a beállítandó új érték
 */
    public void setReadonly(boolean readonly) { this.readonly = readonly; }
/**
 * Visszaadja a {@code mask} mező aktuális értékét.
 * @return a {@code mask} mező értéke
 */
    public String getMask() { return mask; }
/**
 * Beállítja a {@code mask} mező értékét.
 * @param mask a beállítandó új érték
 */
    public void setMask(String mask) { this.mask = mask; }
    /**
     * A {@code getMaxLength} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public Integer getMaxLength() { return maxLength; }
    /**
     * A {@code setMaxLength} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param maxLength a művelet bemeneti {@code maxLength} értéke
     */
    public void setMaxLength(Integer maxLength) { this.maxLength = maxLength; }
/**
 * Visszaadja a {@code layoutWidth} mező aktuális értékét.
 * @return a {@code layoutWidth} mező értéke
 */
    public Integer getLayoutWidth() { return layoutWidth; }
/**
 * Beállítja a {@code layoutWidth} mező értékét.
 * @param layoutWidth a beállítandó új érték
 */
    public void setLayoutWidth(Integer layoutWidth) { this.layoutWidth = layoutWidth; }
/**
 * Visszaadja a {@code enumValues} mező aktuális értékét.
 * @return a {@code enumValues} mező értéke
 */
    public List<String> getEnumValues() { return enumValues; }
/**
 * Beállítja a {@code enumValues} mező értékét.
 * @param enumValues a beállítandó új érték
 */
    public void setEnumValues(List<String> enumValues) { this.enumValues = enumValues; }
}
