package hu.gov.nav.xsdparsertool.processing.form;

import hu.gov.nav.xsdparsertool.core.model.form.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DefaultFormDataBuilderServiceTest {
    @TempDir Path tempDir;

    @Test
    void bindsSimpleFieldByFullXmlPath() throws Exception {
        Path xml = tempDir.resolve("input.xml");
        Files.writeString(xml, "<Root><Block><Field_A>value-a</Field_A></Block></Root>");
        FormDefinition definition = definitionWithRow(simpleRow("r1", field("A", "Field_A", "/Root/Block/Field_A")));

        FormData data = new DefaultFormDataBuilderService().build(definition, xml);

        assertEquals("value-a", data.getValuesByFieldId().get("A").getValue());
        assertTrue(data.getValuesByFieldId().get("A").isPresent());
    }

    @Test
    void bindsRepeatedRowsWithoutCollapsingOccurrences() throws Exception {
        Path xml = tempDir.resolve("repeat.xml");
        Files.writeString(xml, "<Root><Row><Field_A>one</Field_A></Row><Row><Field_A>two</Field_A></Row></Root>");
        FormRowDefinition row = simpleRow("rows", field("A", "Field_A", "/Root/Row/Field_A"));
        row.setRepeatable(true);
        row.setXmlPath("/Root/Row");
        FormDefinition definition = definitionWithRow(row);

        FormData data = new DefaultFormDataBuilderService().build(definition, xml);

        assertEquals(2, data.getRowInstancesByRowId().get("rows").size());
        assertEquals("one", data.getRowInstancesByRowId().get("rows").get(0).getValuesByFieldId().get("A").getValue());
        assertEquals("two", data.getRowInstancesByRowId().get("rows").get(1).getValuesByFieldId().get("A").getValue());
        assertNotEquals(data.getRowInstancesByRowId().get("rows").get(0).getXmlPath(), data.getRowInstancesByRowId().get("rows").get(1).getXmlPath());
    }


    @Test
    void missingFieldIsRecordedAsNotPresent() throws Exception {
        Path xml = tempDir.resolve("missing.xml");
        Files.writeString(xml, "<Root><Block/></Root>");
        FormDefinition definition = definitionWithRow(simpleRow("r1", field("A", "Field_A", "/Root/Block/Field_A")));

        FormData data = new DefaultFormDataBuilderService().build(definition, xml);

        assertNull(data.getValuesByFieldId().get("A").getValue());
        assertFalse(data.getValuesByFieldId().get("A").isPresent());
    }

    @Test
    void indexedPathSelectsRequestedOccurrence() throws Exception {
        Path xml = tempDir.resolve("indexed.xml");
        Files.writeString(xml, "<Root><Row><Field_A>one</Field_A></Row><Row><Field_A>two</Field_A></Row></Root>");
        FormDefinition definition = definitionWithRow(simpleRow("r1", field("A", "Field_A", "/Root/Row[2]/Field_A")));

        FormData data = new DefaultFormDataBuilderService().build(definition, xml);

        assertEquals("two", data.getValuesByFieldId().get("A").getValue());
    }

    @Test
    void namespacePrefixesDoNotBreakLocalNamePathBinding() throws Exception {
        Path xml = tempDir.resolve("namespaced.xml");
        Files.writeString(xml, "<n:Root xmlns:n=\"urn:test\"><n:Block><n:Field_A> value </n:Field_A></n:Block></n:Root>");
        FormDefinition definition = definitionWithRow(simpleRow("r1", field("A", "Field_A", "/Root/Block/Field_A")));

        FormData data = new DefaultFormDataBuilderService().build(definition, xml);

        assertEquals("value", data.getValuesByFieldId().get("A").getValue());
    }

    @Test
    void malformedXmlIsWrappedAsIllegalState() throws Exception {
        Path xml = tempDir.resolve("broken.xml");
        Files.writeString(xml, "<Root><Block></Root>");
        FormDefinition definition = definitionWithRow(simpleRow("r1", field("A", "Field_A", "/Root/Block/Field_A")));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new DefaultFormDataBuilderService().build(definition, xml));

        assertTrue(error.getMessage().contains("Failed to build form data from XML"));
    }

    private static FormFieldDefinition field(String id, String xmlName, String xmlPath) {
        FormFieldDefinition field = new FormFieldDefinition();
        field.setId(id); field.setXmlName(xmlName); field.setXmlPath(xmlPath);
        return field;
    }

    private static FormRowDefinition simpleRow(String id, FormFieldDefinition field) {
        FormRowDefinition row = new FormRowDefinition();
        row.setId(id); row.getFields().add(field);
        return row;
    }

    private static FormDefinition definitionWithRow(FormRowDefinition row) {
        FormSectionDefinition section = new FormSectionDefinition(); section.getRows().add(row);
        FormTabDefinition tab = new FormTabDefinition(); tab.getSections().add(section);
        FormDefinition definition = new FormDefinition(); definition.getTabs().add(tab);
        return definition;
    }
}
