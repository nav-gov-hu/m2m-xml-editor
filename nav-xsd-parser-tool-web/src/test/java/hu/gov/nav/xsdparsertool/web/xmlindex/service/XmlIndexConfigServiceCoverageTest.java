package hu.gov.nav.xsdparsertool.web.xmlindex.service;

import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import hu.gov.nav.xsdparsertool.web.audit.AuditLogService;
import hu.gov.nav.xsdparsertool.web.config.PathConfigurationProperties;
import hu.gov.nav.xsdparsertool.web.security.service.CurrentUserService;
import hu.gov.nav.xsdparsertool.web.xmlindex.config.XmlIndexConfigProperties;
import hu.gov.nav.xsdparsertool.web.xmlindex.dto.XmlIndexDtos.IndexFieldDto;
import hu.gov.nav.xsdparsertool.web.xmlindex.dto.XmlIndexDtos.IndexFormConfigDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class XmlIndexConfigServiceCoverageTest {

    @TempDir Path tempDir;

    @Test
    void listFormsFindsDeclaredDocumentAndVersion() throws Exception {
        Path repo = ExceptionSafeOperations.createDirectories(tempDir.resolve("repo"));
        Files.writeString(repo.resolve("IGNORED_9.9.xsd"), documentXsd("TEST", false));
        XmlIndexConfigService service = service(repo);

        var response = service.listForms();

        assertEquals(1, response.forms().size());
        assertEquals("TEST", response.forms().get(0).formName());
        assertEquals(List.of("9.9"), response.forms().get(0).versions());
        assertFalse(response.forms().get(0).configured());
    }

    @Test
    void listFormsAlsoContainsSavedFormWithoutXsd() throws Exception {
        XmlIndexConfigService service = service(ExceptionSafeOperations.createDirectories(tempDir.resolve("empty")));
        IndexFormConfigDto config = new IndexFormConfigDto();
        config.setFormName("SAVED_ONLY");
        service.save(config);

        var response = service.listForms();

        assertEquals(1, response.forms().size());
        assertEquals("SAVED_ONLY", response.forms().get(0).formName());
        assertTrue(response.forms().get(0).configured());
    }

    @Test
    void structureDiscoversMainAndRepeatingFormPartsWithFullPaths() throws Exception {
        Path repo = ExceptionSafeOperations.createDirectories(tempDir.resolve("repo"));
        Files.writeString(repo.resolve("TEST_1.0.xsd"), documentXsd("TEST", true));

        var response = service(repo).structure("TEST", "1.0");

        assertEquals("1.0", response.sourceVersion());
        assertEquals(2, response.formParts().size());
        assertEquals("MAIN", response.formParts().get(0).getRole());
        assertEquals("REPEATING", response.formParts().get(1).getRole());
        assertEquals("/Doc_TEST/Form_TESTM", response.formParts().get(1).getXmlPath());
        assertTrue(response.fields().stream().anyMatch(f -> "/Doc_TEST/Form_TESTA/Block_100/FieldGroup_101/Field_SHARED".equals(f.getXmlPath())));
        assertTrue(response.fields().stream().anyMatch(f -> "/Doc_TEST/Form_TESTM/Block_200/FieldGroup_201/Field_SHARED".equals(f.getXmlPath())));
    }

    @Test
    void structureKeepsDuplicateFieldIdsSeparatedByPathAndFormPart() throws Exception {
        Path repo = ExceptionSafeOperations.createDirectories(tempDir.resolve("repo"));
        Files.writeString(repo.resolve("TEST_1.0.xsd"), documentXsd("TEST", true));

        var shared = service(repo).structure("TEST", null).fields().stream()
                .filter(f -> "Field_SHARED".equals(f.getName())).toList();

        assertEquals(2, shared.size());
        assertNotEquals(shared.get(0).getXmlPath(), shared.get(1).getXmlPath());
        assertNotEquals(shared.get(0).getFormPartName(), shared.get(1).getFormPartName());
    }

    @Test
    void savedFlagsAreMergedByFullPathNotOnlyFieldName() throws Exception {
        Path repo = ExceptionSafeOperations.createDirectories(tempDir.resolve("repo"));
        Files.writeString(repo.resolve("TEST_1.0.xsd"), documentXsd("TEST", true));
        XmlIndexConfigService service = service(repo);
        IndexFormConfigDto config = new IndexFormConfigDto();
        config.setFormName("TEST");
        IndexFieldDto field = new IndexFieldDto();
        field.setName("Field_SHARED");
        field.setXmlPath("/Doc_TEST/Form_TESTM/Block_200/FieldGroup_201/Field_SHARED");
        field.setFormPartName("Form_TESTM");
        field.setFormPartRole("REPEATING");
        field.setDefaultSearch(true);
        field.setDisplay(true);
        field.setMatchMode("exact");
        config.setFields(List.of(field));
        service.save(config);

        var fields = service.structure("TEST", "1.0").fields();
        IndexFieldDto main = fields.stream().filter(f -> f.getXmlPath().contains("Form_TESTA")).filter(f -> "Field_SHARED".equals(f.getName())).findFirst().orElseThrow();
        IndexFieldDto repeated = fields.stream().filter(f -> f.getXmlPath().contains("Form_TESTM")).filter(f -> "Field_SHARED".equals(f.getName())).findFirst().orElseThrow();

        assertFalse(main.isSearchable());
        assertFalse(main.isDisplay());
        assertTrue(repeated.isSearchable());
        assertTrue(repeated.isDefaultSearch());
        assertTrue(repeated.isDisplay());
        assertEquals("exact", repeated.getMatchMode());
    }

    @Test
    void uniqueFieldMayStillUseLegacyNameFallbackWhenSavedPathIsMissing() throws Exception {
        Path repo = ExceptionSafeOperations.createDirectories(tempDir.resolve("repo"));
        Files.writeString(repo.resolve("TEST_1.0.xsd"), documentXsd("TEST", false));
        XmlIndexConfigService service = service(repo);

        IndexFormConfigDto config = new IndexFormConfigDto();
        config.setFormName("TEST");
        IndexFieldDto legacyField = new IndexFieldDto();
        legacyField.setName("Field_SHARED");
        legacyField.setDisplay(true);
        config.setFields(List.of(legacyField));
        service.save(config);

        IndexFieldDto field = service.structure("TEST", "1.0").fields().stream()
                .filter(f -> "Field_SHARED".equals(f.getName()))
                .findFirst()
                .orElseThrow();

        assertTrue(field.isDisplay());
    }

    @Test
    void saveSanitizesDuplicatePathsAndMatchMode() throws Exception {
        XmlIndexConfigService service = service(ExceptionSafeOperations.createDirectories(tempDir.resolve("repo")));
        IndexFormConfigDto config = new IndexFormConfigDto();
        config.setFormName(" TEST ");
        IndexFieldDto first = field("Field_A", "/Doc_TEST/Form_TESTA/Field_A");
        first.setDefaultSearch(true);
        first.setMatchMode("unsupported");
        IndexFieldDto replacement = field("Field_A", "/Doc_TEST/Form_TESTA/Field_A");
        replacement.setDisplay(true);
        replacement.setMatchMode("unsupported");
        config.setFields(List.of(first, replacement));

        var saved = service.save(config);
        var loaded = service.structure("TEST", null).savedConfig();

        assertEquals("TEST", saved.formName());
        assertEquals(1, saved.fieldCount());
        assertEquals(1, loaded.getFields().size());
        assertTrue(loaded.getFields().get(0).isDisplay());
        assertEquals("contains", loaded.getFields().get(0).getMatchMode());
    }

    @Test
    void saveRejectsMissingFormName() throws Exception {
        XmlIndexConfigService service = service(ExceptionSafeOperations.createDirectories(tempDir.resolve("repo")));
        assertThrows(IllegalArgumentException.class, () -> service.save(new IndexFormConfigDto()));
        assertThrows(IllegalArgumentException.class, () -> service.structure(" ", null));
    }

    @Test
    void maliciousDoctypeConfigFailsClosedToEmptyConfiguration() throws Exception {
        Path repo = ExceptionSafeOperations.createDirectories(tempDir.resolve("repo"));
        Path config = tempDir.resolve("config/xml-index.xml");
        ExceptionSafeOperations.createDirectories(config.getParent());
        Files.writeString(config, "<!DOCTYPE x [<!ENTITY e SYSTEM 'file:///etc/passwd'>]><xmlIndexConfig><form formName='X'><field name='F' label='&e;'/></form></xmlIndexConfig>");
        XmlIndexConfigService service = service(repo);

        assertTrue(service.listForms().forms().isEmpty());
    }

    private XmlIndexConfigService service(Path repo) {
        XmlIndexConfigProperties config = new XmlIndexConfigProperties();
        config.setConfigPath(tempDir.resolve("config/xml-index.xml").toString());
        PathConfigurationProperties paths = new PathConfigurationProperties();
        paths.setSchemaDir(repo.toString());
        AuditLogService audit = mock(AuditLogService.class);
        CurrentUserService currentUser = mock(CurrentUserService.class);
        when(currentUser.getCurrentUsername()).thenReturn("tester");
        return new XmlIndexConfigService(config, paths, audit, currentUser);
    }

    private IndexFieldDto field(String name, String path) {
        IndexFieldDto field = new IndexFieldDto();
        field.setName(name);
        field.setLabel(name);
        field.setXmlPath(path);
        return field;
    }

    private String documentXsd(String form, boolean multiform) {
        String repeated = multiform ? "<xs:element name='Form_" + form + "M' type='FormMType' minOccurs='0' maxOccurs='unbounded'><xs:annotation><xs:documentation>Melléklap</xs:documentation></xs:annotation></xs:element>" : "";
        String repeatedType = multiform ? "<xs:complexType name='FormMType'><xs:sequence><xs:element name='Block_200'><xs:complexType><xs:sequence><xs:element name='FieldGroup_201'><xs:complexType><xs:sequence><xs:element name='Field_SHARED' type='xs:string'/></xs:sequence></xs:complexType></xs:element></xs:sequence></xs:complexType></xs:element></xs:sequence></xs:complexType>" : "";
        return """
                <xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
                  <xs:element name='Doc_%s' type='DocType'/>
                  <xs:complexType name='DocType'><xs:sequence>
                    <xs:element name='Form_%sA' type='FormAType' minOccurs='1' maxOccurs='1'/>
                    %s
                  </xs:sequence></xs:complexType>
                  <xs:complexType name='FormAType'><xs:sequence><xs:element name='Block_100'><xs:complexType><xs:sequence><xs:element name='FieldGroup_101'><xs:complexType><xs:sequence><xs:element name='Field_SHARED' type='xs:string'/></xs:sequence></xs:complexType></xs:element></xs:sequence></xs:complexType></xs:element></xs:sequence></xs:complexType>
                  %s
                </xs:schema>
                """.formatted(form, form, repeated, repeatedType);
    }
}
