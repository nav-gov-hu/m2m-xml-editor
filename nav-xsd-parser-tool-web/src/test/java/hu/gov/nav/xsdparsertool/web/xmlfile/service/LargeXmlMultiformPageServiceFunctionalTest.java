package hu.gov.nav.xsdparsertool.web.xmlfile.service;

import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import hu.gov.nav.xsdparsertool.web.support.RepositoryAccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import hu.gov.nav.xsdparsertool.web.xmlfile.config.XmlFileStorageProperties;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileRepository;
import hu.gov.nav.xsdparsertool.web.xmlindex.dto.XmlIndexDtos.IndexFieldDto;
import hu.gov.nav.xsdparsertool.web.xmlindex.dto.XmlIndexDtos.IndexFormConfigDto;
import hu.gov.nav.xsdparsertool.web.xmlindex.dto.XmlIndexDtos.StructureResponse;
import hu.gov.nav.xsdparsertool.web.xmlindex.service.XmlIndexConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LargeXmlMultiformPageServiceFunctionalTest {

    @TempDir Path tempDir;
    @Mock XmlFileRepository xmlFiles;
    @Mock XmlIndexConfigService indexConfig;

    private XmlFileEntity file;
    private LargeXmlMultiformPageService service;

    @BeforeEach
    void setUp() throws Exception {
        Path source = tempDir.resolve("large.xml");
        Files.writeString(source, """
                <Doc_26HIPAK>
                  <Form_26HIPAKA><Field_SHARED>MAIN</Field_SHARED><Field_MAIN>main-only</Field_MAIN></Form_26HIPAKA>
                  <Form_26HIPAKM><Field_SHARED>A-001</Field_SHARED><Field_NAME>Alfa Kft</Field_NAME><Field_CITY>Budapest</Field_CITY></Form_26HIPAKM>
                  <Form_26HIPAKM><Field_SHARED>B-002</Field_SHARED><Field_NAME>Béta Kft</Field_NAME><Field_CITY>Szeged</Field_CITY></Form_26HIPAKM>
                  <Form_26HIPAKM><Field_SHARED>C-003</Field_SHARED><Field_NAME>Gamma Kft</Field_NAME><Field_CITY>Pécs</Field_CITY></Form_26HIPAKM>
                </Doc_26HIPAK>
                """);
        file = new XmlFileEntity();
        file.setId(77L);
        file.setFileName("large.xml");
        file.setFilePath(source.toString());
        file.setRootElement("Doc_26HIPAK");
        file.setFormType("BEVALLAS");
        file.setFormVersion("1.0.0");
        XmlFileStorageProperties storage = new XmlFileStorageProperties();
        storage.setXmlIndexDir(tempDir.resolve("index").toString());
        service = new LargeXmlMultiformPageService(xmlFiles, indexConfig, new ObjectMapper(), storage);
    }

    @Test
    void rejectsPathTraversalInFormNameBeforeCreatingIndexFiles() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.page(77L, "../outside/Form_26HIPAKM", 0, 50, ""));

        assertEquals("Érvénytelen űrlaprész név.", error.getMessage());
        verifyNoInteractions(indexConfig);
        assertFalse(ExceptionSafeOperations.fileExists(tempDir.resolve("outside")));
    }

    @Test
    void pagesRepeatingFormWithoutMixingMainFormValues() throws Exception {
        stubFileLookup();
        when(indexConfig.structure("26HIPAK", "1.0.0")).thenReturn(structure(fieldsForAttachment(), null));

        LargeXmlMultiformPageService.PageResult result = service.page(77L, "Form_26HIPAKM", 0, 2, "");

        assertEquals(3, result.total());
        assertEquals(2, result.rows().size());
        assertTrue(result.hasMore());
        assertEquals(1L, result.rows().get(0).index());
        assertEquals("A-001", result.rows().get(0).values().get("Field_SHARED"));
        assertEquals("B-002", result.rows().get(1).values().get("Field_SHARED"));
        assertFalse(result.rows().get(0).xml().contains("MAIN"));
        assertFalse(result.rows().get(0).values().containsValue("main-only"));
    }

    @Test
    void secondPageKeepsOccurrenceIndexAndReturnsOnlyRemainingRow() throws Exception {
        stubFileLookup();
        when(indexConfig.structure("26HIPAK", "1.0.0")).thenReturn(structure(fieldsForAttachment(), null));

        LargeXmlMultiformPageService.PageResult result = service.page(77L, "Form_26HIPAKM", 1, 2, "");

        assertEquals(1, result.rows().size());
        assertEquals(3L, result.rows().get(0).index());
        assertEquals("C-003", result.rows().get(0).values().get("Field_SHARED"));
        assertFalse(result.hasMore());
    }

    @Test
    void searchUsesConfiguredSearchableFieldsAndIsCaseInsensitive() throws Exception {
        stubFileLookup();
        when(indexConfig.structure("26HIPAK", "1.0.0")).thenReturn(structure(fieldsForAttachment(), null));

        LargeXmlMultiformPageService.PageResult result = service.page(77L, "Form_26HIPAKM", 0, 50, "BÉTA");

        assertEquals(1, result.total());
        assertEquals(2L, result.rows().get(0).index());
        assertEquals("Béta Kft", result.rows().get(0).values().get("Field_NAME"));
    }

    @Test
    void nonSearchableDisplayFieldDoesNotAccidentallyBecomeSearchSource() throws Exception {
        stubFileLookup();
        when(indexConfig.structure("26HIPAK", "1.0.0")).thenReturn(structure(fieldsForAttachment(), null));

        LargeXmlMultiformPageService.PageResult result = service.page(77L, "Form_26HIPAKM", 0, 50, "szeged");

        assertEquals(0, result.total());
        assertTrue(result.rows().isEmpty());
    }

    @Test
    void columnsComeOnlyFromDisplayFieldsWhenDisplayConfigurationExists() throws Exception {
        stubFileLookup();
        when(indexConfig.structure("26HIPAK", "1.0.0")).thenReturn(structure(fieldsForAttachment(), null));

        LargeXmlMultiformPageService.PageResult result = service.page(77L, "Form_26HIPAKM", 0, 50, "");

        assertEquals(List.of("Field_SHARED", "Field_CITY"), result.columns().stream().map(LargeXmlMultiformPageService.ColumnResult::name).toList());
        assertEquals(List.of("Azonosító", "Település"), result.columns().stream().map(LargeXmlMultiformPageService.ColumnResult::label).toList());
    }

    @Test
    void savedConfigurationIsUsedWhenParsedStructureOmitsConfiguredField() throws Exception {
        stubFileLookup();
        IndexFieldDto saved = field("Field_NAME", "Név", "/Doc_26HIPAK/Form_26HIPAKM/Field_NAME", true, true, true);
        IndexFormConfigDto savedConfig = new IndexFormConfigDto();
        savedConfig.setFields(List.of(saved));
        when(indexConfig.structure("26HIPAK", "1.0.0")).thenReturn(structure(List.of(), savedConfig));

        LargeXmlMultiformPageService.ConfigurationStatus status = service.configurationStatus(77L, "Form_26HIPAKM");
        LargeXmlMultiformPageService.PageResult result = service.page(77L, "Form_26HIPAKM", 0, 50, "gamma");

        assertFalse(status.configurationRequired());
        assertTrue(status.hasDisplayFields());
        assertTrue(status.hasSearchableFields());
        assertEquals(1, result.total());
        assertEquals("Gamma Kft", result.rows().get(0).values().get("Field_NAME"));
    }

    @Test
    void refreshAfterSaveUpdatesOnlyTargetOccurrenceInMemoryIndex() throws Exception {
        stubFileLookup();
        when(indexConfig.structure("26HIPAK", "1.0.0")).thenReturn(structure(fieldsForAttachment(), null));
        service.page(77L, "Form_26HIPAKM", 0, 50, "");

        String updatedFragment = "<Form_26HIPAKM><Field_SHARED>B-999</Field_SHARED><Field_NAME>Frissített Kft</Field_NAME><Field_CITY>Győr</Field_CITY></Form_26HIPAKM>";
        service.refreshAfterSave(77L, "Form_26HIPAKM", 2, updatedFragment);

        LargeXmlMultiformPageService.PageResult result = service.page(77L, "Form_26HIPAKM", 0, 50, "");
        assertEquals("A-001", result.rows().get(0).values().get("Field_SHARED"));
        assertEquals("B-999", result.rows().get(1).values().get("Field_SHARED"));
        assertEquals("C-003", result.rows().get(2).values().get("Field_SHARED"));
        assertTrue(result.rows().get(1).xml().contains("Frissített Kft"));
    }

    private void stubFileLookup() {
        when(RepositoryAccess.findById(xmlFiles, 77L)).thenReturn(Optional.of(file));
    }

    private List<IndexFieldDto> fieldsForAttachment() {
        return List.of(
                field("Field_SHARED", "Azonosító", "/Doc_26HIPAK/Form_26HIPAKM/Field_SHARED", true, true, true),
                field("Field_NAME", "Név", "/Doc_26HIPAK/Form_26HIPAKM/Field_NAME", true, false, false),
                field("Field_CITY", "Település", "/Doc_26HIPAK/Form_26HIPAKM/Field_CITY", false, true, false),
                fieldForPart("Field_SHARED", "Főlap azonosító", "/Doc_26HIPAK/Form_26HIPAKA/Field_SHARED", "Form_26HIPAKA", true, true, true)
        );
    }

    private IndexFieldDto field(String name, String label, String path, boolean searchable, boolean display, boolean defaultSearch) {
        return fieldForPart(name, label, path, "Form_26HIPAKM", searchable, display, defaultSearch);
    }

    private IndexFieldDto fieldForPart(String name, String label, String path, String formPart, boolean searchable, boolean display, boolean defaultSearch) {
        IndexFieldDto field = new IndexFieldDto();
        field.setName(name);
        field.setLabel(label);
        field.setXmlPath(path);
        field.setFormPartName(formPart);
        field.setSearchable(searchable);
        field.setDisplay(display);
        field.setDefaultSearch(defaultSearch);
        return field;
    }

    private StructureResponse structure(List<IndexFieldDto> fields, IndexFormConfigDto saved) {
        return new StructureResponse("26HIPAK", "HIPAK", "1.0.0", "schema.xsd", List.of(), List.of(), fields, List.of(), saved);
    }
}
