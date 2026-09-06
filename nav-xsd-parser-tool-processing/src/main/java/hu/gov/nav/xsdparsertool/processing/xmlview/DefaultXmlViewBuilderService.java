package hu.gov.nav.xsdparsertool.processing.xmlview;

import hu.gov.nav.xsdparsertool.core.model.xmlview.XmlDocumentView;
import hu.gov.nav.xsdparsertool.core.model.xmlview.XmlNodeView;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * A {@link XmlViewBuilderService} DOM-alapú implementációja.
 *
 * <p>Biztonságos parserbeállításokkal építi fel a fa nézetet, megtartja az attribútumokat,
 * a levélelemek szövegét pedig külön értékként tárolja.</p>
 */
public class DefaultXmlViewBuilderService implements XmlViewBuilderService {
    /**
     * Felépíti az XML dokumentum megjelenítési modelljét.
     *
     * <p>A metódus a teljes nyers XML-t is megőrzi, emellett biztonságosan konfigurált,
     * namespace-aware DOM parserrel hierarchikus {@link XmlNodeView} fát készít.
     * A fa útvonalai az ismétlődő testvérelemeket egytől induló indexekkel különböztetik meg.</p>
     *
     * @param xmlFile a megjelenítendő XML állomány
     * @return a nyers forrást és a navigálható XML-fát tartalmazó nézetmodell
     * @throws IllegalStateException ha az XML nem olvasható vagy nem jól formált
     */
    @Override
    public XmlDocumentView build(Path xmlFile) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            Document document = factory.newDocumentBuilder().parse(xmlFile.toFile());
            XmlDocumentView view = new XmlDocumentView();
            view.setRawXml(Files.readString(xmlFile));
            view.setRoot(toNodeView(document.getDocumentElement(), "/" + resolveName(document.getDocumentElement())));
            return view;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build XML view for file: " + xmlFile, e);
        }
    }

    /**
     * Rekurzívan átalakít egy DOM-csomópontot az XML-fa megjelenítési modelljévé.
     *
     * <p>Átmásolja az attribútumokat, majd az elemtípusú gyermekeket név szerint
     * számlálja. Az azonos nevű testvérek egytől induló indexet kapnak, így például
     * {@code /Doc/Form[1]/Block[2]} alakú, stabil navigációs útvonal jön létre.</p>
     *
     * <p>Szöveges értéket csak olyan elemhez rendel, amelynek nincs további
     * elemtípusú gyermeke; összetett elemeknél a tartalom a gyermekcsomópontokban jelenik meg.</p>
     *
     * @param node az átalakítandó DOM-csomópont
     * @param path a csomóponthoz már meghatározott kanonikus XML-útvonal
     * @return a csomópont és teljes részfájának megjelenítési modellje
     */
    private XmlNodeView toNodeView(Node node, String path) {
        XmlNodeView view = new XmlNodeView();
        view.setElement(node.getNodeType() == Node.ELEMENT_NODE);
        view.setName(resolveName(node));
        view.setPath(path);
        NamedNodeMap attributes = node.getAttributes();
        if (attributes != null) {
            for (int i = 0; i < attributes.getLength(); i++) {
                Node attr = attributes.item(i);
                view.getAttributes().put(attr.getNodeName(), attr.getNodeValue());
            }
        }
        boolean hasElementChild = false;
        Map<String, Integer> nameCounters = new LinkedHashMap<>();
        for (int i = 0; i < node.getChildNodes().getLength(); i++) {
            Node child = node.getChildNodes().item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                hasElementChild = true;
                String childName = resolveName(child);
                int index = nameCounters.merge(childName, 1, Integer::sum);
                String childPath = path + "/" + childName + "[" + index + "]";
                view.getChildren().add(toNodeView(child, childPath));
            }
        }
        if (!hasElementChild) {
            String text = node.getTextContent();
            if (text != null && !text.isBlank()) view.setTextValue(text.trim());
        }
        return view;
    }

    /**
     * Meghatározza az XML-fa csomópontjának namespace-független megjelenítési nevét.
     *
     * @param node a vizsgált DOM-csomópont
     * @return a lokális név, vagy annak hiányában a DOM {@code nodeName} értéke
     */
    private String resolveName(Node node) {
        return node.getLocalName() != null ? node.getLocalName() : node.getNodeName();
    }
}
