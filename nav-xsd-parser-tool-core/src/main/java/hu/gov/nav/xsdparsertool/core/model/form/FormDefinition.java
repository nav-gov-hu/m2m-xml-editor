package hu.gov.nav.xsdparsertool.core.model.form;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/**
 * A felhasználói űrlap megjelenítéséhez szükséges, XML-értékektől független definíció.
 *
 * <p>Tabokból, szekciókból, sorokból és mezőkből áll. A strukturális címkéket teljes
 * útvonal szerint őrzi, hogy multiform dokumentumokban az azonos rövid nevű elemek
 * ne veszítsék el a kontextusukat.</p>
 */
public class FormDefinition {
    private String id;
    private String title;
    private List<FormTabDefinition> tabs = new ArrayList<>();
    private Map<String, String> structuralLabelsByPath = new LinkedHashMap<>();
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
 * Visszaadja a következő modellértéket: az űrlap tabjainak rendezett listája.
 *
 * @return az űrlap tabjainak rendezett listája
 */
public List<FormTabDefinition> getTabs() { return tabs; }
/**
 * Beállítja a következő modellértéket: az űrlap tabjainak rendezett listája.
 *
 * @param tabs az űrlap tabjainak rendezett listája
 */
public void setTabs(List<FormTabDefinition> tabs) { this.tabs = tabs; }
    /**
     * Visszaadja a következő modellértéket: a teljes strukturális XML/XSD-útvonalhoz rendelt címkék térképe.
     *
     * @return a teljes strukturális XML/XSD-útvonalhoz rendelt címkék térképe
     */
    public Map<String, String> getStructuralLabelsByPath() { return structuralLabelsByPath; }
    /**
     * Beállítja a következő modellértéket: a teljes strukturális XML/XSD-útvonalhoz rendelt címkék térképe.
     *
     * @param Map<String a teljes strukturális XML/XSD-útvonalhoz rendelt címkék térképe
     */
    public void setStructuralLabelsByPath(Map<String, String> structuralLabelsByPath) {
        this.structuralLabelsByPath = structuralLabelsByPath == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(structuralLabelsByPath);
    }
}
