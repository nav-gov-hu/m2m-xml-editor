package hu.gov.nav.xsdparsertool.multiform;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Discovers the document root, the one main part and one repeating part from an XSD. */
public final class MultiformSchemaAnalyzer {
    private static final String XSD_NS = XMLConstants.W3C_XML_SCHEMA_NS_URI;

    public MultiformDescriptor analyze(Path xsd) {
        try {
            Document document = XmlSecurity.documentBuilderFactory().newDocumentBuilder().parse(xsd.toFile());
            Element schema = document.getDocumentElement();
            String targetNamespace = required(schema.getAttribute("targetNamespace"), "targetNamespace");

            Element documentElement = findGlobalDocumentElement(schema);
            QName documentType = resolveQName(documentElement, required(documentElement.getAttribute("type"), "document type"));
            Element documentComplexType = findNamedComplexType(schema, documentType.getLocalPart());
            List<Element> children = directSequenceElements(documentComplexType);

            if (children.size() != 2) {
                throw new IllegalArgumentException("A támogatott multiform séma pontosan két közvetlen részbizonylat-elemet vár; talált: " + children.size());
            }

            PartDescriptor main = null;
            PartDescriptor repeating = null;
            for (Element child : children) {
                int min = parseOccurs(child.getAttribute("minOccurs"), 1);
                String max = child.hasAttribute("maxOccurs") ? child.getAttribute("maxOccurs") : "1";
                QName type = resolveQName(child, required(child.getAttribute("type"), "part type"));
                QName elementName = new QName("", required(child.getAttribute("name"), "part name"));
                if ("1".equals(max) && min == 1) {
                    if (main != null) {
                        throw new IllegalArgumentException("Több kötelező 1..1 főlapjelölt található az XSD-ben.");
                    }
                    main = new PartDescriptor(PartKind.MAIN, elementName, type, min, max);
                } else if ("unbounded".equals(max)) {
                    if (repeating != null) {
                        throw new IllegalArgumentException("Több ismétlődő melléklapjelölt található az XSD-ben.");
                    }
                    repeating = new PartDescriptor(PartKind.REPEATING, elementName, type, min, max);
                }
            }

            if (main == null || repeating == null) {
                throw new IllegalArgumentException("Nem azonosítható egyértelműen az 1..1 főlap és az ismétlődő melléklap.");
            }

            QName documentName = new QName(targetNamespace, required(documentElement.getAttribute("name"), "document element"));
            return new MultiformDescriptor(documentName, documentType, targetNamespace, main, repeating);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Az XSD nem elemezhető: " + xsd, e);
        }
    }

    private Element findGlobalDocumentElement(Element schema) {
        List<Element> globals = directChildren(schema, "element");
        if (globals.size() != 1) {
            throw new IllegalArgumentException("A támogatott XSD pontosan egy globális dokumentum-elemet vár; talált: " + globals.size());
        }
        return globals.get(0);
    }

    private Element findNamedComplexType(Element schema, String name) {
        for (Element element : directChildren(schema, "complexType")) {
            if (name.equals(element.getAttribute("name"))) {
                return element;
            }
        }
        throw new IllegalArgumentException("Nem található complexType: " + name);
    }

    private List<Element> directSequenceElements(Element complexType) {
        for (Element child : directChildren(complexType, "sequence")) {
            return directChildren(child, "element");
        }
        throw new IllegalArgumentException("A dokumentumtípusnak nincs közvetlen xs:sequence eleme.");
    }

    private List<Element> directChildren(Element parent, String localName) {
        List<Element> result = new ArrayList<>();
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element element
                    && XSD_NS.equals(element.getNamespaceURI())
                    && localName.equals(element.getLocalName())) {
                result.add(element);
            }
        }
        return result;
    }

    private QName resolveQName(Element context, String lexical) {
        int colon = lexical.indexOf(':');
        String prefix = colon >= 0 ? lexical.substring(0, colon) : "";
        String local = colon >= 0 ? lexical.substring(colon + 1) : lexical;
        String namespace = context.lookupNamespaceURI(prefix.isEmpty() ? null : prefix);
        return new QName(namespace == null ? "" : namespace, local, prefix);
    }

    private int parseOccurs(String value, int defaultValue) {
        return value == null || value.isBlank() ? defaultValue : Integer.parseInt(value);
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Hiányzó XSD adat: " + field);
        }
        return value;
    }
}
