package hu.gov.nav.xsdparsertool.processing.form;

import hu.gov.nav.xsdparsertool.core.model.bundle.SchemaBundle;
import hu.gov.nav.xsdparsertool.core.model.definition.BlockDefinition;
import hu.gov.nav.xsdparsertool.core.model.definition.DocumentDefinition;
import hu.gov.nav.xsdparsertool.core.model.definition.FieldDefinition;
import hu.gov.nav.xsdparsertool.core.model.form.FormFieldDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DefaultFormDefinitionBuilderServiceCoverageTest {

    @TempDir Path tempDir;

    private final DefaultFormDefinitionBuilderService service = new DefaultFormDefinitionBuilderService();

    @Test
    void nullDocumentIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.build(null, null));
    }

    @Test
    void fallbackBuildsSectionRowAndFields() {
        DocumentDefinition document = document(field("F1", "/Doc/Form/Block/FieldGroup/F1", "xs:string"));
        var form = service.build(document, null);

        assertEquals("DOC", form.getId());
        assertEquals("Dokumentum", form.getTitle());
        assertEquals(1, form.getTabs().size());
        assertEquals(1, form.getTabs().get(0).getSections().size());
        assertEquals(1, form.getTabs().get(0).getSections().get(0).getRows().get(0).getFields().size());
    }

    @Test
    void fallbackPreservesStructuralLabels() {
        DocumentDefinition document = document(field("F1", "/Doc/Form/F1", "xs:string"));
        LinkedHashMap<String, String> labels = new LinkedHashMap<>();
        labels.put("/Doc/Form", "Főlap");
        document.setStructuralLabelsByPath(labels);

        var form = service.build(document, null);

        assertEquals("Főlap", form.getStructuralLabelsByPath().get("/Doc/Form"));
    }

    @Test
    void labelPriorityIsUiThenXsdThenXmlName() {
        FieldDefinition withUi = field("F1", "/D/F1", "xs:string");
        withUi.setUiLabel("UI címke");
        withUi.setXsdLabel("XSD címke");
        FieldDefinition withXsd = field("F2", "/D/F2", "xs:string");
        withXsd.setXsdLabel("XSD 2");
        FieldDefinition raw = field("F3", "/D/F3", "xs:string");

        var fields = service.build(document(withUi, withXsd, raw), null).getTabs().get(0).getSections().get(0).getRows().get(0).getFields();

        assertEquals("UI címke", fields.get(0).getLabel());
        assertEquals("XSD 2", fields.get(1).getLabel());
        assertEquals("F3", fields.get(2).getLabel());
    }

    @Test
    void enumBecomesSelectAndKeepsAllValues() {
        FieldDefinition field = field("STATUS", "/D/STATUS", "xs:string");
        field.setEnumValues(List.of("A", "B", "C"));

        FormFieldDefinition mapped = onlyField(field);

        assertEquals("select", mapped.getType());
        assertEquals(List.of("A", "B", "C"), mapped.getEnumValues());
    }

    @Test
    void primitiveTypesMapToExpectedControls() {
        assertEquals("checkbox", onlyField(field("B", "/B", "xs:boolean")).getType());
        assertEquals("date", onlyField(field("D", "/D", "xs:date")).getType());
        assertEquals("number", onlyField(field("N", "/N", "xs:decimal")).getType());
        assertEquals("text", onlyField(field("T", "/T", "xs:string")).getType());
    }

    @Test
    void requiredRepeatableMaskAndMaxLengthArePreserved() {
        FieldDefinition field = field("F", "/Doc/Form/Chain_1/FieldGroup/F", "xs:string");
        field.setRequired(true);
        field.setMaxOccurs("unbounded");
        field.setMask("AA-999");
        field.setMaxLength(12);

        FormFieldDefinition mapped = onlyField(field);

        assertTrue(mapped.isRequired());
        assertTrue(mapped.isRepeatable());
        assertEquals("AA-999", mapped.getMask());
        assertEquals(12, mapped.getMaxLength());
    }

    @Test
    void rowUsesCommonParentPathAndDetectsChainAsRepeatable() {
        FieldDefinition first = field("F1", "/Doc/Form/Chain_42/Chain_elem/FieldGroup_1/F1", "xs:string");
        FieldDefinition second = field("F2", "/Doc/Form/Chain_42/Chain_elem/FieldGroup_1/F2", "xs:string");

        var row = service.build(document(first, second), null).getTabs().get(0).getSections().get(0).getRows().get(0);

        assertEquals("/Doc/Form/Chain_42/Chain_elem/FieldGroup_1", row.getXmlPath());
        assertTrue(row.isRepeatable());
    }

    @Test
    void emptyBlockDoesNotCreateEmptySection() {
        DocumentDefinition document = new DocumentDefinition();
        document.setId("DOC");
        BlockDefinition block = new BlockDefinition();
        block.setId("B");
        document.setBlocks(List.of(block));

        var form = service.build(document, null);

        assertTrue(form.getTabs().get(0).getSections().isEmpty());
    }


    @Test
    void uiModelBuildUsesMenuOrderingGroupNormalizationAndUiFieldMetadata() throws Exception {
        Path ui = tempDir.resolve("ui.xml");
        Files.writeString(ui, """
                <UiModel id="UI_DOC" webName="UI cím">
                  <MenuItem id="second" name="Második" pos="2"><FieldGroupId id="200"/></MenuItem>
                  <MenuItem id="first" name="Első" pos="1"><FieldGroupId id="100"/></MenuItem>
                  <Form>
                    <FieldGroup id="100" name="UI csoport 1" index="1">
                      <Fid id="F1"/><Field id="F1" label="UI mező" type="boolean" mask="UIMASK" maxLength="7"/>
                    </FieldGroup>
                    <FieldGroup id="200" name="UI csoport 2" index="2"><Fid id="F2"/></FieldGroup>
                  </Form>
                </UiModel>
                """);
        FieldDefinition first = field("F1", "/Doc/Form/FieldGroup_100/F1", "xs:string");
        first.setMask("XSDMASK");
        first.setMaxLength(99);
        FieldDefinition second = field("F2", "/Doc/Form/FieldGroup_200/F2", "xs:string");
        DocumentDefinition document = new DocumentDefinition();
        document.setId("DOC");
        BlockDefinition block1 = block("FieldGroup_100", first);
        BlockDefinition block2 = block("FieldGroup_200", second);
        document.setBlocks(List.of(block1, block2));
        SchemaBundle bundle = new SchemaBundle();
        bundle.setUiModelFile(ui);

        var form = service.build(document, bundle);

        assertEquals("first", form.getTabs().get(0).getSections().get(0).getId());
        assertEquals("second", form.getTabs().get(0).getSections().get(1).getId());
        FormFieldDefinition mapped = form.getTabs().get(0).getSections().get(0).getRows().get(0).getFields().get(0);
        assertEquals("UI mező", mapped.getLabel());
        assertEquals("checkbox", mapped.getType());
        assertEquals("UIMASK", mapped.getMask());
        assertEquals(7, mapped.getMaxLength());
    }

    @Test
    void uiModelKeepsDuplicateFieldIdBoundToCurrentBlock() throws Exception {
        Path ui = tempDir.resolve("duplicate-ui.xml");
        Files.writeString(ui, """
                <UiModel id="DOC">
                  <MenuItem id="main" name="Főlap" pos="1"><FieldGroupId id="100"/></MenuItem>
                  <MenuItem id="attachment" name="Melléklap" pos="2"><FieldGroupId id="200"/></MenuItem>
                  <Form>
                    <FieldGroup id="100" name="Fő"><Fid id="DUP"/></FieldGroup>
                    <FieldGroup id="200" name="Mellék"><Fid id="DUP"/></FieldGroup>
                  </Form>
                </UiModel>
                """);
        FieldDefinition main = field("DUP", "/Doc/Form_MAIN/FieldGroup_100/DUP", "xs:string");
        FieldDefinition attachment = field("DUP", "/Doc/Form_ATTACHMENT/FieldGroup_200/DUP", "xs:string");
        attachment.setEnumValues(List.of("A", "B"));
        DocumentDefinition document = new DocumentDefinition();
        document.setBlocks(List.of(block("FieldGroup_100", main), block("FieldGroup_200", attachment)));
        SchemaBundle bundle = new SchemaBundle();
        bundle.setUiModelFile(ui);

        var form = service.build(document, bundle);

        assertEquals("text", form.getTabs().get(0).getSections().get(0).getRows().get(0).getFields().get(0).getType());
        assertEquals("select", form.getTabs().get(0).getSections().get(1).getRows().get(0).getFields().get(0).getType());
    }

    @Test
    void unmatchedUiGroupIsSkippedAndRemainingFieldsBecomeUngrouped() throws Exception {
        Path ui = tempDir.resolve("ungrouped-ui.xml");
        Files.writeString(ui, """
                <UiModel id="DOC">
                  <MenuItem id="missing" name="Missing" pos="1"><FieldGroupId id="999"/></MenuItem>
                  <Form><FieldGroup id="999" name="Missing"><Fid id="UNKNOWN"/></FieldGroup></Form>
                </UiModel>
                """);
        FieldDefinition field = field("F1", "/Doc/Form/FieldGroup_100/F1", "xs:string");
        DocumentDefinition document = new DocumentDefinition();
        document.setBlocks(List.of(block("FieldGroup_100", field)));
        SchemaBundle bundle = new SchemaBundle();
        bundle.setUiModelFile(ui);

        var form = service.build(document, bundle);

        assertEquals(1, form.getTabs().get(0).getSections().size());
        assertEquals("ungrouped", form.getTabs().get(0).getSections().get(0).getId());
        assertEquals("F1", form.getTabs().get(0).getSections().get(0).getRows().get(0).getFields().get(0).getId());
    }

    @Test
    void fallbackHandlesNumericAndInvalidRepeatCountsAndCommonPrefix() {
        FieldDefinition first = field("F1", "/Doc/Form/A/F1", "xs:string");
        first.setMaxOccurs("2");
        FieldDefinition second = field("F2", "/Doc/Form/B/F2", "xs:string");
        second.setMaxOccurs("not-a-number");

        var row = service.build(document(first, second), null).getTabs().get(0).getSections().get(0).getRows().get(0);

        assertEquals("/Doc/Form", row.getXmlPath());
        assertFalse(row.isRepeatable());
        assertTrue(row.getFields().get(0).isRepeatable());
        assertFalse(row.getFields().get(1).isRepeatable());
    }

    private FormFieldDefinition onlyField(FieldDefinition field) {
        return service.build(document(field), null).getTabs().get(0).getSections().get(0).getRows().get(0).getFields().get(0);
    }


    private BlockDefinition block(String id, FieldDefinition... fields) {
        BlockDefinition block = new BlockDefinition();
        block.setId(id);
        block.setName(id);
        block.setTitle(id);
        block.setFields(List.of(fields));
        return block;
    }

    private DocumentDefinition document(FieldDefinition... fields) {
        DocumentDefinition document = new DocumentDefinition();
        document.setId("DOC");
        document.setName("DocName");
        document.setTitle("Dokumentum");
        BlockDefinition block = new BlockDefinition();
        block.setId("FieldGroup_100");
        block.setName("Csoport");
        block.setTitle("Csoport cím");
        block.setFields(List.of(fields));
        document.setBlocks(List.of(block));
        return document;
    }

    private FieldDefinition field(String id, String path, String type) {
        FieldDefinition field = new FieldDefinition();
        field.setId(id);
        field.setXmlName(id);
        field.setXmlPath(path);
        field.setDataType(type);
        return field;
    }

    @Test
    void uiModelMapsLayoutWidthsReadonlyAndAllUiTypeBranches() throws Exception {
        Path ui = tempDir.resolve("layout-ui.xml");
        Files.writeString(ui, """
                <UiModel id="DOC">
                  <MenuItem id="main" name="Main" pos="1"><FieldGroupId id="100"/></MenuItem>
                  <Form>
                    <FieldGroup id="100" name="Group">
                      <Fid id="DATE"/><Fid id="NUM"/><Fid id="TEXT"/><Fid id="CUSTOM"/><Fid id="BOOL"/>
                      <Field id="DATE" type="date" grid="10" readonly="true"/>
                      <Field id="NUM" type="numeric" grid="7"/>
                      <Field id="TEXT" type="text" grid="5"/>
                      <Field id="CUSTOM" type="custom" grid="3"/>
                      <Field id="BOOL" type="boolean" grid="2"/>
                    </FieldGroup>
                  </Form>
                </UiModel>
                """);

        FieldDefinition date = field("DATE", "/Doc/Form/FieldGroup_100/DATE", "xs:string");
        FieldDefinition num = field("NUM", "/Doc/Form/FieldGroup_100/NUM", "xs:string");
        FieldDefinition text = field("TEXT", "/Doc/Form/FieldGroup_100/TEXT", "xs:string");
        FieldDefinition custom = field("CUSTOM", "/Doc/Form/FieldGroup_100/CUSTOM", "xs:double");
        FieldDefinition bool = field("BOOL", "/Doc/Form/FieldGroup_100/BOOL", "xs:string");
        SchemaBundle bundle = new SchemaBundle();
        bundle.setUiModelFile(ui);
        var fields = service.build(document(date, num, text, custom, bool), bundle)
                .getTabs().get(0).getSections().get(0).getRows().get(0).getFields();

        assertEquals(List.of(12, 8, 6, 4, 3), fields.stream().map(FormFieldDefinition::getLayoutWidth).toList());
        assertEquals(List.of("date", "number", "text", "number", "checkbox"), fields.stream().map(FormFieldDefinition::getType).toList());
        assertTrue(fields.get(0).isReadonly());
        assertFalse(fields.get(1).isReadonly());
    }

    @Test
    void uiModelNormalizesGroupAndFieldPrefixes() throws Exception {
        Path ui = tempDir.resolve("normalized-ui.xml");
        Files.writeString(ui, """
                <UiModel id="DOC">
                  <MenuItem id="main" name="Main" pos="1"><FieldGroupId id="100"/></MenuItem>
                  <Form><FieldGroup id="FieldGroup_100" name="Group"><Fid id="Field_F1"/></FieldGroup></Form>
                </UiModel>
                """);
        FieldDefinition field = field("F1", "/Doc/Form/FieldGroup_100/F1", "xs:string");
        DocumentDefinition document = new DocumentDefinition();
        document.setId("DOC");
        document.setBlocks(List.of(block("FieldGroup_100", field)));
        SchemaBundle bundle = new SchemaBundle();
        bundle.setUiModelFile(ui);

        var form = service.build(document, bundle);

        assertEquals("F1", form.getTabs().get(0).getSections().get(0).getRows().get(0).getFields().get(0).getId());
    }

    @Test
    void uiModelGroupFallsBackToBlockFieldsWhenFidsCannotBeResolved() throws Exception {
        Path ui = tempDir.resolve("group-fallback-ui.xml");
        Files.writeString(ui, """
                <UiModel id="DOC">
                  <MenuItem id="main" name="Main" pos="1"><FieldGroupId id="100"/></MenuItem>
                  <Form><FieldGroup id="100" name="Group"><Fid id="UNKNOWN"/></FieldGroup></Form>
                </UiModel>
                """);
        FieldDefinition actual = field("ACTUAL", "/Doc/Form/FieldGroup_100/ACTUAL", "xs:string");
        DocumentDefinition document = new DocumentDefinition();
        document.setId("DOC");
        document.setBlocks(List.of(block("FieldGroup_100", actual)));
        SchemaBundle bundle = new SchemaBundle();
        bundle.setUiModelFile(ui);

        var form = service.build(document, bundle);

        assertEquals("ACTUAL", form.getTabs().get(0).getSections().get(0).getRows().get(0).getFields().get(0).getId());
    }

    @Test
    void emptyUiModelFallsBackToStructuralForm() throws Exception {
        Path ui = tempDir.resolve("empty-ui.xml");
        Files.writeString(ui, "<UiModel id=\"UI_ONLY\"><Form/></UiModel>");
        SchemaBundle bundle = new SchemaBundle();
        bundle.setUiModelFile(ui);
        DocumentDefinition document = document(field("F1", "/Doc/Form/FieldGroup_100/F1", "xs:string"));

        var form = service.build(document, bundle);

        assertEquals("DOC", form.getId());
        assertEquals("F1", form.getTabs().get(0).getSections().get(0).getRows().get(0).getFields().get(0).getId());
    }

    @Test
    void fallbackCoversNullDatatypeIdFallbackAndDebugFieldMapping() {
        FieldDefinition noId = new FieldDefinition();
        noId.setXmlName("RAW");
        noId.setXmlPath("/Doc/Form/RAW");
        noId.setDataType(null);
        noId.setMaxLength(11);

        FieldDefinition debug = field("0A0001D003A", "/Doc/Form/0A0001D003A", "xs:integer");

        var fields = service.build(document(noId, debug), null)
                .getTabs().get(0).getSections().get(0).getRows().get(0).getFields();

        assertEquals("RAW", fields.get(0).getId());
        assertEquals("text", fields.get(0).getType());
        assertEquals(11, fields.get(0).getMaxLength());
        assertEquals("number", fields.get(1).getType());
    }

    @Test
    void fallbackWithNullBlocksProducesEmptyMainTab() {
        DocumentDefinition document = new DocumentDefinition();
        document.setId("EMPTY");
        document.setTitle("Empty");
        document.setBlocks(null);

        var form = service.build(document, null);

        assertEquals("EMPTY", form.getId());
        assertEquals("Empty", form.getTitle());
        assertEquals(1, form.getTabs().size());
        assertTrue(form.getTabs().get(0).getSections().isEmpty());
    }

    @Test
    void fallbackSkipsBlockWithoutFields() {
        BlockDefinition block = new BlockDefinition();
        block.setId("B1");
        block.setName("Blokk");
        block.setFields(null);
        DocumentDefinition document = new DocumentDefinition();
        document.setId("DOC");
        document.setBlocks(List.of(block));

        var form = service.build(document, null);

        assertTrue(form.getTabs().get(0).getSections().isEmpty());
    }

    @Test
    void fallbackMarksChainRowRepeatable() {
        FieldDefinition field = field("F1", "/Doc/Form/Chain_elem/FieldGroup_100/F1", "xs:string");

        var row = service.build(document(field), null)
                .getTabs().get(0).getSections().get(0).getRows().get(0);

        assertTrue(row.isRepeatable());
        assertEquals("/Doc/Form/Chain_elem/FieldGroup_100", row.getXmlPath());
    }

    @Test
    void fallbackUsesNameWhenDocumentTitleIsBlank() {
        DocumentDefinition document = document(field("F1", "/Doc/F1", "xs:string"));
        document.setTitle("   ");
        document.setName("Név alapú cím");

        var form = service.build(document, null);

        assertEquals("Név alapú cím", form.getTitle());
    }

    @Test
    void fallbackMapsRemainingXsdDatatypeBranches() {
        FieldDefinition bool = field("BOOL", "/Doc/BOOL", "xs:boolean");
        FieldDefinition date = field("DATE", "/Doc/DATE", "xs:dateTime");
        FieldDefinition decimal = field("DEC", "/Doc/DEC", "xs:decimal");
        FieldDefinition floatField = field("FLOAT", "/Doc/FLOAT", "xs:float");
        FieldDefinition nonNegative = field("NNI", "/Doc/NNI", "xs:nonNegativeInteger");

        var fields = service.build(document(bool, date, decimal, floatField, nonNegative), null)
                .getTabs().get(0).getSections().get(0).getRows().get(0).getFields();

        assertEquals(List.of("checkbox", "date", "number", "number", "number"),
                fields.stream().map(FormFieldDefinition::getType).toList());
    }

    @Test
    void fallbackHandlesBlankAndRootLikeXmlPaths() {
        FieldDefinition blank = field("BLANK", "   ", "xs:string");
        FieldDefinition rootLike = field("ROOT", "ROOT", "xs:string");

        var row = service.build(document(blank, rootLike), null)
                .getTabs().get(0).getSections().get(0).getRows().get(0);

        assertEquals("ROOT", row.getXmlPath());
        assertFalse(row.isRepeatable());
    }

}
