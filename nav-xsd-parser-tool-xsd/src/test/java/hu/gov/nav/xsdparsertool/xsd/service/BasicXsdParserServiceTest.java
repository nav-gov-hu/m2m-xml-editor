package hu.gov.nav.xsdparsertool.xsd.service;

import hu.gov.nav.xsdparsertool.core.model.bundle.SchemaBundle;
import hu.gov.nav.xsdparsertool.core.model.definition.DocumentDefinition;
import hu.gov.nav.xsdparsertool.core.model.definition.FieldDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BasicXsdParserServiceTest {
    @TempDir Path tempDir;

    @Test
    void resolvesReferencedSimpleTypeEnumAndAnnotationLabel() throws Exception {
        Path xsd = tempDir.resolve("form.xsd");
        Files.writeString(xsd, """
                <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                  <xs:simpleType name="StatusType">
                    <xs:annotation><xs:documentation>Státusz</xs:documentation></xs:annotation>
                    <xs:restriction base="xs:string">
                      <xs:enumeration value="A"/><xs:enumeration value="B"/>
                    </xs:restriction>
                  </xs:simpleType>
                  <xs:element name="Doc">
                    <xs:complexType><xs:sequence>
                      <xs:element name="FieldGroup_100">
                        <xs:complexType><xs:sequence>
                          <xs:element name="Field_ABC" type="StatusType" minOccurs="0"/>
                        </xs:sequence></xs:complexType>
                      </xs:element>
                    </xs:sequence></xs:complexType>
                  </xs:element>
                </xs:schema>
                """);
        SchemaBundle bundle = bundle(xsd, "Doc");

        DocumentDefinition definition = new BasicXsdParserService().parse(bundle);
        FieldDefinition field = definition.getBlocks().stream()
                .flatMap(block -> block.getFields().stream())
                .filter(item -> "Field_ABC".equals(item.getXmlName()))
                .findFirst().orElseThrow();

        assertEquals(List.of("A", "B"), field.getEnumValues());
        assertEquals("Státusz", field.getLabel());
        assertFalse(field.isRequired());
        assertEquals("/Doc/FieldGroup_100/Field_ABC", field.getXmlPath());
    }


    @Test
    void recordsConcreteChainElementCardinalityFromGeneratedXsd() throws Exception {
        Path xsd = tempDir.resolve("chain.xsd");
        Files.writeString(xsd, """
                <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
                  <xs:element name="Doc"><xs:complexType><xs:sequence>
                    <xs:element name="Form_1"><xs:complexType><xs:sequence>
                      <xs:element name="Block_1"><xs:complexType><xs:sequence>
                        <xs:element name="Chain_1"><xs:complexType><xs:sequence>
                          <xs:element name="Chain_elem" minOccurs="1" maxOccurs="10"><xs:complexType><xs:sequence>
                            <xs:element name="FieldGroup_100"><xs:complexType><xs:sequence>
                              <xs:element name="Field_A" type="xs:string"/>
                            </xs:sequence></xs:complexType></xs:element>
                          </xs:sequence></xs:complexType></xs:element>
                        </xs:sequence><xs:attribute name="sdgID" use="required" fixed="120319"/><xs:attribute name="Type" use="required" fixed="technical"/></xs:complexType></xs:element>
                      </xs:sequence></xs:complexType></xs:element>
                    </xs:sequence></xs:complexType></xs:element>
                  </xs:sequence></xs:complexType></xs:element>
                </xs:schema>
                """);

        DocumentDefinition definition = new BasicXsdParserService().parse(bundle(xsd, "Doc"));
        String repeatPath = "/Doc/Form_1/Block_1/Chain_1/Chain_elem";

        assertEquals(1, definition.getStructuralMinOccursByPath().get(repeatPath));
        assertEquals("10", definition.getStructuralMaxOccursByPath().get(repeatPath));
        assertEquals("120319", definition.getStructuralFixedAttributesByPath()
                .get("/Doc/Form_1/Block_1/Chain_1").get("sdgID"));
        assertFalse(definition.getStructuralFixedAttributesByPath()
                .get("/Doc/Form_1/Block_1/Chain_1").containsKey("Type"));
    }

    @Test
    void nullBundleIsRejectedClearly() {
        assertThrows(IllegalArgumentException.class, () -> new BasicXsdParserService().parse(null));
    }

    private static SchemaBundle bundle(Path xsd, String root) {
        SchemaBundle bundle = new SchemaBundle();
        bundle.setPrimaryXsd(xsd);
        bundle.setXsdFiles(List.of(xsd));
        bundle.setRootElementName(root);
        bundle.setDocumentType("TEST");
        return bundle;
    }
}
