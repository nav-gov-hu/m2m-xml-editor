package hu.gov.nav.xsdparsertool.processing.xmlview;

import hu.gov.nav.xsdparsertool.core.model.xmlview.XmlDocumentView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DefaultXmlViewBuilderServiceTest {
    @TempDir Path tempDir;

    @Test
    void buildsStableIndexedPathsForRepeatedElements() throws Exception {
        Path xml = tempDir.resolve("input.xml");
        Files.writeString(xml, "<Root code=\"A\"><Item>one</Item><Item>two</Item></Root>");

        XmlDocumentView view = new DefaultXmlViewBuilderService().build(xml);

        assertEquals("Root", view.getRoot().getName());
        assertEquals("A", view.getRoot().getAttributes().get("code"));
        assertEquals("/Root/Item[1]", view.getRoot().getChildren().get(0).getPath());
        assertEquals("/Root/Item[2]", view.getRoot().getChildren().get(1).getPath());
        assertEquals("two", view.getRoot().getChildren().get(1).getTextValue());
    }

    @Test
    void leafTextIsTrimmedAndContainerTextIsNotDuplicated() throws Exception {
        Path xml = tempDir.resolve("text.xml");
        Files.writeString(xml, "<Root> ignored <Container><Leaf>  value  </Leaf></Container></Root>");

        XmlDocumentView view = new DefaultXmlViewBuilderService().build(xml);

        assertNull(view.getRoot().getTextValue());
        assertNull(view.getRoot().getChildren().get(0).getTextValue());
        assertEquals("value", view.getRoot().getChildren().get(0).getChildren().get(0).getTextValue());
        assertEquals(Files.readString(xml), view.getRawXml());
    }

    @Test
    void namespacedElementsUseLocalNamesAndKeepQualifiedAttributes() throws Exception {
        Path xml = tempDir.resolve("ns.xml");
        Files.writeString(xml, "<n:Root xmlns:n=\"urn:test\" n:code=\"A\"><n:Item>one</n:Item></n:Root>");

        XmlDocumentView view = new DefaultXmlViewBuilderService().build(xml);

        assertEquals("Root", view.getRoot().getName());
        assertEquals("Item", view.getRoot().getChildren().get(0).getName());
        assertEquals("/Root/Item[1]", view.getRoot().getChildren().get(0).getPath());
        assertEquals("A", view.getRoot().getAttributes().get("n:code"));
    }

    @Test
    void malformedXmlIsWrappedAsIllegalState() throws Exception {
        Path xml = tempDir.resolve("broken.xml");
        Files.writeString(xml, "<Root><Item></Root>");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new DefaultXmlViewBuilderService().build(xml));

        assertTrue(error.getMessage().contains("Failed to build XML view"));
    }

}
