package hu.gov.nav.xsdparsertool.core.model.form;

import java.util.ArrayList;
import java.util.List;
/**
 * Egy űrlap tabján belüli megjelenítési szekció definíciója.
 *
 * <p>A szekció azonosítót, címet és a hozzá tartozó sorok rendezett listáját tartalmazza.</p>
 */
public class FormSectionDefinition {
    private String id;
    private String title;
    private List<FormRowDefinition> rows = new ArrayList<>();
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
 * Visszaadja a következő modellértéket: a szekcióhoz tartozó űrlapsorok rendezett listája.
 *
 * @return a szekcióhoz tartozó űrlapsorok rendezett listája
 */
public List<FormRowDefinition> getRows() { return rows; }
/**
 * Beállítja a következő modellértéket: a szekcióhoz tartozó űrlapsorok rendezett listája.
 *
 * @param rows a szekcióhoz tartozó űrlapsorok rendezett listája
 */
public void setRows(List<FormRowDefinition> rows) { this.rows = rows; }
}
