package hu.gov.nav.xsdparsertool.web.xmlfile.service;

import hu.gov.nav.xsdparsertool.web.xmlfile.dto.XmlHeaderInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class XmlHeaderDetectionServiceCoverageTest {

    @TempDir
    Path tempDir;

    private final XmlHeaderDetectionService service = new XmlHeaderDetectionService();

    @Test
    void detectsFormTypeAndVersionFromNavNamespace() throws Exception {
        Path xml = write("namespace.xml", """
                <Doc_TEST xmlns="https://example.nav.gov.hu/schema/TEST/2.4"
                          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                          xsi:schemaLocation="https://example.nav.gov.hu/schema/TEST/2.4 TEST.xsd"/>
                """);

        XmlHeaderInfo result = service.detect(xml);

        assertTrue(result.detected());
        assertEquals("Doc_TEST", result.rootElement());
        assertEquals("https://example.nav.gov.hu/schema/TEST/2.4", result.namespaceUri());
        assertEquals("TEST", result.formType());
        assertEquals("2.4", result.formVersion());
        assertNull(result.errorMessage());
    }

    @Test
    void fallsBackToSchemaLocationWhenNamespaceIsMissing() throws Exception {
        Path xml = write("schema-location.xml", """
                <Root xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                      xsi:noNamespaceSchemaLocation="local.xsd"
                      xsi:schemaLocation="urn:ignored ignored.xsd https://schemas.example.hu/FORMX/3.7 FORMX.xsd"/>
                """);

        XmlHeaderInfo result = service.detect(xml);

        assertEquals("FORMX", result.formType());
        assertEquals("3.7", result.formVersion());
        assertEquals("local.xsd", result.noNamespaceSchemaLocation());
    }

    @Test
    void fallsBackToDocumentRootForFormType() throws Exception {
        XmlHeaderInfo doc = service.detect(write("doc.xml", "<Doc_AB12/>"));
        XmlHeaderInfo form = service.detect(write("form.xml", "<Form_CD34/>"));

        assertEquals("AB12", doc.formType());
        assertEquals("CD34", form.formType());
        assertNull(doc.formVersion());
        assertNull(form.formVersion());
    }

    @Test
    void returnsNullFormMetadataForGenericRootWithoutHints() throws Exception {
        XmlHeaderInfo result = service.detect(write("generic.xml", "<Root/>"));

        assertTrue(result.detected());
        assertEquals("Root", result.rootElement());
        assertNull(result.formType());
        assertNull(result.formVersion());
    }

    @Test
    void rejectsDoctypeAndDoesNotResolveExternalEntity() throws Exception {
        Path secret = write("secret.txt", "TOP-SECRET");
        String uri = secret.toUri().toString();
        Path xml = write("xxe.xml", "<!DOCTYPE Root [<!ENTITY xxe SYSTEM \"" + uri + "\">]><Root>&xxe;</Root>");

        XmlHeaderInfo result = service.detect(xml);

        assertFalse(result.detected());
        assertNotNull(result.errorMessage());
        assertFalse(result.errorMessage().contains("TOP-SECRET"));
    }

    @Test
    void malformedXmlReturnsDetectionError() throws Exception {
        XmlHeaderInfo result = service.detect(write("broken.xml", "<"));

        assertFalse(result.detected());
        assertNotNull(result.errorMessage());
        assertTrue(result.errorMessage().startsWith("XML fejléc felismerése sikertelen:"));
    }

    private Path write(String name, String content) throws Exception {
        Path path = tempDir.resolve(name);
        Files.writeString(path, content);
        return path;
    }
}
