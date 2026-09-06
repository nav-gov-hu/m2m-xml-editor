package hu.gov.nav.xsdparsertool.core.model.form;
/**
 * Egy űrlapmező konkrét XML-értékét és kötési metaadatait tárolja.
 *
 * <p>A mezőazonosító mellett a teljes XML-útvonal is megmarad, így azonos technikai
 * mezőnév több kontextusban is biztonságosan megkülönböztethető. A {@code present}
 * jelző külön kezeli a hiányzó elemet és a létező, akár üres értékű elemet.</p>
 */
public class FormValue {
    private String key;
    private String fieldId;
    private String xmlPath;
    private String value;
    private boolean present;
/**
 * Visszaadja a következő modellértéket: az űrlapérték belső kulcsa.
 *
 * @return az űrlapérték belső kulcsa
 */
public String getKey() { return key; }
/**
 * Beállítja a következő modellértéket: az űrlapérték belső kulcsa.
 *
 * @param key az űrlapérték belső kulcsa
 */
public void setKey(String key) { this.key = key; }
/**
 * Visszaadja a következő modellértéket: a meződefiníció technikai azonosítója.
 *
 * @return a meződefiníció technikai azonosítója
 */
public String getFieldId() { return fieldId; }
/**
 * Beállítja a következő modellértéket: a meződefiníció technikai azonosítója.
 *
 * @param fieldId a meződefiníció technikai azonosítója
 */
public void setFieldId(String fieldId) { this.fieldId = fieldId; }
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
 * Visszaadja a következő modellértéket: az XML-ből származó vagy szerkesztett mezőérték.
 *
 * @return az XML-ből származó vagy szerkesztett mezőérték
 */
public String getValue() { return value; }
/**
 * Beállítja a következő modellértéket: az XML-ből származó vagy szerkesztett mezőérték.
 *
 * @param value az XML-ből származó vagy szerkesztett mezőérték
 */
public void setValue(String value) { this.value = value; }
/**
 * Visszaadja a következő modellértéket: annak jelzése, hogy a megfelelő XML-elem ténylegesen jelen van-e.
 *
 * @return annak jelzése, hogy a megfelelő XML-elem ténylegesen jelen van-e
 */
public boolean isPresent() { return present; }
/**
 * Beállítja a következő modellértéket: annak jelzése, hogy a megfelelő XML-elem ténylegesen jelen van-e.
 *
 * @param present annak jelzése, hogy a megfelelő XML-elem ténylegesen jelen van-e
 */
public void setPresent(boolean present) { this.present = present; }
}
