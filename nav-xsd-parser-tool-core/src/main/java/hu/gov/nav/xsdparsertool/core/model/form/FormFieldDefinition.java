package hu.gov.nav.xsdparsertool.core.model.form;

import java.util.ArrayList;
import java.util.List;
/**
 * Egy megjeleníthető űrlapmező teljes definíciója.
 *
 * <p>Az XML-kötés mellett tartalmazza a címkeforrásokat, a mezőtípust, kötelezőséget,
 * ismételhetőséget, láthatóságot, csak olvasható állapotot, beviteli korlátokat és
 * az enum értékeket. Az {@code xmlPath} a mező kontextusérzékeny azonosításának alapja.</p>
 */
public class FormFieldDefinition {
    private String id;
    private String xmlName;
    private String xmlPath;
    private String label;
    private String uiLabel;
    private String xsdLabel;
    private String type;
    private boolean required;
    private boolean repeatable;
    private boolean visible = true;
    private boolean readonly;
    private String mask;
    private Integer maxLength;
    private Integer layoutWidth;
    private List<String> enumValues = new ArrayList<>();
/**
 * Visszaadja a következő modellértéket: az objektum technikai azonosítója.
 *
 * @return az objektum technikai azonosítója
 */
public String getId() { return id; }
/**
 * Beállítja a következő modellértéket: az objektum technikai azonosítója.
 *
 * @param id az objektum technikai azonosítója
 */
public void setId(String id) { this.id = id; }
/**
 * Visszaadja a következő modellértéket: az elem XML-ben használt lokális neve.
 *
 * @return az elem XML-ben használt lokális neve
 */
public String getXmlName() { return xmlName; }
/**
 * Beállítja a következő modellértéket: az elem XML-ben használt lokális neve.
 *
 * @param xmlName az elem XML-ben használt lokális neve
 */
public void setXmlName(String xmlName) { this.xmlName = xmlName; }
/**
 * Visszaadja a következő modellértéket: az elem teljes, kontextusérzékeny XML-útvonala.
 *
 * @return az elem teljes, kontextusérzékeny XML-útvonala
 */
public String getXmlPath() { return xmlPath; }
/**
 * Beállítja a következő modellértéket: az elem teljes, kontextusérzékeny XML-útvonala.
 *
 * @param xmlPath az elem teljes, kontextusérzékeny XML-útvonala
 */
public void setXmlPath(String xmlPath) { this.xmlPath = xmlPath; }
/**
 * Visszaadja a következő modellértéket: a feldolgozás által kiválasztott effektív mezőcímke.
 *
 * @return a feldolgozás által kiválasztott effektív mezőcímke
 */
public String getLabel() { return label; }
/**
 * Beállítja a következő modellértéket: a feldolgozás által kiválasztott effektív mezőcímke.
 *
 * @param label a feldolgozás által kiválasztott effektív mezőcímke
 */
public void setLabel(String label) { this.label = label; }
/**
 * Visszaadja a következő modellértéket: a UIModelből származó mezőcímke.
 *
 * @return a UIModelből származó mezőcímke
 */
public String getUiLabel() { return uiLabel; }
/**
 * Beállítja a következő modellértéket: a UIModelből származó mezőcímke.
 *
 * @param uiLabel a UIModelből származó mezőcímke
 */
public void setUiLabel(String uiLabel) { this.uiLabel = uiLabel; }
/**
 * Visszaadja a következő modellértéket: az XSD dokumentációjából származó mezőcímke.
 *
 * @return az XSD dokumentációjából származó mezőcímke
 */
public String getXsdLabel() { return xsdLabel; }
/**
 * Beállítja a következő modellértéket: az XSD dokumentációjából származó mezőcímke.
 *
 * @param xsdLabel az XSD dokumentációjából származó mezőcímke
 */
public void setXsdLabel(String xsdLabel) { this.xsdLabel = xsdLabel; }
/**
 * Visszaadja a következő modellértéket: a megjelenítési vagy vezérlőtípus.
 *
 * @return a megjelenítési vagy vezérlőtípus
 */
public String getType() { return type; }
/**
 * Beállítja a következő modellértéket: a megjelenítési vagy vezérlőtípus.
 *
 * @param type a megjelenítési vagy vezérlőtípus
 */
public void setType(String type) { this.type = type; }
/**
 * Visszaadja a következő modellértéket: annak jelzése, hogy a mező kötelező-e.
 *
 * @return annak jelzése, hogy a mező kötelező-e
 */
public boolean isRequired() { return required; }
/**
 * Beállítja a következő modellértéket: annak jelzése, hogy a mező kötelező-e.
 *
 * @param required annak jelzése, hogy a mező kötelező-e
 */
public void setRequired(boolean required) { this.required = required; }
/**
 * Visszaadja a következő modellértéket: annak jelzése, hogy az elem ismétlődhet-e.
 *
 * @return annak jelzése, hogy az elem ismétlődhet-e
 */
public boolean isRepeatable() { return repeatable; }
/**
 * Beállítja a következő modellértéket: annak jelzése, hogy az elem ismétlődhet-e.
 *
 * @param repeatable annak jelzése, hogy az elem ismétlődhet-e
 */
public void setRepeatable(boolean repeatable) { this.repeatable = repeatable; }
/**
 * Visszaadja a következő modellértéket: annak jelzése, hogy a mező megjelenítendő-e.
 *
 * @return annak jelzése, hogy a mező megjelenítendő-e
 */
public boolean isVisible() { return visible; }
/**
 * Beállítja a következő modellértéket: annak jelzése, hogy a mező megjelenítendő-e.
 *
 * @param visible annak jelzése, hogy a mező megjelenítendő-e
 */
public void setVisible(boolean visible) { this.visible = visible; }
/**
 * Visszaadja a következő modellértéket: annak jelzése, hogy a mező csak olvasható-e.
 *
 * @return annak jelzése, hogy a mező csak olvasható-e
 */
public boolean isReadonly() { return readonly; }
/**
 * Beállítja a következő modellértéket: annak jelzése, hogy a mező csak olvasható-e.
 *
 * @param readonly annak jelzése, hogy a mező csak olvasható-e
 */
public void setReadonly(boolean readonly) { this.readonly = readonly; }
/**
 * Visszaadja a következő modellértéket: a mezőhöz tartozó beviteli vagy megjelenítési maszk.
 *
 * @return a mezőhöz tartozó beviteli vagy megjelenítési maszk
 */
public String getMask() { return mask; }
/**
 * Beállítja a következő modellértéket: a mezőhöz tartozó beviteli vagy megjelenítési maszk.
 *
 * @param mask a mezőhöz tartozó beviteli vagy megjelenítési maszk
 */
public void setMask(String mask) { this.mask = mask; }
    /**
     * Visszaadja a következő modellértéket: a mező megengedett maximális hossza.
     *
     * @return a mező megengedett maximális hossza
     */
    public Integer getMaxLength() { return maxLength; }
    /**
     * Beállítja a következő modellértéket: a mező megengedett maximális hossza.
     *
     * @param maxLength a mező megengedett maximális hossza
     */
    public void setMaxLength(Integer maxLength) { this.maxLength = maxLength; }
/**
 * Visszaadja a következő modellértéket: a UIModelből származó elrendezési szélesség.
 *
 * @return a UIModelből származó elrendezési szélesség
 */
public Integer getLayoutWidth() { return layoutWidth; }
/**
 * Beállítja a következő modellértéket: a UIModelből származó elrendezési szélesség.
 *
 * @param layoutWidth a UIModelből származó elrendezési szélesség
 */
public void setLayoutWidth(Integer layoutWidth) { this.layoutWidth = layoutWidth; }
/**
 * Visszaadja a következő modellértéket: a mező megengedett enum értékeinek listája.
 *
 * @return a mező megengedett enum értékeinek listája
 */
public List<String> getEnumValues() { return enumValues; }
/**
 * Beállítja a következő modellértéket: a mező megengedett enum értékeinek listája.
 *
 * @param enumValues a mező megengedett enum értékeinek listája
 */
public void setEnumValues(List<String> enumValues) { this.enumValues = enumValues; }
}
