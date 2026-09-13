package hu.gov.nav.xsdparsertool.web.xmlfile.service;

import hu.gov.nav.xsdparsertool.core.model.processing.ExportResult;
import hu.gov.nav.xsdparsertool.processing.service.XmlProcessingService;
import hu.gov.nav.xsdparsertool.schemaregistry.model.SchemaDocumentOption;
import hu.gov.nav.xsdparsertool.schemaregistry.service.FileSystemSchemaRegistryService;
import hu.gov.nav.xsdparsertool.web.config.PathConfigurationProperties;
import hu.gov.nav.xsdparsertool.web.xmlfile.config.XmlFileStorageProperties;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.CreateXmlFileRequest;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.XmlFileDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class XmlFileGenerationServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void listsGenerationOptionsFromSchemaRepository() throws Exception {
        Path schemaDir = Files.createDirectories(tempDir.resolve("xsd"));
        XmlProcessingService processing = mock(XmlProcessingService.class);
        FileSystemSchemaRegistryService registry = mock(FileSystemSchemaRegistryService.class);
        PathConfigurationProperties paths = mock(PathConfigurationProperties.class);
        XmlFileStorageProperties storage = mock(XmlFileStorageProperties.class);
        XmlFileService xmlFiles = mock(XmlFileService.class);
        when(paths.getSchemaDir()).thenReturn(schemaDir.toString());
        when(registry.listDocumentOptions(schemaDir.toAbsolutePath().normalize()))
                .thenReturn(List.of(new SchemaDocumentOption("NAV_F10", List.of("1.12", "1.13"))));

        XmlFileGenerationService service = new XmlFileGenerationService(processing, registry, paths, storage, xmlFiles);

        var options = service.listOptions();

        assertEquals(1, options.size());
        assertEquals("NAV_F10", options.get(0).formType());
        assertEquals(List.of("1.12", "1.13"), options.get(0).versions());
    }

    @Test
    void createsPhysicalXmlAndRegistersItWithPartner() throws Exception {
        Path schemaDir = Files.createDirectories(tempDir.resolve("xsd"));
        Path uploadDir = tempDir.resolve("xml");
        XmlProcessingService processing = mock(XmlProcessingService.class);
        FileSystemSchemaRegistryService registry = mock(FileSystemSchemaRegistryService.class);
        PathConfigurationProperties paths = mock(PathConfigurationProperties.class);
        XmlFileStorageProperties storage = mock(XmlFileStorageProperties.class);
        XmlFileService xmlFiles = mock(XmlFileService.class);
        when(paths.getSchemaDir()).thenReturn(schemaDir.toString());
        when(storage.getUploadDir()).thenReturn(uploadDir.toString());

        when(processing.generateOpenableXml(eq("NAV_F10"), eq("1.12"), eq(schemaDir.toAbsolutePath().normalize()), any(Path.class)))
                .thenAnswer(invocation -> {
                    Path output = invocation.getArgument(3);
                    Files.writeString(output, "<?xml version=\"1.0\"?><Doc_NAV_F10/>");
                    ExportResult result = new ExportResult();
                    result.setSuccess(true);
                    result.setOutputFile(output);
                    return result;
                });

        XmlFileDto expected = mock(XmlFileDto.class);
        when(xmlFiles.registerGeneratedFile(any(Path.class), eq("uj.xml"), eq("megjegyzés"), eq(42L)))
                .thenReturn(expected);

        XmlFileGenerationService service = new XmlFileGenerationService(processing, registry, paths, storage, xmlFiles);
        XmlFileDto actual = service.create(new CreateXmlFileRequest("NAV_F10", "1.12", "uj.xml", 42L, "megjegyzés"));

        assertEquals(expected, actual);
        assertTrue(Files.isDirectory(uploadDir));
        verify(processing).generateOpenableXml(eq("NAV_F10"), eq("1.12"), eq(schemaDir.toAbsolutePath().normalize()), any(Path.class));
        verify(xmlFiles).registerGeneratedFile(any(Path.class), eq("uj.xml"), eq("megjegyzés"), eq(42L));
    }
}
