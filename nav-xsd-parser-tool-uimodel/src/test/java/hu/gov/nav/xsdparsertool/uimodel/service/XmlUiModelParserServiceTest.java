package hu.gov.nav.xsdparsertool.uimodel.service;

import hu.gov.nav.xsdparsertool.core.model.definition.BlockDefinition;
import hu.gov.nav.xsdparsertool.core.model.definition.DocumentDefinition;
import hu.gov.nav.xsdparsertool.core.model.definition.FieldDefinition;
import hu.gov.nav.xsdparsertool.uimodel.model.UiModelMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class XmlUiModelParserServiceTest {
    @TempDir Path tempDir;

    @Test
    void parsesDocumentFieldGroupAndFieldMetadata() throws Exception {
        Path ui = tempDir.resolve("ui.xml");
        Files.writeString(ui, """
                <UiModel id="DOC" webName="Teszt űrlap" major="2" minor="7" tipus="T">
                  <Form>
                    <FieldGroup id="100" name="Személyes adatok" index="1">
                      <Fid id="ABC"/>
                      <Field id="ABC" label="Név" type="string" mask="MASK" maxLength="30" mandatory="true"/>
                    </FieldGroup>
                  </Form>
                </UiModel>
                """);

        UiModelMetadata metadata = new XmlUiModelParserService().parse(ui);

        assertEquals("DOC", metadata.getDocumentId());
        assertEquals("Teszt űrlap", metadata.getTitle());
        assertEquals("2.7", metadata.getVersion());
        assertEquals("Személyes adatok", metadata.getBlockGroupsById().get("100").getTitle());
        assertEquals("Név", metadata.getFieldsById().get("ABC").getLabel());
        assertTrue(metadata.getFieldsById().get("ABC").isRequired());
    }

    @Test
    void applyUiModelOverridesUiLabelButKeepsXsdLabel() throws Exception {
        Path ui = tempDir.resolve("ui.xml");
        Files.writeString(ui, """
                <UiModel id="DOC" webName="UI cím"><Form>
                  <FieldGroup id="100" name="UI blokk"><Field id="ABC" label="UI mező" type="string"/></FieldGroup>
                </Form></UiModel>
                """);
        DocumentDefinition definition = new DocumentDefinition();
        definition.setTitle("XSD cím");
        BlockDefinition block = new BlockDefinition();
        block.setId("100");
        FieldDefinition field = new FieldDefinition();
        field.setId("ABC");
        field.setXsdLabel("XSD mező");
        field.setLabel("XSD mező");
        block.getFields().add(field);
        definition.getBlocks().add(block);

        new XmlUiModelParserService().applyUiModel(definition, ui);

        assertEquals("UI cím", definition.getTitle());
        assertEquals("UI blokk", block.getTitle());
        assertEquals("UI mező", field.getUiLabel());
        assertEquals("UI mező", field.getLabel());
        assertEquals("XSD mező", field.getXsdLabel());
    }
}
