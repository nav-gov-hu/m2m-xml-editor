package hu.gov.nav.xsdparsertool.core.model.instance;
/**
 * A dokumentumdefinícióhoz tartozó konkrét XML-példány fa-reprezentációja.
 *
 * <p>A modell a használt definíció azonosítóját és az XML-példány gyökércsomópontját tartalmazza.</p>
 */
public class DocumentInstance {
    private String definitionId;
    private InstanceNode root;
/**
 * Visszaadja a következő modellértéket: a dokumentumpéldányhoz tartozó definíció azonosítója.
 *
 * @return a dokumentumpéldányhoz tartozó definíció azonosítója
 */
public String getDefinitionId() {
        return definitionId;
    }
/**
 * Beállítja a következő modellértéket: a dokumentumpéldányhoz tartozó definíció azonosítója.
 *
 * @param definitionId a dokumentumpéldányhoz tartozó definíció azonosítója
 */
public void setDefinitionId(String definitionId) {
        this.definitionId = definitionId;
    }
/**
 * Visszaadja a következő modellértéket: a dokumentum vagy XML-nézet gyökércsomópontja.
 *
 * @return a dokumentum vagy XML-nézet gyökércsomópontja
 */
public InstanceNode getRoot() {
        return root;
    }
/**
 * Beállítja a következő modellértéket: a dokumentum vagy XML-nézet gyökércsomópontja.
 *
 * @param root a dokumentum vagy XML-nézet gyökércsomópontja
 */
public void setRoot(InstanceNode root) {
        this.root = root;
    }
}
