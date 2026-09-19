package hu.gov.nav.xsdparsertool.web.form;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateInputFrontendContractTest {

    private static String rendererSource;
    private static String xmlEditorSource;

    @BeforeAll
    static void loadSources() throws Exception {
        rendererSource = normalizeLineEndings(new ClassPathResource("static/js/form/form-renderer-runtime.js")
                .getContentAsString(StandardCharsets.UTF_8));
        xmlEditorSource = normalizeLineEndings(new ClassPathResource("static/js/xml/xml-editor-runtime.js")
                .getContentAsString(StandardCharsets.UTF_8));
    }

    private static String normalizeLineEndings(String source) {
        return source.replace("\r\n", "\n").replace('\r', '\n');
    }

    @Test
    void dateFieldUsesXsdYearMonthDayOrderInUiAndXml() {
        assertTrue(rendererSource.contains("input.placeholder = 'éééé-hh-nn'"));
        assertTrue(rendererSource.contains("input.dataset.dateDisplayFormat = 'yyyy-mm-dd'"));
        assertTrue(xmlEditorSource.contains("input.dataset?.dateDisplayFormat === 'yyyy-mm-dd'"));
        assertFalse(rendererSource.contains("yyyy-dd-mm"));
    }

    @Test
    void dateConversionDoesNotSwapMonthAndDay() {
        assertTrue(rendererSource.contains("function uiDateToIsoDate(value){\n  return isoDateToUiDate(value);"));
        assertTrue(rendererSource.contains("if(m) return `${m[1]}-${m[2]}-${m[3]}`;"));
        assertFalse(rendererSource.contains("`${match[1]}-${match[3]}-${match[2]}`"));
    }
}
