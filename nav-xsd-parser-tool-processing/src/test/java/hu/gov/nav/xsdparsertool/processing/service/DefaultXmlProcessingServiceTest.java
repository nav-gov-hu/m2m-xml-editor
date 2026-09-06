package hu.gov.nav.xsdparsertool.processing.service;

import hu.gov.nav.xsdparsertool.core.model.bundle.SchemaBundle;
import hu.gov.nav.xsdparsertool.core.model.definition.DocumentDefinition;
import hu.gov.nav.xsdparsertool.core.model.processing.ValidationResult;
import hu.gov.nav.xsdparsertool.pageschema.service.PageSchemaParserService;
import hu.gov.nav.xsdparsertool.processing.validation.XsdValidationService;
import hu.gov.nav.xsdparsertool.processing.xml.XmlProbeService;
import hu.gov.nav.xsdparsertool.schemaregistry.model.XmlProbeResult;
import hu.gov.nav.xsdparsertool.schemaregistry.service.SchemaRegistryService;
import hu.gov.nav.xsdparsertool.uimodel.service.UiModelParserService;
import hu.gov.nav.xsdparsertool.xsd.service.XsdParserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DefaultXmlProcessingServiceTest {

    @TempDir Path tempDir;

    @Test
    void inspectDelegatesToRegistryParserAndOptionalDecorators() throws Exception {
        Path xml = Files.writeString(tempDir.resolve("input.xml"), "<Root/>");
        Path schemaRoot = Files.createDirectory(tempDir.resolve("schemas"));
        Path general = Files.createDirectory(tempDir.resolve("general"));
        Path uiDir = Files.createDirectory(tempDir.resolve("ui"));
        Path uiFile = Files.writeString(uiDir.resolve("model.xml"), "<ui/>");
        Path pageFile = Files.writeString(tempDir.resolve("page.xml"), "<page/>");

        XmlProbeService probeService = mock(XmlProbeService.class);
        SchemaRegistryService registry = mock(SchemaRegistryService.class);
        XsdParserService xsdParser = mock(XsdParserService.class);
        UiModelParserService uiParser = mock(UiModelParserService.class);
        PageSchemaParserService pageParser = mock(PageSchemaParserService.class);
        XsdValidationService validation = mock(XsdValidationService.class);

        XmlProbeResult probe = new XmlProbeResult();
        SchemaBundle bundle = new SchemaBundle();
        bundle.setUiModelFile(uiFile);
        bundle.setPageSchemaFile(pageFile);
        DocumentDefinition definition = new DocumentDefinition();
        when(probeService.probe(xml)).thenReturn(probe);
        when(registry.resolveByXmlProbe(probe, schemaRoot, general, uiDir)).thenReturn(bundle);
        when(xsdParser.parse(bundle)).thenReturn(definition);

        DefaultXmlProcessingService service = service(probeService, registry, xsdParser, uiParser, pageParser, validation);
        var result = service.inspect(xml, schemaRoot, general, uiDir);

        assertSame(bundle, result.getSchemaBundle());
        assertSame(definition, result.getDocumentDefinition());
        verify(uiParser).applyUiModel(definition, uiFile);
        verify(pageParser).applyPageSchema(definition, pageFile);
    }

    @Test
    void inspectSkipsOptionalDecoratorsWhenBundleHasNoFiles() {
        XmlProbeService probeService = mock(XmlProbeService.class);
        SchemaRegistryService registry = mock(SchemaRegistryService.class);
        XsdParserService xsdParser = mock(XsdParserService.class);
        UiModelParserService uiParser = mock(UiModelParserService.class);
        PageSchemaParserService pageParser = mock(PageSchemaParserService.class);
        XsdValidationService validation = mock(XsdValidationService.class);
        XmlProbeResult probe = new XmlProbeResult();
        SchemaBundle bundle = new SchemaBundle();
        DocumentDefinition definition = new DocumentDefinition();
        Path xml = tempDir.resolve("input.xml");
        Path root = tempDir.resolve("root");

        when(probeService.probe(xml)).thenReturn(probe);
        when(registry.resolveByXmlProbe(probe, root, null, null)).thenReturn(bundle);
        when(xsdParser.parse(bundle)).thenReturn(definition);

        service(probeService, registry, xsdParser, uiParser, pageParser, validation).inspect(xml, root);

        verifyNoInteractions(uiParser, pageParser);
    }

    @Test
    void validateReturnsAllPreflightIssuesWithoutCallingDependencies() throws Exception {
        Path missingXml = tempDir.resolve("missing.xml");
        Path missingRoot = tempDir.resolve("missing-root");
        Path missingGeneral = tempDir.resolve("missing-general");
        Path missingUi = tempDir.resolve("missing-ui");

        XmlProbeService probeService = mock(XmlProbeService.class);
        SchemaRegistryService registry = mock(SchemaRegistryService.class);
        XsdParserService xsdParser = mock(XsdParserService.class);
        UiModelParserService uiParser = mock(UiModelParserService.class);
        PageSchemaParserService pageParser = mock(PageSchemaParserService.class);
        XsdValidationService validation = mock(XsdValidationService.class);

        ValidationResult result = service(probeService, registry, xsdParser, uiParser, pageParser, validation)
                .validate(missingXml, missingRoot, missingGeneral, missingUi);

        assertFalse(result.isValid());
        assertEquals(4, result.getIssues().size());
        assertEquals("FILE_NOT_FOUND", result.getIssues().get(0).getCode());
        assertEquals("SCHEMA_DIR_NOT_FOUND", result.getIssues().get(1).getCode());
        assertEquals("GENERAL_XSD_DIR_NOT_FOUND", result.getIssues().get(2).getCode());
        assertEquals("UI_MODEL_DIR_NOT_FOUND", result.getIssues().get(3).getCode());
        verifyNoInteractions(probeService, registry, xsdParser, uiParser, pageParser, validation);
    }

    @Test
    void validateAddsResolvedSchemaAndUiModelInformation() throws Exception {
        Path xml = Files.writeString(tempDir.resolve("valid.xml"), "<Root/>");
        Path root = Files.createDirectory(tempDir.resolve("root"));
        Path general = Files.createDirectory(tempDir.resolve("general"));
        Path uiDir = Files.createDirectory(tempDir.resolve("ui-dir"));
        Path xsd = Files.writeString(root.resolve("main.xsd"), "<x/>");
        Path uiFile = Files.writeString(uiDir.resolve("ui.xml"), "<u/>");

        XmlProbeService probeService = mock(XmlProbeService.class);
        SchemaRegistryService registry = mock(SchemaRegistryService.class);
        XsdParserService xsdParser = mock(XsdParserService.class);
        UiModelParserService uiParser = mock(UiModelParserService.class);
        PageSchemaParserService pageParser = mock(PageSchemaParserService.class);
        XsdValidationService validation = mock(XsdValidationService.class);
        XmlProbeResult probe = new XmlProbeResult();
        SchemaBundle bundle = new SchemaBundle();
        bundle.setPrimaryXsd(xsd);
        bundle.setUiModelFile(uiFile);
        DocumentDefinition definition = new DocumentDefinition();
        ValidationResult downstream = new ValidationResult();
        downstream.setValid(true);

        when(probeService.probe(xml)).thenReturn(probe);
        when(registry.resolveByXmlProbe(probe, root, general, uiDir)).thenReturn(bundle);
        when(xsdParser.parse(bundle)).thenReturn(definition);
        when(validation.validate(xml, bundle, general)).thenReturn(downstream);

        ValidationResult result = service(probeService, registry, xsdParser, uiParser, pageParser, validation)
                .validate(xml, root, general, uiDir);

        assertTrue(result.isValid());
        assertEquals("SCHEMA_RESOLVED", result.getIssues().get(0).getCode());
        assertEquals("UI_MODEL_RESOLVED", result.getIssues().get(1).getCode());
    }

    @Test
    void generateEmptyXmlUsesRootNameCreatesParentAndReturnsSuccess() throws Exception {
        SchemaRegistryService registry = mock(SchemaRegistryService.class);
        SchemaBundle bundle = new SchemaBundle();
        bundle.setRootElementName("DocRoot");
        when(registry.resolveByDocumentType(eq("DOC"), any(), isNull(), isNull())).thenReturn(bundle);
        DefaultXmlProcessingService service = service(mock(XmlProbeService.class), registry, mock(XsdParserService.class),
                mock(UiModelParserService.class), mock(PageSchemaParserService.class), mock(XsdValidationService.class));
        Path output = tempDir.resolve("nested/out.xml");

        var result = service.generateEmptyXml("DOC", tempDir, output);

        assertTrue(result.isSuccess());
        assertEquals(output, result.getOutputFile());
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<DocRoot></DocRoot>\n", Files.readString(output));
    }

    @Test
    void generateEmptyXmlFallsBackToDocumentTypeAndWrapsWriteFailure() throws Exception {
        SchemaRegistryService registry = mock(SchemaRegistryService.class);
        SchemaBundle bundle = new SchemaBundle();
        bundle.setDocumentType("FallbackDoc");
        when(registry.resolveByDocumentType(eq("DOC"), any(), isNull(), isNull())).thenReturn(bundle);
        DefaultXmlProcessingService service = service(mock(XmlProbeService.class), registry, mock(XsdParserService.class),
                mock(UiModelParserService.class), mock(PageSchemaParserService.class), mock(XsdValidationService.class));
        Path directoryAsOutput = Files.createDirectory(tempDir.resolve("output-dir"));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.generateEmptyXml("DOC", tempDir, directoryAsOutput));

        assertTrue(error.getMessage().contains("Failed to write output XML"));
    }

    private static DefaultXmlProcessingService service(XmlProbeService probeService,
                                                       SchemaRegistryService registry,
                                                       XsdParserService xsdParser,
                                                       UiModelParserService uiParser,
                                                       PageSchemaParserService pageParser,
                                                       XsdValidationService validation) {
        return new DefaultXmlProcessingService(probeService, registry, xsdParser, uiParser, pageParser, validation);
    }

    @Test
    void threeArgumentInspectOverloadPassesGeneralDirectory() {
        XmlProbeService probeService = mock(XmlProbeService.class);
        SchemaRegistryService registry = mock(SchemaRegistryService.class);
        XsdParserService xsdParser = mock(XsdParserService.class);
        UiModelParserService uiParser = mock(UiModelParserService.class);
        PageSchemaParserService pageParser = mock(PageSchemaParserService.class);
        XsdValidationService validation = mock(XsdValidationService.class);
        XmlProbeResult probe = new XmlProbeResult();
        SchemaBundle bundle = new SchemaBundle();
        DocumentDefinition definition = new DocumentDefinition();
        Path xml = tempDir.resolve("three-arg.xml");
        Path root = tempDir.resolve("three-arg-root");
        Path general = tempDir.resolve("three-arg-general");

        when(probeService.probe(xml)).thenReturn(probe);
        when(registry.resolveByXmlProbe(probe, root, general, null)).thenReturn(bundle);
        when(xsdParser.parse(bundle)).thenReturn(definition);

        var result = service(probeService, registry, xsdParser, uiParser, pageParser, validation)
                .inspect(xml, root, general);

        assertSame(bundle, result.getSchemaBundle());
        verify(registry).resolveByXmlProbe(probe, root, general, null);
    }

    @Test
    void validationConvenienceOverloadsDelegateToPreflightValidation() {
        DefaultXmlProcessingService service = service(mock(XmlProbeService.class), mock(SchemaRegistryService.class),
                mock(XsdParserService.class), mock(UiModelParserService.class), mock(PageSchemaParserService.class),
                mock(XsdValidationService.class));

        ValidationResult twoArgs = service.validate(null, null);
        ValidationResult threeArgs = service.validate(null, null, null);

        assertFalse(twoArgs.isValid());
        assertFalse(threeArgs.isValid());
        assertEquals(2, twoArgs.getIssues().size());
        assertEquals(2, threeArgs.getIssues().size());
    }

    @Test
    void fourArgumentGenerateOverloadPassesGeneralDirectory() throws Exception {
        SchemaRegistryService registry = mock(SchemaRegistryService.class);
        SchemaBundle bundle = new SchemaBundle();
        bundle.setRootElementName("GeneralRoot");
        Path general = Files.createDirectory(tempDir.resolve("generate-general"));
        Path output = tempDir.resolve("general-output.xml");
        when(registry.resolveByDocumentType("DOC", tempDir, general, null)).thenReturn(bundle);
        DefaultXmlProcessingService service = service(mock(XmlProbeService.class), registry, mock(XsdParserService.class),
                mock(UiModelParserService.class), mock(PageSchemaParserService.class), mock(XsdValidationService.class));

        var result = service.generateEmptyXml("DOC", tempDir, general, output);

        assertTrue(result.isSuccess());
        assertTrue(Files.readString(output).contains("<GeneralRoot></GeneralRoot>"));
        verify(registry).resolveByDocumentType("DOC", tempDir, general, null);
    }

}
