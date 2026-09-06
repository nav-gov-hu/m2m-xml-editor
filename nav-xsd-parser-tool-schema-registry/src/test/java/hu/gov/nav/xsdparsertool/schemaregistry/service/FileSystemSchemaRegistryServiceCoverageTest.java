package hu.gov.nav.xsdparsertool.schemaregistry.service;

import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import hu.gov.nav.xsdparsertool.core.model.bundle.SchemaBundle;
import hu.gov.nav.xsdparsertool.schemaregistry.model.XmlProbeResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileSystemSchemaRegistryServiceCoverageTest {

    @TempDir Path tempDir;

    @Test
    void resolvesByExactRootElement() throws Exception {
        Path xsd = writeXsd(tempDir.resolve("TEST_1.0.xsd"), "urn:test:1.0", "Doc_TEST", null);
        SchemaBundle bundle = new FileSystemSchemaRegistryService().resolveByDocumentType("Doc_TEST", tempDir);
        assertEquals(xsd, bundle.getPrimaryXsd());
        assertEquals("Doc_TEST", bundle.getRootElementName());
        assertTrue(bundle.getMatchReason().contains("root-element"));
    }

    @Test
    void probePrefersRootAndNamespaceMatch() throws Exception {
        Path first = writeXsd(tempDir.resolve("A_1.0.xsd"), "urn:a", "Doc_SHARED", null);
        Path second = writeXsd(tempDir.resolve("B_1.0.xsd"), "urn:b", "Doc_SHARED", null);
        XmlProbeResult probe = new XmlProbeResult();
        probe.setRootElementName("Doc_SHARED");
        probe.setNamespace("urn:b");

        SchemaBundle bundle = new FileSystemSchemaRegistryService().resolveByXmlProbe(probe, tempDir);

        assertEquals(second, bundle.getPrimaryXsd());
        assertNotEquals(first, bundle.getPrimaryXsd());
        assertTrue(bundle.getMatchReason().contains("namespace"));
    }

    @Test
    void schemaLocationHintWinsWhenRootIsAmbiguous() throws Exception {
        Path wanted = writeXsd(tempDir.resolve("wanted.xsd"), "urn:a", "Doc_A", null);
        writeXsd(tempDir.resolve("other.xsd"), "urn:a", "Doc_A", null);
        XmlProbeResult probe = new XmlProbeResult();
        probe.setRootElementName("Doc_A");
        probe.setNamespace("urn:a");
        probe.setSchemaLocation("urn:a wanted.xsd");

        assertEquals(wanted, new FileSystemSchemaRegistryService().resolveByXmlProbe(probe, tempDir).getPrimaryXsd());
    }

    @Test
    void higherReleasePatchWinsOnEqualContentScore() throws Exception {
        Path v120 = ExceptionSafeOperations.createDirectories(tempDir.resolve("TEST/1.2.0"));
        Path v121 = ExceptionSafeOperations.createDirectories(tempDir.resolve("TEST/1.2.1"));
        writeXsd(v120.resolve("TEST.xsd"), "urn:test:1.2", "Doc_TEST", null);
        Path newer = writeXsd(v121.resolve("TEST.xsd"), "urn:test:1.2", "Doc_TEST", null);

        SchemaBundle bundle = new FileSystemSchemaRegistryService().resolveByDocumentType("Doc_TEST", tempDir);

        assertEquals(newer, bundle.getPrimaryXsd());
    }

    @Test
    void relatedIncludesAreAddedToBundle() throws Exception {
        Path common = writeXsd(tempDir.resolve("common.xsd"), "urn:test", "CommonRoot", null);
        Path primary = writeXsd(tempDir.resolve("TEST.xsd"), "urn:test", "Doc_TEST", "common.xsd");

        SchemaBundle bundle = new FileSystemSchemaRegistryService().resolveByDocumentType("Doc_TEST", tempDir);

        assertEquals(primary, bundle.getPrimaryXsd());
        assertTrue(bundle.getXsdFiles().contains(primary));
        assertTrue(bundle.getXsdFiles().contains(common));
    }

    @Test
    void missingDocumentTypeFailsClearly() throws Exception {
        writeXsd(tempDir.resolve("TEST.xsd"), "urn:test", "Doc_TEST", null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new FileSystemSchemaRegistryService().resolveByDocumentType("NOT_PRESENT", tempDir));
        assertTrue(ex.getMessage().contains("No matching XSD"));
    }


    @Test
    void refreshesCachedDescriptorsWhenNewSchemaAppearsAfterInitialScan() throws Exception {
        writeXsd(tempDir.resolve("FIRST.xsd"), "urn:first", "Doc_FIRST", null);
        FileSystemSchemaRegistryService service = new FileSystemSchemaRegistryService();
        assertEquals("Doc_FIRST", service.resolveByDocumentType("Doc_FIRST", tempDir).getRootElementName());

        Path added = writeXsd(tempDir.resolve("KSZERZ/3.0/NAV_KSZERZ.xsd"),
                "https://soap.api.nav.gov.hu/definitions/model/2.0/KSZERZ/3.0", "Doc_KSZERZ", null);

        SchemaBundle bundle = service.resolveByDocumentType("Doc_KSZERZ", tempDir);

        assertEquals(added, bundle.getPrimaryXsd());
        assertEquals("Doc_KSZERZ", bundle.getRootElementName());
    }

    @Test
    void malformedXsdIsRejectedInsteadOfSilentlyAccepted() throws Exception {
        Files.writeString(tempDir.resolve("broken.xsd"), "<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'><xs:element");
        assertThrows(IllegalStateException.class,
                () -> new FileSystemSchemaRegistryService().resolveByDocumentType("TEST", tempDir));
    }

    private Path writeXsd(Path path, String namespace, String root, String include) throws Exception {
        ExceptionSafeOperations.createDirectories(path.getParent());
        String includeXml = include == null ? "" : "<xs:include schemaLocation='" + include + "'/>";
        Files.writeString(path, "<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema' targetNamespace='" + namespace + "'>" + includeXml + "<xs:element name='" + root + "' type='xs:string'/></xs:schema>");
        return path;
    }
}
