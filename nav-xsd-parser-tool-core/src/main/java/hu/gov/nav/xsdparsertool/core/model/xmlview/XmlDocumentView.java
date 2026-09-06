package hu.gov.nav.xsdparsertool.core.model.xmlview;
/**
 * Az XML-megtekintő számára előállított dokumentumreprezentáció.
 *
 * <p>A strukturált csomópontfa mellett opcionálisan a teljes nyers XML-szöveget is
 * tartalmazza. Nagy XML esetén a hívó réteg dönthet úgy, hogy a nyers forrást nem tölti be.</p>
 */
public class XmlDocumentView {
    private XmlNodeView root;
    private String rawXml;
/**
 * Visszaadja a következő modellértéket: a dokumentum vagy XML-nézet gyökércsomópontja.
 *
 * @return a dokumentum vagy XML-nézet gyökércsomópontja
 */
public XmlNodeView getRoot() { return root; }
/**
 * Beállítja a következő modellértéket: a dokumentum vagy XML-nézet gyökércsomópontja.
 *
 * @param root a dokumentum vagy XML-nézet gyökércsomópontja
 */
public void setRoot(XmlNodeView root) { this.root = root; }
/**
 * Visszaadja a következő modellértéket: a dokumentum teljes nyers XML-szövege.
 *
 * @return a dokumentum teljes nyers XML-szövege
 */
public String getRawXml() { return rawXml; }
/**
 * Beállítja a következő modellértéket: a dokumentum teljes nyers XML-szövege.
 *
 * @param rawXml a dokumentum teljes nyers XML-szövege
 */
public void setRawXml(String rawXml) { this.rawXml = rawXml; }
}
