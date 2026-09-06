package hu.gov.nav.xsdparsertool.core.model.form;

import java.util.ArrayList;
import java.util.List;
/**
 * Az űrlap legfelső megjelenítési csoportjának, egy tabnak a definíciója.
 *
 * <p>A tab azonosítót, címet és szekciókat tartalmaz; a UIModel-first feldolgozás
 * eredményeként ez adja az űrlap fő navigációs szerkezetét.</p>
 */
public class FormTabDefinition {
    private String id;
    private String title;
    private List<FormSectionDefinition> sections = new ArrayList<>();
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
 * Visszaadja a következő modellértéket: a tabhoz tartozó szekciók rendezett listája.
 *
 * @return a tabhoz tartozó szekciók rendezett listája
 */
public List<FormSectionDefinition> getSections() { return sections; }
/**
 * Beállítja a következő modellértéket: a tabhoz tartozó szekciók rendezett listája.
 *
 * @param sections a tabhoz tartozó szekciók rendezett listája
 */
public void setSections(List<FormSectionDefinition> sections) { this.sections = sections; }
}
