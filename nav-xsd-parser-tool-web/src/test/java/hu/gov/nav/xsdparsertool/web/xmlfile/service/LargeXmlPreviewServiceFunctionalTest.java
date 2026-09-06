package hu.gov.nav.xsdparsertool.web.xmlfile.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LargeXmlPreviewServiceFunctionalTest {

    @TempDir Path tempDir;

    @Test
    void previewContainsMainFormAndOnlyFirstRepeatingAttachmentButReportsFullCount() throws Exception {
        Path source = tempDir.resolve("large.xml");
        Files.writeString(source, """
                <Doc_26HIPAK>
                  <Form_26HIPAKA><Field_ID>MAIN</Field_ID></Form_26HIPAKA>
                  <Form_26HIPAKM><Field_ID>M1</Field_ID></Form_26HIPAKM>
                  <Form_26HIPAKM><Field_ID>M2</Field_ID></Form_26HIPAKM>
                  <Form_26HIPAKM><Field_ID>M3</Field_ID></Form_26HIPAKM>
                </Doc_26HIPAK>
                """);

        LargeXmlPreviewService.PreviewResult result = new LargeXmlPreviewService().createMainFormPreview(source);
        try {
            assertEquals("Form_26HIPAKA", result.mainFormName());
            assertEquals("Form_26HIPAKM", result.repeatingFormName());
            assertEquals(3L, result.repeatingFormCount());
            String preview = Files.readString(result.previewPath());
            assertTrue(preview.contains("MAIN"));
            assertTrue(preview.contains("M1"));
            assertFalse(preview.contains("M2"));
            assertFalse(preview.contains("M3"));
            assertEquals(List.of("Form_26HIPAKA", "Form_26HIPAKM"), directFormNames(result.previewPath()));
        } finally {
            Files.deleteIfExists(result.previewPath());
        }
    }

    @Test
    void previewUsesLocalNamesWhenDocumentHasNamespacePrefix() throws Exception {
        Path source = tempDir.resolve("namespaced.xml");
        Files.writeString(source, """
                <n:Doc_26HIPAK xmlns:n="urn:test">
                  <n:Form_26HIPAKA><n:Field_ID>MAIN</n:Field_ID></n:Form_26HIPAKA>
                  <n:Form_26HIPAKM><n:Field_ID>M1</n:Field_ID></n:Form_26HIPAKM>
                  <n:Form_26HIPAKM><n:Field_ID>M2</n:Field_ID></n:Form_26HIPAKM>
                </n:Doc_26HIPAK>
                """);

        LargeXmlPreviewService.PreviewResult result = new LargeXmlPreviewService().createMainFormPreview(source);
        try {
            assertEquals("Form_26HIPAKA", result.mainFormName());
            assertEquals("Form_26HIPAKM", result.repeatingFormName());
            assertEquals(2L, result.repeatingFormCount());
            assertEquals(List.of("Form_26HIPAKA", "Form_26HIPAKM"), directFormNames(result.previewPath()));
        } finally {
            Files.deleteIfExists(result.previewPath());
        }
    }

    @Test
    void previewRejectsDocumentWithoutDirectFormPart() throws Exception {
        Path source = tempDir.resolve("invalid.xml");
        Files.writeString(source, "<Root><Container><Form_X/></Container></Root>");

        assertThrows(java.io.IOException.class, () -> new LargeXmlPreviewService().createMainFormPreview(source));
    }

    private List<String> directFormNames(Path xml) throws Exception {
        List<String> names = new ArrayList<>();
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
        try (InputStream input = Files.newInputStream(xml)) {
            XMLStreamReader reader = factory.createXMLStreamReader(input);
            int depth = 0;
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamReader.START_ELEMENT) {
                    depth++;
                    if (depth == 2 && reader.getLocalName().startsWith("Form_")) names.add(reader.getLocalName());
                } else if (event == XMLStreamReader.END_ELEMENT) {
                    depth--;
                }
            }
            reader.close();
        }
        return names;
    }
}
