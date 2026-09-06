package hu.gov.nav.xsdparsertool.web.xmlfile.service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.springframework.stereotype.Service;

import hu.gov.nav.xsdparsertool.web.xmlfile.dto.XmlHeaderInfo;

/**
 * A kapcsolódó webes üzleti vagy alkalmazási folyamatokat összefogó szolgáltatás.
 *
 * <p>A {@code XmlHeaderDetectionService} osztály a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Service
public class XmlHeaderDetectionService {
    private static final Pattern NAV_NAMESPACE_PATTERN = Pattern.compile(".*/([^/]+)/([^/]+)$");

    /**
     * A {@code detect} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param xmlPath a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @return a művelet feldolgozási eredménye
     */
    public XmlHeaderInfo detect(Path xmlPath) {
        try (InputStream inputStream = Files.newInputStream(xmlPath)) {
            XMLInputFactory factory = XMLInputFactory.newFactory();
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setXMLResolver((publicId, systemId, baseUri, namespace) -> {
                throw new javax.xml.stream.XMLStreamException("Külső XML entitások feloldása tiltott.");
            });
            XMLStreamReader reader = factory.createXMLStreamReader(inputStream);
            try {
                while (reader.hasNext()) {
                    int event = reader.next();
                    if (event == XMLStreamConstants.DTD) {
                        return new XmlHeaderInfo(null, null, null, null, null, null,
                                "DOCTYPE deklaráció nem engedélyezett.");
                    }
                    if (event == XMLStreamConstants.START_ELEMENT) {
                        String root = reader.getLocalName();
                        String namespace = reader.getNamespaceURI();
                        String schemaLocation = reader.getAttributeValue(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "schemaLocation");
                        String noNamespaceSchemaLocation = reader.getAttributeValue(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "noNamespaceSchemaLocation");
                        String formType = detectFormType(root, namespace, schemaLocation);
                        String formVersion = detectFormVersion(namespace, schemaLocation);
                        return new XmlHeaderInfo(root, namespace, schemaLocation, noNamespaceSchemaLocation, formType, formVersion, null);
                    }
                }
            } finally {
                reader.close();
            }
        } catch (Exception ex) {
            return new XmlHeaderInfo(null, null, null, null, null, null,
                    "XML fejléc felismerése sikertelen: " + ex.getMessage());
        }
        return new XmlHeaderInfo(null, null, null, null, null, null, "Az XML nem tartalmaz root elemet.");
    }



    /**
     * A {@code detectFormType} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param root a művelet bemeneti {@code root} értéke
     * @param namespace a feloldáshoz vagy azonosításhoz használt név
     * @param schemaLocation a művelet bemeneti {@code schemaLocation} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String detectFormType(String root, String namespace, String schemaLocation) {
        if (namespace != null && !namespace.isBlank()) {
            Matcher matcher = NAV_NAMESPACE_PATTERN.matcher(namespace);
            if (matcher.matches()) {
                return matcher.group(1);
            }
        }
        if (schemaLocation != null && !schemaLocation.isBlank()) {
            String detected = detectFormTypeFromSchemaLocation(schemaLocation);
            if (detected != null) {
                return detected;
            }
        }
        if (root != null && root.startsWith("Doc_")) {
            return root.substring("Doc_".length());
        }
        if (root != null && root.startsWith("Form_")) {
            return root.substring("Form_".length());
        }
        return null;
    }

    /**
     * A {@code detectFormVersion} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param namespace a feloldáshoz vagy azonosításhoz használt név
     * @param schemaLocation a művelet bemeneti {@code schemaLocation} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String detectFormVersion(String namespace, String schemaLocation) {
        if (namespace != null && !namespace.isBlank()) {
            Matcher matcher = NAV_NAMESPACE_PATTERN.matcher(namespace);
            if (matcher.matches()) {
                return matcher.group(2);
            }
        }
        if (schemaLocation != null && !schemaLocation.isBlank()) {
            String[] parts = schemaLocation.trim().split("\\s+");
            for (String part : parts) {
                Matcher matcher = NAV_NAMESPACE_PATTERN.matcher(part);
                if (matcher.matches()) {
                    return matcher.group(2);
                }
            }
        }
        return null;
    }

    /**
     * A {@code detectFormTypeFromSchemaLocation} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param schemaLocation a művelet bemeneti {@code schemaLocation} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String detectFormTypeFromSchemaLocation(String schemaLocation) {
        String[] parts = schemaLocation.trim().split("\\s+");
        for (String part : parts) {
            Matcher matcher = NAV_NAMESPACE_PATTERN.matcher(part);
            if (matcher.matches()) {
                return matcher.group(1);
            }
        }
        return null;
    }
}
