package hu.gov.nav.xsdparsertool.core.model.definition;

import java.util.ArrayList;
import java.util.List;
/**
 * Az XSD-ből felépített dokumentumdefiníció egy logikai blokkját írja le.
 *
 * <p>A blokk azonosítóval, névvel és megjelenítési címmel rendelkezik, és a hozzá
 * tartozó meződefiníciókat deklarációs sorrendben tartalmazza.</p>
 */
public class BlockDefinition {
    private String id;
    private String name;
    private String title;
    private List<FieldDefinition> fields = new ArrayList<>();
/**
 * Visszaadja a következő modellértéket: az objektum technikai azonosítója.
 *
 * @return az objektum technikai azonosítója
 */
public String getId() {
        return id;
    }
/**
 * Beállítja a következő modellértéket: az objektum technikai azonosítója.
 *
 * @param id az objektum technikai azonosítója
 */
public void setId(String id) {
        this.id = id;
    }
/**
 * Visszaadja a következő modellértéket: az elem technikai vagy megjelenítési neve.
 *
 * @return az elem technikai vagy megjelenítési neve
 */
public String getName() {
        return name;
    }
/**
 * Beállítja a következő modellértéket: az elem technikai vagy megjelenítési neve.
 *
 * @param name az elem technikai vagy megjelenítési neve
 */
public void setName(String name) {
        this.name = name;
    }
/**
 * Visszaadja a következő modellértéket: a felhasználói felületen megjelenő cím.
 *
 * @return a felhasználói felületen megjelenő cím
 */
public String getTitle() {
        return title;
    }
/**
 * Beállítja a következő modellértéket: a felhasználói felületen megjelenő cím.
 *
 * @param title a felhasználói felületen megjelenő cím
 */
public void setTitle(String title) {
        this.title = title;
    }
/**
 * Visszaadja a következő modellértéket: a blokkhoz vagy sorhoz tartozó meződefiníciók rendezett listája.
 *
 * @return a blokkhoz vagy sorhoz tartozó meződefiníciók rendezett listája
 */
public List<FieldDefinition> getFields() {
        return fields;
    }
/**
 * Beállítja a következő modellértéket: a blokkhoz vagy sorhoz tartozó meződefiníciók rendezett listája.
 *
 * @param fields a blokkhoz vagy sorhoz tartozó meződefiníciók rendezett listája
 */
public void setFields(List<FieldDefinition> fields) {
        this.fields = fields;
    }
}
