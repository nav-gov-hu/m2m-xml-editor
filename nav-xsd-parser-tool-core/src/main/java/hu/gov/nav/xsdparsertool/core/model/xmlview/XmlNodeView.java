package hu.gov.nav.xsdparsertool.core.model.xmlview;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/**
 * Az XML-fa nézet egy megjeleníthető csomópontja.
 *
 * <p>Megőrzi a nevet, a kanonikus/indexelt útvonalat, levélelem esetén a szöveges
 * értéket, az attribútumokat és a gyermekcsomópontokat.</p>
 */
public class XmlNodeView {
    private String name;
    private String path;
    private String textValue;
    private boolean element;
    private Map<String, String> attributes = new LinkedHashMap<>();
    private List<XmlNodeView> children = new ArrayList<>();
/**
 * Visszaadja a következő modellértéket: az elem technikai vagy megjelenítési neve.
 *
 * @return az elem technikai vagy megjelenítési neve
 */
public String getName() { return name; }
/**
 * Beállítja a következő modellértéket: az elem technikai vagy megjelenítési neve.
 *
 * @param name az elem technikai vagy megjelenítési neve
 */
public void setName(String name) { this.name = name; }
/**
 * Visszaadja a következő modellértéket: a csomópont vagy probléma teljes XML-útvonala.
 *
 * @return a csomópont vagy probléma teljes XML-útvonala
 */
public String getPath() { return path; }
/**
 * Beállítja a következő modellértéket: a csomópont vagy probléma teljes XML-útvonala.
 *
 * @param path a csomópont vagy probléma teljes XML-útvonala
 */
public void setPath(String path) { this.path = path; }
/**
 * Visszaadja a következő modellértéket: levélelem esetén a csomópont szöveges értéke.
 *
 * @return levélelem esetén a csomópont szöveges értéke
 */
public String getTextValue() { return textValue; }
/**
 * Beállítja a következő modellértéket: levélelem esetén a csomópont szöveges értéke.
 *
 * @param textValue levélelem esetén a csomópont szöveges értéke
 */
public void setTextValue(String textValue) { this.textValue = textValue; }
/**
 * Visszaadja a következő modellértéket: annak jelzése, hogy a nézetcsomópont XML-elem-e.
 *
 * @return annak jelzése, hogy a nézetcsomópont XML-elem-e
 */
public boolean isElement() { return element; }
/**
 * Beállítja a következő modellértéket: annak jelzése, hogy a nézetcsomópont XML-elem-e.
 *
 * @param element annak jelzése, hogy a nézetcsomópont XML-elem-e
 */
public void setElement(boolean element) { this.element = element; }
/**
 * Visszaadja a következő modellértéket: az XML-csomópont attribútumainak név-érték térképe.
 *
 * @return az XML-csomópont attribútumainak név-érték térképe
 */
public Map<String, String> getAttributes() { return attributes; }
/**
 * Beállítja a következő modellértéket: az XML-csomópont attribútumainak név-érték térképe.
 *
 * @param Map<String az XML-csomópont attribútumainak név-érték térképe
 */
public void setAttributes(Map<String, String> attributes) { this.attributes = attributes; }
/**
 * Visszaadja a következő modellértéket: a közvetlen gyermekcsomópontok rendezett listája.
 *
 * @return a közvetlen gyermekcsomópontok rendezett listája
 */
public List<XmlNodeView> getChildren() { return children; }
/**
 * Beállítja a következő modellértéket: a közvetlen gyermekcsomópontok rendezett listája.
 *
 * @param children a közvetlen gyermekcsomópontok rendezett listája
 */
public void setChildren(List<XmlNodeView> children) { this.children = children; }
}
