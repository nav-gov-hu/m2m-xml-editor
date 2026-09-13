package hu.gov.nav.xsdparsertool.core.model.form;

import java.util.ArrayList;
import java.util.List;
/**
 * Egy űrlapszekción belüli logikai sor definíciója.
 *
 * <p>A sor lehet egyszeri vagy ismétlődő, saját XML-útvonallal rendelkezhet, és a
 * hozzá tartozó meződefiníciókat deklarációs sorrendben tartalmazza.</p>
 */
public class FormRowDefinition {
    private String id;
    private String title;
    private String type;
    private boolean repeatable;
    private String xmlPath;
    private String repeatContainerPath;
    private Integer minOccurs;
    private String maxOccurs;
    private List<FormFieldDefinition> fields = new ArrayList<>();
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
 * Visszaadja a következő modellértéket: a felhasználói felületen megjelenő cím.
 *
 * @return a felhasználói felületen megjelenő cím
 */
public String getTitle() { return title; }
/**
 * Beállítja a következő modellértéket: a felhasználói felületen megjelenő cím.
 *
 * @param title a felhasználói felületen megjelenő cím
 */
public void setTitle(String title) { this.title = title; }
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
 * Visszaadja az ismétlődő sor legkülső, egy példányként kezelendő XML-konténerének útvonalát.
 *
 * @return az ismétlődő konténer útvonala, vagy {@code null}
 */
public String getRepeatContainerPath() { return repeatContainerPath; }
/**
 * Beállítja az ismétlődő sor egy példányként kezelendő XML-konténerének útvonalát.
 *
 * @param repeatContainerPath az ismétlődő konténer útvonala
 */
public void setRepeatContainerPath(String repeatContainerPath) { this.repeatContainerPath = repeatContainerPath; }
/**
 * Visszaadja az ismétlődő konténer minimális előfordulásszámát.
 *
 * @return minimális előfordulásszám
 */
public Integer getMinOccurs() { return minOccurs; }
/**
 * Beállítja az ismétlődő konténer minimális előfordulásszámát.
 *
 * @param minOccurs minimális előfordulásszám
 */
public void setMinOccurs(Integer minOccurs) { this.minOccurs = minOccurs; }
/**
 * Visszaadja az ismétlődő konténer maximális előfordulásszámát.
 *
 * @return maximális előfordulásszám vagy {@code unbounded}
 */
public String getMaxOccurs() { return maxOccurs; }
/**
 * Beállítja az ismétlődő konténer maximális előfordulásszámát.
 *
 * @param maxOccurs maximális előfordulásszám vagy {@code unbounded}
 */
public void setMaxOccurs(String maxOccurs) { this.maxOccurs = maxOccurs; }
/**
 * Visszaadja a következő modellértéket: a blokkhoz vagy sorhoz tartozó meződefiníciók rendezett listája.
 *
 * @return a blokkhoz vagy sorhoz tartozó meződefiníciók rendezett listája
 */
public List<FormFieldDefinition> getFields() { return fields; }
/**
 * Beállítja a következő modellértéket: a blokkhoz vagy sorhoz tartozó meződefiníciók rendezett listája.
 *
 * @param fields a blokkhoz vagy sorhoz tartozó meződefiníciók rendezett listája
 */
public void setFields(List<FormFieldDefinition> fields) { this.fields = fields; }
}
