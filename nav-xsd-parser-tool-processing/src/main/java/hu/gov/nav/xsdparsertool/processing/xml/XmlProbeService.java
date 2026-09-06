package hu.gov.nav.xsdparsertool.processing.xml;

import hu.gov.nav.xsdparsertool.schemaregistry.model.XmlProbeResult;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;


/**
 * Az XML gyors szerkezeti azonosítását végző szolgáltatás.
 *
 * <p>Kiolvassa a gyökérelem lokális nevét, namespace-ét, valamint az
 * {@code xsi:schemaLocation} és {@code xsi:noNamespaceSchemaLocation} értékeket.
 * Ezeket a Schema Registry használja a megfelelő séma-csomag kiválasztásához.</p>
 */
public class XmlProbeService {

/**
 * Beolvassa az XML sémafeloldáshoz szükséges alapmetaadatait.
 * @param xmlFile a vizsgálandó XML állomány
 * @return a gyökérelem és sémahivatkozások adatai
 * @throws IllegalStateException ha az XML nem olvasható vagy nem jól formált
 */
    public XmlProbeResult probe(Path xmlFile) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Document document = factory.newDocumentBuilder().parse(xmlFile.toFile());

            XmlProbeResult result = new XmlProbeResult();
            result.setRootElementName(document.getDocumentElement().getLocalName());
            result.setNamespace(document.getDocumentElement().getNamespaceURI());

            String schemaLocation = document.getDocumentElement()
                    .getAttributeNS(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "schemaLocation");
            if (schemaLocation != null && !schemaLocation.isBlank()) {
                result.setSchemaLocation(schemaLocation);
            }

            String noNamespaceSchemaLocation = document.getDocumentElement()
                    .getAttributeNS(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "noNamespaceSchemaLocation");
            if (noNamespaceSchemaLocation != null && !noNamespaceSchemaLocation.isBlank()) {
                result.setNoNamespaceSchemaLocation(noNamespaceSchemaLocation);
            }
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to probe XML file: " + xmlFile, e);
        }
    }
}
