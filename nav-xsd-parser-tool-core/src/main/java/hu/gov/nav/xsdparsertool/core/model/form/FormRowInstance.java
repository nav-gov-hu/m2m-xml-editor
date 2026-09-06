package hu.gov.nav.xsdparsertool.core.model.form;

import java.util.LinkedHashMap;
import java.util.Map;
/**
 * Egy ismétlődő űrlapsor konkrét XML-példánya.
 *
 * <p>A példány saját azonosítóval és indexelt XML-útvonallal rendelkezik; a benne
 * lévő értékeket mezőazonosító szerint tárolja.</p>
 */
public class FormRowInstance {
    private String id;
    private String xmlPath;
    private Map<String, FormValue> valuesByFieldId = new LinkedHashMap<>();
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
 * Visszaadja a következő modellértéket: a mezőazonosító szerint elérhető konkrét űrlapértékek térképe.
 *
 * @return a mezőazonosító szerint elérhető konkrét űrlapértékek térképe
 */
public Map<String, FormValue> getValuesByFieldId() { return valuesByFieldId; }
/**
 * Beállítja a következő modellértéket: a mezőazonosító szerint elérhető konkrét űrlapértékek térképe.
 *
 * @param Map<String a mezőazonosító szerint elérhető konkrét űrlapértékek térképe
 */
public void setValuesByFieldId(Map<String, FormValue> valuesByFieldId) { this.valuesByFieldId = valuesByFieldId; }
}
