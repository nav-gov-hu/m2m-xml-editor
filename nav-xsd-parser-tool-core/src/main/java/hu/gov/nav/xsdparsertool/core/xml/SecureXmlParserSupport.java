package hu.gov.nav.xsdparsertool.core.xml;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;

/**
 * Biztonságos XML-, StAX- és transzformer factory-k központi konfiguráló segédosztálya.
 *
 * <p>A beállítások tiltják a külső DTD-k, külső entitások, XInclude és külső stylesheet
 * erőforrások betöltését, ezzel csökkentve az XXE és kapcsolódó külső erőforrás-támadások kockázatát.</p>
 */
public final class SecureXmlParserSupport {

    /**
     * Privát konstruktor; a SecureXmlParserSupport segédosztály példányosítását megakadályozza.
     */
    private SecureXmlParserSupport() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Biztonságosan konfigurál egy DOM parser factory-t.
     *
     * <p>Bekapcsolja a secure processing módot, tiltja a DOCTYPE deklarációt, a külső általános és paraméterentitásokat, a külső DTD-k betöltését, az XInclude-ot és az entitásreferenciák kibontását; az ACCESS_EXTERNAL_DTD és ACCESS_EXTERNAL_SCHEMA értékét üresre állítja.</p>
     * @param factory a konfigurálandó DocumentBuilderFactory
     * @throws ParserConfigurationException ha valamelyik szükséges parser feature nem állítható be
     */
    public static void configureSecureDocumentBuilderFactory(DocumentBuilderFactory factory)
            throws ParserConfigurationException {

        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    }
    /**
     * Biztonságosan konfigurál egy StAX input factory-t.
     *
     * <p>Tiltja a DTD- és külső entitáskezelést, továbbá olyan XMLResolver-t telepít, amely minden külső erőforrás-feloldási kísérletet XMLStreamException hibával megszakít.</p>
     * @param factory a konfigurálandó XMLInputFactory
     */
    public static void configureSecureXmlInputFactory(XMLInputFactory factory) {
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setXMLResolver((publicId, systemId, baseUri, namespace) -> {
            throw new XMLStreamException("Külső XML entitások feloldása tiltott.");
        });
    }

    /**
     * Biztonságosan konfigurál egy transzformer factory-t.
     *
     * <p>Bekapcsolja a secure processing módot, és letiltja a külső DTD-k és stylesheet erőforrások elérését.</p>
     * @param factory a konfigurálandó TransformerFactory
     * @throws TransformerConfigurationException ha a secure-processing feature nem állítható be
     */
    public static void configureSecureTransformerFactory(TransformerFactory factory)
            throws TransformerConfigurationException {
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
    }

}
