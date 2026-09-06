package hu.gov.nav.xsdparsertool.core.xml;

import org.junit.jupiter.api.Test;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecureXmlParserSupportSecurityTest {

    @Test
    void secureDocumentBuilderRejectsDoctypeBeforeExternalEntityResolution() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        SecureXmlParserSupport.configureSecureDocumentBuilderFactory(factory);
        String xml = "<!DOCTYPE Root [<!ENTITY xxe SYSTEM \"file:///definitely-not-readable\">]><Root>&xxe;</Root>";

        Exception error = assertThrows(Exception.class, () -> factory.newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));

        assertTrue(error.getMessage() == null || !error.getMessage().contains("definitely-not-readable"));
    }

    @Test
    void secureStaxFactoryDoesNotResolveExternalEntity() throws Exception {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        SecureXmlParserSupport.configureSecureXmlInputFactory(factory);
        String xml = "<!DOCTYPE Root [<!ENTITY xxe SYSTEM \"file:///definitely-not-readable\">]><Root>&xxe;</Root>";

        try (ByteArrayInputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            XMLStreamReader reader = factory.createXMLStreamReader(input);
            try {
                while (reader.hasNext()) {
                    reader.next();
                }
            } catch (XMLStreamException error) {
                assertTrue(error.getMessage() == null || !error.getMessage().contains("definitely-not-readable"));
            } finally {
                reader.close();
            }
        }
    }
}
