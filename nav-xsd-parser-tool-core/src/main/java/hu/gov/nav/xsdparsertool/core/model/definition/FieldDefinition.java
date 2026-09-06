package hu.gov.nav.xsdparsertool.core.model.definition;

import java.util.ArrayList;
import java.util.List;
/**
 * Egy XSD-elemből feloldott mező szerkezeti és megjelenítési metaadatai.
 *
 * <p>A mező az XML-nevet és teljes útvonalat, típust, előfordulási korlátokat,
 * címkéket, maszkot, maximális hosszt és az esetleges enum értékkészletet tartalmazza.</p>
 */
public class FieldDefinition {
    private String id;
    private String xmlName;
    private String xmlPath;
    private String dataType;
    private boolean required;
    private Integer minOccurs;
    private String maxOccurs;
    private String label;
    private String uiLabel;
    private String xsdLabel;
    private String mask;
    private Integer maxLength;
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
 * Visszaadja a következő modellértéket: az XSD-ből feloldott adattípus.
 *
 * @return az XSD-ből feloldott adattípus
 */
public String getDataType() { return dataType; }
/**
 * Beállítja a következő modellértéket: az XSD-ből feloldott adattípus.
 *
 * @param dataType az XSD-ből feloldott adattípus
 */
public void setDataType(String dataType) { this.dataType = dataType; }
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
 * Visszaadja a következő modellértéket: az XSD szerinti minimális előfordulásszám.
 *
 * @return az XSD szerinti minimális előfordulásszám
 */
public Integer getMinOccurs() { return minOccurs; }
/**
 * Beállítja a következő modellértéket: az XSD szerinti minimális előfordulásszám.
 *
 * @param minOccurs az XSD szerinti minimális előfordulásszám
 */
public void setMinOccurs(Integer minOccurs) { this.minOccurs = minOccurs; }
/**
 * Visszaadja a következő modellértéket: az XSD szerinti maximális előfordulásszám.
 *
 * @return az XSD szerinti maximális előfordulásszám
 */
public String getMaxOccurs() { return maxOccurs; }
/**
 * Beállítja a következő modellértéket: az XSD szerinti maximális előfordulásszám.
 *
 * @param maxOccurs az XSD szerinti maximális előfordulásszám
 */
public void setMaxOccurs(String maxOccurs) { this.maxOccurs = maxOccurs; }
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
