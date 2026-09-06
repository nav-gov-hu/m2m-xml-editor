package hu.gov.nav.xsdparsertool.core.model.instance;

import hu.gov.nav.xsdparsertool.core.enums.PresenceState;

import java.util.ArrayList;
import java.util.List;
/**
 * Egy feldolgozott XML-példány egy csomópontját reprezentálja.
 *
 * <p>A csomópont megőrzi az XML-nevet, a teljes útvonalat, jelenléti állapotot,
 * értéket és a gyermekcsomópontokat. Ez a reprezentáció a dokumentumpéldány
 * strukturális bejárására szolgál.</p>
 */
public class InstanceNode {
    private String id;
    private String xmlName;
    private String path;
    private PresenceState presenceState;
    private String value;
    private List<InstanceNode> children = new ArrayList<>();
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
 * Visszaadja a következő modellértéket: az elem XML-ben használt lokális neve.
 *
 * @return az elem XML-ben használt lokális neve
 */
public String getXmlName() {
        return xmlName;
    }
/**
 * Beállítja a következő modellértéket: az elem XML-ben használt lokális neve.
 *
 * @param xmlName az elem XML-ben használt lokális neve
 */
public void setXmlName(String xmlName) {
        this.xmlName = xmlName;
    }
/**
 * Visszaadja a következő modellértéket: a csomópont vagy probléma teljes XML-útvonala.
 *
 * @return a csomópont vagy probléma teljes XML-útvonala
 */
public String getPath() {
        return path;
    }
/**
 * Beállítja a következő modellértéket: a csomópont vagy probléma teljes XML-útvonala.
 *
 * @param path a csomópont vagy probléma teljes XML-útvonala
 */
public void setPath(String path) {
        this.path = path;
    }
/**
 * Visszaadja a következő modellértéket: a csomópont jelenléti állapota.
 *
 * @return a csomópont jelenléti állapota
 */
public PresenceState getPresenceState() {
        return presenceState;
    }
/**
 * Beállítja a következő modellértéket: a csomópont jelenléti állapota.
 *
 * @param presenceState a csomópont jelenléti állapota
 */
public void setPresenceState(PresenceState presenceState) {
        this.presenceState = presenceState;
    }
/**
 * Visszaadja a következő modellértéket: az XML-ből származó vagy szerkesztett mezőérték.
 *
 * @return az XML-ből származó vagy szerkesztett mezőérték
 */
public String getValue() {
        return value;
    }
/**
 * Beállítja a következő modellértéket: az XML-ből származó vagy szerkesztett mezőérték.
 *
 * @param value az XML-ből származó vagy szerkesztett mezőérték
 */
public void setValue(String value) {
        this.value = value;
    }
/**
 * Visszaadja a következő modellértéket: a közvetlen gyermekcsomópontok rendezett listája.
 *
 * @return a közvetlen gyermekcsomópontok rendezett listája
 */
public List<InstanceNode> getChildren() {
        return children;
    }
/**
 * Beállítja a következő modellértéket: a közvetlen gyermekcsomópontok rendezett listája.
 *
 * @param children a közvetlen gyermekcsomópontok rendezett listája
 */
public void setChildren(List<InstanceNode> children) {
        this.children = children;
    }
}
