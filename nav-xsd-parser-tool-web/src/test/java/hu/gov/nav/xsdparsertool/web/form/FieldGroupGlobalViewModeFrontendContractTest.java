package hu.gov.nav.xsdparsertool.web.form;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldGroupGlobalViewModeFrontendContractTest {

    private static String rendererSource;
    private static String shellSource;
    private static String formHtml;

    @BeforeAll
    static void loadSources() throws Exception {
        rendererSource = new ClassPathResource("static/js/form/form-renderer-runtime.js")
                .getContentAsString(StandardCharsets.UTF_8);
        shellSource = new ClassPathResource("static/js/runtime/application-shell.js")
                .getContentAsString(StandardCharsets.UTF_8);
        formHtml = new ClassPathResource("static/form.html")
                .getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void fieldGroupViewModeIsPersistedInCookie() {
        assertTrue(rendererSource.contains("m2m.fieldgroup.viewMode"));
        assertTrue(rendererSource.contains("Max-Age=${FIELDGROUP_VIEW_MODE_COOKIE_MAX_AGE_SECONDS}"));
        assertTrue(rendererSource.contains("SameSite=Lax"));
    }

    @Test
    void viewMenuOffersAllGlobalFieldGroupModes() {
        assertTrue(formHtml.contains("name=\"fieldGroupViewMode\" value=\"auto\""));
        assertTrue(formHtml.contains("name=\"fieldGroupViewMode\" value=\"grid\""));
        assertTrue(formHtml.contains("name=\"fieldGroupViewMode\" value=\"table\""));
    }

    @Test
    void renderingUsesGlobalModeBeforeAutomaticHeuristic() {
        int function = rendererSource.indexOf("function shouldRenderUiModelRowAsTable");
        int grid = rendererSource.indexOf("currentFieldGroupViewMode === 'grid'", function);
        int table = rendererSource.indexOf("currentFieldGroupViewMode === 'table'", function);
        int automatic = rendererSource.indexOf("return isUiModelTableBlock(row);", function);
        int render = rendererSource.indexOf("if (shouldRenderUiModelRowAsTable(row))");

        assertTrue(function >= 0);
        assertTrue(grid > function);
        assertTrue(table > grid);
        assertTrue(automatic > table);
        assertTrue(render > automatic);
    }

    @Test
    void changingGlobalModeRerendersCurrentForm() {
        int handler = shellSource.indexOf("input[name=\"fieldGroupViewMode\"]");
        int persist = shellSource.indexOf("setFieldGroupViewMode", handler);
        int rerender = shellSource.indexOf("renderForm(currentFormDefinition, currentFormData", persist);

        assertTrue(handler >= 0);
        assertTrue(persist > handler);
        assertTrue(rerender > persist);
    }
    @Test
    void gridColumnCountIsPersistedInCookieAndOfferedGlobally() {
        assertTrue(rendererSource.contains("m2m.fieldgroup.gridColumns"));
        assertTrue(formHtml.contains("name=\"fieldGroupGridColumns\" value=\"1\""));
        assertTrue(formHtml.contains("name=\"fieldGroupGridColumns\" value=\"2\""));
        assertTrue(formHtml.contains("name=\"fieldGroupGridColumns\" value=\"3\""));
        assertTrue(formHtml.contains("name=\"fieldGroupGridColumns\" value=\"4\""));
        assertTrue(rendererSource.contains("function fieldGroupGridWidth(field, type)"));
        assertTrue(rendererSource.contains("return 12 / currentFieldGroupGridColumns;"));
    }

    @Test
    void changingGridColumnCountRerendersCurrentForm() {
        int handler = shellSource.indexOf("input[name=\"fieldGroupGridColumns\"]");
        int persist = shellSource.indexOf("setFieldGroupGridColumns", handler);
        int rerender = shellSource.indexOf("renderForm(currentFormDefinition, currentFormData", persist);

        assertTrue(handler >= 0);
        assertTrue(persist > handler);
        assertTrue(rerender > persist);
    }

}
