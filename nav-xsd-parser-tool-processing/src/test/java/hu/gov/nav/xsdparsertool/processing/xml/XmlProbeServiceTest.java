package hu.gov.nav.xsdparsertool.processing.xml;

import hu.gov.nav.xsdparsertool.schemaregistry.model.XmlProbeResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class XmlProbeServiceTest {
    @TempDir Path tempDir;

    @Test
    void readsRootNamespaceAndSchemaLocation() throws Exception {
        Path xml = tempDir.resolve("input.xml");
        Files.writeString(xml, """
                <Doc xmlns="urn:test" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="urn:test test.xsd"><Value>1</Value></Doc>
                """);

        XmlProbeResult result = new XmlProbeService().probe(xml);

        assertEquals("Doc", result.getRootElementName());
        assertEquals("urn:test", result.getNamespace());
        assertEquals("urn:test test.xsd", result.getSchemaLocation());
    }


    @Test
    void readsNoNamespaceSchemaLocation() throws Exception {
        Path xml = tempDir.resolve("no-ns.xml");
        Files.writeString(xml, "<Doc xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"doc.xsd\"/>");

        XmlProbeResult result = new XmlProbeService().probe(xml);

        assertEquals("Doc", result.getRootElementName());
        assertNull(result.getNamespace());
        assertEquals("doc.xsd", result.getNoNamespaceSchemaLocation());
        assertNull(result.getSchemaLocation());
    }

    @Test
    void xxEIsRejectedBySecureParserConfiguration() throws Exception {
        Path secret = Files.writeString(tempDir.resolve("secret.txt"), "TOP-SECRET");
        Path xml = tempDir.resolve("xxe.xml");
        Files.writeString(xml, "<!DOCTYPE Doc [<!ENTITY xxe SYSTEM \"" + secret.toUri() + "\">]><Doc>&xxe;</Doc>");

        assertThrows(IllegalStateException.class, () -> new XmlProbeService().probe(xml));
    }

    @Test
    void rejectsMalformedXml() throws Exception {
        Path xml = tempDir.resolve("broken.xml");
        Files.writeString(xml, "<Doc><Value></Doc>");
        assertThrows(IllegalStateException.class, () -> new XmlProbeService().probe(xml));
    }
}
