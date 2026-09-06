package hu.gov.nav.xsdparsertool.processing.validation;

import hu.gov.nav.xsdparsertool.core.model.bundle.SchemaBundle;
import hu.gov.nav.xsdparsertool.core.model.processing.ValidationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XsdValidationServiceSecurityTest {

    @TempDir Path tempDir;

    @Test
    void externalHttpSchemaImportMustNotBeResolved() throws Exception {
        Path xsd = tempDir.resolve("main.xsd");
        Files.writeString(xsd, """
                <?xml version="1.0" encoding="UTF-8"?>
                <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                  <xs:import namespace="urn:external" schemaLocation="https://127.0.0.1:9/external.xsd"/>
                  <xs:element name="root" type="xs:string"/>
                </xs:schema>
                """);
        Path xml = tempDir.resolve("input.xml");
        Files.writeString(xml, "<root>ok</root>");
        SchemaBundle bundle = new SchemaBundle();
        bundle.setPrimaryXsd(xsd);

        ValidationResult result = new XsdValidationService().validate(xml, bundle);

        assertFalse(result.isValid());
        assertTrue(result.getIssues().stream().anyMatch(issue -> "VALIDATION_EXCEPTION".equals(issue.getCode())));
        assertTrue(result.getIssues().stream()
                .filter(issue -> "VALIDATION_EXCEPTION".equals(issue.getCode()))
                .allMatch(issue -> "Validation failed due to an internal processing error.".equals(issue.getMessage())));
    }

    @Test
    void validXmlPassesSchemaValidation() throws Exception {
        Path xsd = tempDir.resolve("valid.xsd");
        Files.writeString(xsd, """
                <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                  <xs:element name="root"><xs:complexType><xs:sequence>
                    <xs:element name="count" type="xs:int"/>
                  </xs:sequence></xs:complexType></xs:element>
                </xs:schema>
                """);
        Path xml = tempDir.resolve("valid.xml");
        Files.writeString(xml, "<root><count>12</count></root>");
        SchemaBundle bundle = new SchemaBundle();
        bundle.setPrimaryXsd(xsd);

        ValidationResult result = new XsdValidationService().validate(xml, bundle);

        assertTrue(result.isValid());
        assertTrue(result.getIssues().isEmpty());
    }

    @Test
    void invalidFieldValueProducesValidationIssue() throws Exception {
        Path xsd = tempDir.resolve("invalid-value.xsd");
        Files.writeString(xsd, """
                <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                  <xs:element name="root"><xs:complexType><xs:sequence>
                    <xs:element name="count" type="xs:int"/>
                  </xs:sequence></xs:complexType></xs:element>
                </xs:schema>
                """);
        Path xml = tempDir.resolve("invalid-value.xml");
        Files.writeString(xml, "<root><count>not-a-number</count></root>");
        SchemaBundle bundle = new SchemaBundle();
        bundle.setPrimaryXsd(xsd);

        ValidationResult result = new XsdValidationService().validate(xml, bundle);

        assertFalse(result.isValid());
        assertFalse(result.getIssues().isEmpty());
    }

    @Test
    void missingXmlIsReportedBeforeSchemaLookup() {
        SchemaBundle bundle = new SchemaBundle();
        bundle.setPrimaryXsd(tempDir.resolve("unused.xsd"));

        ValidationResult result = new XsdValidationService().validate(tempDir.resolve("missing.xml"), bundle);

        assertFalse(result.isValid());
        assertEquals("XML_FILE_NOT_FOUND", result.getIssues().get(0).getCode());
    }

    @Test
    void missingPrimarySchemaMetadataIsReported() throws Exception {
        Path xml = Files.writeString(tempDir.resolve("input.xml"), "<root/>");

        ValidationResult nullBundle = new XsdValidationService().validate(xml, null);
        ValidationResult emptyBundle = new XsdValidationService().validate(xml, new SchemaBundle());

        assertEquals("PRIMARY_XSD_NOT_FOUND", nullBundle.getIssues().get(0).getCode());
        assertEquals("PRIMARY_XSD_NOT_FOUND", emptyBundle.getIssues().get(0).getCode());
    }

    @Test
    void missingPrimarySchemaFileIsReported() throws Exception {
        Path xml = Files.writeString(tempDir.resolve("input-missing-xsd.xml"), "<root/>");
        SchemaBundle bundle = new SchemaBundle();
        bundle.setPrimaryXsd(tempDir.resolve("missing.xsd"));

        ValidationResult result = new XsdValidationService().validate(xml, bundle);

        assertFalse(result.isValid());
        assertEquals("PRIMARY_XSD_MISSING", result.getIssues().get(0).getCode());
    }

    @Test
    void relativeIncludeCanBeResolvedFromGeneralXsdDirectory() throws Exception {
        Path primaryDir = Files.createDirectory(tempDir.resolve("primary"));
        Path generalDir = Files.createDirectory(tempDir.resolve("general"));
        Files.writeString(generalDir.resolve("types.xsd"), """
                <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                  <xs:simpleType name="Code"><xs:restriction base="xs:string"><xs:enumeration value="A"/></xs:restriction></xs:simpleType>
                </xs:schema>
                """);
        Path xsd = primaryDir.resolve("main.xsd");
        Files.writeString(xsd, """
                <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                  <xs:include schemaLocation="types.xsd"/>
                  <xs:element name="root" type="Code"/>
                </xs:schema>
                """);
        Path xml = Files.writeString(tempDir.resolve("included.xml"), "<root>A</root>");
        SchemaBundle bundle = new SchemaBundle();
        bundle.setPrimaryXsd(xsd);

        ValidationResult result = new XsdValidationService().validate(xml, bundle, generalDir);

        assertTrue(result.isValid(), () -> "Unexpected issues: " + result.getIssues());
    }

    @Test
    void malformedXmlProducesFatalOrValidationExceptionIssue() throws Exception {
        Path xsd = tempDir.resolve("malformed.xsd");
        Files.writeString(xsd, """
                <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                  <xs:element name="root" type="xs:string"/>
                </xs:schema>
                """);
        Path xml = Files.writeString(tempDir.resolve("malformed.xml"), "<root>");
        SchemaBundle bundle = new SchemaBundle();
        bundle.setPrimaryXsd(xsd);

        ValidationResult result = new XsdValidationService().validate(xml, bundle);

        assertFalse(result.isValid());
        assertTrue(result.getIssues().stream().anyMatch(issue ->
                "XSD_FATAL_ERROR".equals(issue.getCode()) || "VALIDATION_EXCEPTION".equals(issue.getCode())));
    }

    @Test
    void invalidSecondMultiformOccurrenceGetsIndexedXmlPath() throws Exception {
        Path xsd = tempDir.resolve("multiform-path.xsd");
        Files.writeString(xsd, """
                <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                  <xs:element name="Doc_TEST"><xs:complexType><xs:sequence>
                    <xs:element name="Form_TESTM" minOccurs="0" maxOccurs="unbounded">
                      <xs:complexType><xs:sequence>
                        <xs:element name="Field_CODE">
                          <xs:simpleType><xs:restriction base="xs:string"><xs:maxLength value="3"/></xs:restriction></xs:simpleType>
                        </xs:element>
                      </xs:sequence></xs:complexType>
                    </xs:element>
                  </xs:sequence></xs:complexType></xs:element>
                </xs:schema>
                """);
        Path xml = tempDir.resolve("multiform-path.xml");
        Files.writeString(xml, """
                <Doc_TEST>
                  <Form_TESTM><Field_CODE>OK</Field_CODE></Form_TESTM>
                  <Form_TESTM><Field_CODE>TOO-LONG</Field_CODE></Form_TESTM>
                </Doc_TEST>
                """);
        SchemaBundle bundle = new SchemaBundle();
        bundle.setPrimaryXsd(xsd);

        ValidationResult result = new XsdValidationService().validate(xml, bundle);

        assertFalse(result.isValid());
        assertTrue(result.getIssues().stream().anyMatch(issue ->
                issue.getPath() != null
                        && issue.getPath().contains("/Form_TESTM[2]/")
                        && issue.getPath().contains("/Field_CODE[1]")),
                () -> "Expected indexed multiform path, got: " + result.getIssues().stream().map(issue -> issue.getPath()).toList());
    }


}
