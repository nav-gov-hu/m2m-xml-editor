package hu.gov.nav.xsdparsertool.print.service;

import hu.gov.nav.xsdparsertool.core.model.bundle.SchemaBundle;
import hu.gov.nav.xsdparsertool.core.model.form.*;
import hu.gov.nav.xsdparsertool.print.model.PrintOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DefaultXmlPrintServiceHtmlTest {
    @TempDir Path tempDir;

    @Test
    void embedsA4AndPaginationPrintContracts() throws Exception {
        String html = render(definitionWith(row("r", field("f", "Field_A", "Címke", null))), data("f", "érték"), options());
        assertAll(
                () -> assertTrue(html.contains("@page{size:A4 portrait;margin:12mm;}")),
                () -> assertTrue(html.contains("border-collapse:collapse")),
                () -> assertTrue(html.contains(".visual-table thead{display:table-header-group;}")),
                () -> assertTrue(html.contains("break-inside:avoid")),
                () -> assertTrue(html.contains("@media print{button{display:none !important;}}"))
        );
    }

    @Test
    void escapesTitleLabelsAndValues() throws Exception {
        FormDefinition definition = definitionWith(row("r", field("f", "Field_A", "A < B & C", null)));
        definition.setTitle("Teszt <nyomtatás> & cím");
        String html = render(definition, data("f", "<script>alert(1)</script> & adat"), options());
        assertFalse(html.contains("<script>alert(1)</script>"));
        assertTrue(html.contains("Teszt &lt;nyomtatás&gt; &amp; cím"));
        assertTrue(html.contains("A &lt; B &amp; C"));
        assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt; &amp; adat"));
    }

    @Test
    void onlyFilledFieldsSuppressesEmptyCells() throws Exception {
        FormRowDefinition row = row("r",
                field("filled", "Field_Filled", "Kitöltött", null),
                field("empty", "Field_Empty", "Üres", null));
        FormData data = new FormData();
        data.getValuesByFieldId().put("filled", value("filled", "adat"));
        data.getValuesByFieldId().put("empty", value("empty", "   "));
        PrintOptions options = options(); options.setOnlyFilledFields(true);
        String html = render(definitionWith(row), data, options);
        assertTrue(html.contains("Kitöltött"));
        assertFalse(html.contains(">Üres<"));
    }

    @Test
    void showFieldIdsAddsTechnicalIdentityWithoutReplacingValue() throws Exception {
        PrintOptions options = options(); options.setShowFieldIds(true);
        String html = render(definitionWith(row("r", field("f42", "Field_ABC", "Felirat", null))), data("f42", "érték"), options);
        assertTrue(html.contains("érték [f42 / Field_ABC]"));
    }

    @Test
    void checkboxValuesUseHungarianPrintableLabels() throws Exception {
        FormFieldDefinition field = field("flag", "Flag", "Jelölés", null); field.setType("checkbox");
        assertTrue(render(definitionWith(row("r", field)), data("flag", "true"), options()).contains(">Igen<"));
        assertTrue(render(definitionWith(row("r", field)), data("flag", "0"), options()).contains(">Nem<"));
    }

    @Test
    void longValuesUseFullWidthPrintCells() throws Exception {
        String longValue = "x".repeat(90);
        String html = render(definitionWith(row("r", field("f", "Field_A", "Hosszú", null))), data("f", longValue), options());
        assertTrue(html.contains("class=\"full-value-cell\" colspan=\"3\""));
        assertTrue(html.contains(longValue));
    }

    @Test
    void numberedLongSeriesRendersVisualTableWithStableColumns() throws Exception {
        FormRowDefinition row = row("table",
                field("a", "A", "01. Nagyon hosszú megnevezés az első sorhoz", 4),
                field("b", "B", "02. Nagyon hosszú megnevezés a második sorhoz", 4),
                field("c", "C", "03. Nagyon hosszú megnevezés a harmadik sorhoz", 4));
        FormData data = new FormData();
        data.getValuesByFieldId().put("a", value("a", "100"));
        data.getValuesByFieldId().put("b", value("b", "200"));
        data.getValuesByFieldId().put("c", value("c", "300"));
        String html = render(definitionWith(row), data, options());
        assertTrue(html.contains("<table class=\"visual-table\">"));
        assertTrue(html.contains("<th class=\"visual-row-no\">Sor</th><th class=\"visual-description\">Megnevezés</th><th class=\"visual-value\">Érték</th>"));
        assertTrue(html.contains(">01.</td>"));
        assertTrue(html.contains("Nagyon hosszú megnevezés az első sorhoz"));
    }

    @Test
    void repeatableRowsRenderEveryOccurrenceWithoutValueMixing() throws Exception {
        FormRowDefinition repeated = row("attachments", field("same", "Field_Same", "Azonos mező", null));
        repeated.setRepeatable(true);
        FormData data = new FormData();
        FormRowInstance first = instance("/Doc/Form_M[1]", "same", "első");
        FormRowInstance second = instance("/Doc/Form_M[2]", "same", "második");
        data.getRowInstancesByRowId().put("attachments", List.of(first, second));
        String html = render(definitionWith(repeated), data, options());
        assertTrue(html.contains("#1"));
        assertTrue(html.contains("#2"));
        assertEquals(1, occurrences(html, ">első<"));
        assertEquals(1, occurrences(html, ">második<"));
    }

    @Test
    void mainAndAttachmentSectionsRemainSeparateInMultiformPrint() throws Exception {
        FormDefinition definition = new FormDefinition(); definition.setTitle("Multiform");
        FormTabDefinition main = tab("main", section("Főlap", row("mainRow", field("mainField", "Same", "Főlap mező", null))));
        FormRowDefinition attachmentRow = row("attachmentRow", field("same", "Same", "Melléklap mező", null)); attachmentRow.setRepeatable(true);
        FormTabDefinition attachments = tab("attachments", section("Melléklapok", attachmentRow));
        definition.getTabs().add(main); definition.getTabs().add(attachments);
        FormData data = data("mainField", "főlap-érték");
        data.getRowInstancesByRowId().put("attachmentRow", List.of(instance("/Doc/Form_M[1]", "same", "melléklet-1")));
        String html = render(definition, data, options());
        assertTrue(html.contains("Főlap"));
        assertTrue(html.contains("Melléklapok"));
        assertTrue(html.contains("főlap-érték"));
        assertTrue(html.contains("melléklet-1"));
    }

    @Test
    void metadataContainsSha3HashAndConfiguredApplicationVersion() throws Exception {
        PrintOptions options = options(); options.setAppVersion("9.9.9-test");
        String html = render(definitionWith(row("r", field("f", "Field_A", "Címke", null))), data("f", "érték"), options);
        assertTrue(html.contains("XML állomány SHA3-512 hash lenyomata:"));
        assertTrue(html.matches("(?s).*<[a-z]+ class=\"meta-value\">[0-9a-f]{128}</[a-z]+>.*"));
        assertTrue(html.contains("9.9.9-test"));
    }

    private String render(FormDefinition definition, FormData data, PrintOptions options) throws Exception {
        Path xml = tempDir.resolve("input.xml");
        if (!Files.exists(xml)) Files.writeString(xml, "<Doc><Value>teszt</Value></Doc>");
        SchemaBundle bundle = new SchemaBundle(); bundle.setDocumentType("TEST");
        Method method = DefaultXmlPrintService.class.getDeclaredMethod("renderHtml", FormDefinition.class, FormData.class, SchemaBundle.class, PrintOptions.class, Path.class);
        method.setAccessible(true);
        return (String) method.invoke(new DefaultXmlPrintService(), definition, data, bundle, options, xml);
    }

    private static PrintOptions options() { return new PrintOptions(); }
    private static FormDefinition definitionWith(FormRowDefinition row) {
        FormDefinition d = new FormDefinition(); d.setTitle("Teszt dokumentum"); d.getTabs().add(tab("tab", section("Szekció", row))); return d;
    }
    private static FormTabDefinition tab(String id, FormSectionDefinition section) { FormTabDefinition t = new FormTabDefinition(); t.setId(id); t.getSections().add(section); return t; }
    private static FormSectionDefinition section(String title, FormRowDefinition row) { FormSectionDefinition s = new FormSectionDefinition(); s.setTitle(title); s.getRows().add(row); return s; }
    private static FormRowDefinition row(String id, FormFieldDefinition... fields) { FormRowDefinition r = new FormRowDefinition(); r.setId(id); r.setTitle(id); r.getFields().addAll(List.of(fields)); return r; }
    private static FormFieldDefinition field(String id, String xmlName, String label, Integer width) { FormFieldDefinition f = new FormFieldDefinition(); f.setId(id); f.setXmlName(xmlName); f.setLabel(label); f.setLayoutWidth(width); return f; }
    private static FormData data(String fieldId, String raw) { FormData d = new FormData(); d.getValuesByFieldId().put(fieldId, value(fieldId, raw)); return d; }
    private static FormValue value(String fieldId, String raw) { FormValue v = new FormValue(); v.setFieldId(fieldId); v.setValue(raw); v.setPresent(true); return v; }
    private static FormRowInstance instance(String path, String fieldId, String raw) { FormRowInstance i = new FormRowInstance(); i.setXmlPath(path); i.getValuesByFieldId().put(fieldId, value(fieldId, raw)); return i; }
    private static int occurrences(String text, String needle) { int count=0, pos=0; while ((pos=text.indexOf(needle,pos))>=0) { count++; pos+=needle.length(); } return count; }
}
