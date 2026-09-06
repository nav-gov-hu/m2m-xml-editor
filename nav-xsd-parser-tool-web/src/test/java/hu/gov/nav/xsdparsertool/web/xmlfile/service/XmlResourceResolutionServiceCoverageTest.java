package hu.gov.nav.xsdparsertool.web.xmlfile.service;

import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import hu.gov.nav.xsdparsertool.web.config.PathConfigurationProperties;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.XmlHeaderInfo;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.XmlResourceResolutionInfo;
import hu.gov.nav.xsdparsertool.web.xpath.config.XPathValidatorProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class XmlResourceResolutionServiceCoverageTest {

    @TempDir
    Path tempDir;

    @Test
    void skipsResolutionWhenHeaderWasNotDetected() {
        XmlResourceResolutionService service = new XmlResourceResolutionService(new PathConfigurationProperties(), new XPathValidatorProperties());

        XmlResourceResolutionInfo result = service.resolve(new XmlHeaderInfo(
                null, null, null, null, null, null, "broken"));

        assertEquals("HEADER_NOT_DETECTED", result.status());
        assertNull(result.xsdPath());
        assertNull(result.uiModelPath());
        assertNull(result.xpathRulesPath());
    }

    @Test
    void missingSchemaRootStillBuildsXpathCandidate() throws Exception {
        Path xpathRoot = ExceptionSafeOperations.createDirectories(tempDir.resolve("xpath"));
        Path release = ExceptionSafeOperations.createDirectories(xpathRoot.resolve("TEST/1.2.1"));
        Path rule = Files.writeString(release.resolve("TEST_xpath.xml"), "<Rules/>");

        PathConfigurationProperties properties = new PathConfigurationProperties();
        properties.setSchemaDir(tempDir.resolve("missing-schema").toString());
        XPathValidatorProperties xpathProperties = new XPathValidatorProperties();
        xpathProperties.setRuleRootDir(xpathRoot.toString());
        XmlResourceResolutionService service = new XmlResourceResolutionService(properties, xpathProperties);

        XmlResourceResolutionInfo result = service.resolve(header("Doc_TEST", "urn:test", null, "TEST", "1.2"));

        assertEquals("CONFIG_MISSING", result.status());
        assertEquals("TEST", result.documentType());
        assertEquals("1.2", result.documentVersion());
        assertEquals(rule.toAbsolutePath().normalize().toString(), result.xpathRulesPath());
    }

    @Test
    void resolvesPrimaryXsdAndXpathRuleFromConfiguredDirectories() throws Exception {
        Path schemaRoot = ExceptionSafeOperations.createDirectories(tempDir.resolve("schema"));
        Path xpathRoot = ExceptionSafeOperations.createDirectories(tempDir.resolve("xpath"));
        Path xsd = writeXsd(schemaRoot.resolve("TEST_1.2.xsd"), "urn:test", "Doc_TEST");
        Path release = ExceptionSafeOperations.createDirectories(xpathRoot.resolve("TEST/1.2.3"));
        Path rule = Files.writeString(release.resolve("TEST_1.2.3_xpath.xml"), "<Rules/>");

        PathConfigurationProperties properties = new PathConfigurationProperties();
        properties.setSchemaDir(schemaRoot.toString());
        XPathValidatorProperties xpathProperties = new XPathValidatorProperties();
        xpathProperties.setRuleRootDir(xpathRoot.toString());
        XmlResourceResolutionService service = new XmlResourceResolutionService(properties, xpathProperties);

        XmlResourceResolutionInfo result = service.resolve(header(
                "Doc_TEST", "urn:test", "urn:test TEST_1.2.xsd", "TEST", "1.2"));

        assertEquals("RESOLVED", result.status());
        assertEquals("TEST", result.documentType());
        assertEquals("1.2", result.documentVersion());
        assertEquals(xsd.toAbsolutePath().normalize().toString(), result.xsdPath());
        assertEquals(rule.toAbsolutePath().normalize().toString(), result.xpathRulesPath());
        assertNull(result.uiModelPath());
        assertNotNull(result.message());
    }

    @Test
    void marksDifferentResolvedXsdVersionAsReadOnlyFallback() throws Exception {
        Path schemaRoot = ExceptionSafeOperations.createDirectories(tempDir.resolve("schema"));
        Path versionDir = ExceptionSafeOperations.createDirectories(schemaRoot.resolve("0.23"));
        writeXsd(versionDir.resolve("TEST_0.23.xsd"), "urn:test", "Doc_TEST");

        PathConfigurationProperties properties = new PathConfigurationProperties();
        properties.setSchemaDir(schemaRoot.toString());
        XmlResourceResolutionService service = new XmlResourceResolutionService(properties, new XPathValidatorProperties());

        XmlResourceResolutionInfo result = service.resolve(header(
                "Doc_TEST", "urn:test", null, "TEST", "0.27"));

        assertEquals("RESOLVED", result.status());
        assertEquals("0.27", result.documentVersion());
        assertEquals("0.23", result.resolvedSchemaVersion());
        assertTrue(result.schemaVersionFallback());
        assertTrue(result.message().contains("csak olvasható"));
    }

    @Test
    void returnsNotResolvedWhenNoSchemaMatchesDetectedHeader() throws Exception {
        Path schemaRoot = ExceptionSafeOperations.createDirectories(tempDir.resolve("schema"));
        writeXsd(schemaRoot.resolve("OTHER.xsd"), "urn:other", "Doc_OTHER");

        PathConfigurationProperties properties = new PathConfigurationProperties();
        properties.setSchemaDir(schemaRoot.toString());
        XmlResourceResolutionService service = new XmlResourceResolutionService(properties, new XPathValidatorProperties());

        XmlResourceResolutionInfo result = service.resolve(header(
                "Doc_MISSING", "urn:missing", null, "MISSING", "9.9"));

        assertEquals("NOT_RESOLVED", result.status());
        assertEquals("MISSING", result.documentType());
        assertEquals("9.9", result.documentVersion());
        assertNull(result.xsdPath());
        assertTrue(result.message().startsWith("Erőforrás-feloldás sikertelen:"));
    }

    @Test
    void xpathPathIsOmittedWhenFormVersionIsMissing() throws Exception {
        Path schemaRoot = ExceptionSafeOperations.createDirectories(tempDir.resolve("schema"));
        writeXsd(schemaRoot.resolve("TEST.xsd"), "urn:test", "Doc_TEST");

        PathConfigurationProperties properties = new PathConfigurationProperties();
        properties.setSchemaDir(schemaRoot.toString());
        XPathValidatorProperties xpathProperties = new XPathValidatorProperties();
        xpathProperties.setRuleRootDir(tempDir.resolve("xpath").toString());
        XmlResourceResolutionService service = new XmlResourceResolutionService(properties, xpathProperties);

        XmlResourceResolutionInfo result = service.resolve(header(
                "Doc_TEST", "urn:test", null, "TEST", null));

        assertEquals("RESOLVED", result.status());
        assertNull(result.xpathRulesPath());
    }

    private XmlHeaderInfo header(String root, String namespace, String schemaLocation, String formType, String formVersion) {
        return new XmlHeaderInfo(root, namespace, schemaLocation, null, formType, formVersion, null);
    }

    private Path writeXsd(Path path, String namespace, String root) throws Exception {
        ExceptionSafeOperations.createDirectories(path.getParent());
        Files.writeString(path, "<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema' targetNamespace='"
                + namespace + "'><xs:element name='" + root + "' type='xs:string'/></xs:schema>");
        return path;
    }
}
