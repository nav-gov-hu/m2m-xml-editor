package hu.gov.nav.xsdparsertool.web.form;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldGroupVisibilityFrontendContractTest {

    private static String source;

    @BeforeAll
    static void loadSource() throws Exception {
        ClassPathResource resource = new ClassPathResource("static/js/form/form-renderer-runtime.js");
        source = resource.getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void uiModelFieldGroupRemainsVisibleWhenMissingFieldsAreFilteredOut() {
        int helper = source.indexOf("function uiModelRowHasStructuralFields");
        int renderFunction = source.indexOf("function renderUiModelSectionContent");
        int structuralCheck = source.indexOf("if(!uiModelRowHasStructuralFields(row)) continue;", renderFunction);
        int appendGroup = source.indexOf("target.appendChild(group);", structuralCheck);

        assertTrue(helper >= 0);
        assertTrue(renderFunction >= 0);
        assertTrue(structuralCheck > renderFunction);
        assertTrue(appendGroup > structuralCheck);
    }

    @Test
    void sectionVisibilityUsesDefinitionInsteadOfMissingFieldFilter() {
        int function = source.indexOf("function uiModelSectionHasRenderableFields");
        int structuralCheck = source.indexOf("if(uiModelRowHasStructuralFields(row)) return true;", function);

        assertTrue(function >= 0);
        assertTrue(structuralCheck > function);
    }

    @Test
    void tableLikeFieldGroupRemainsVisibleWhenAllRowsAreMissingFromXml() {
        int function = source.indexOf("function renderUiModelTableBlock");
        int structuralFields = source.indexOf("const structuralFields = allFields.filter", function);
        int emptyVisibleRows = source.indexOf("if(!fields.length) return block;", function);
        int obsoleteEarlyReturn = source.indexOf("if(!fields.length) return null;", function);

        assertTrue(function >= 0);
        assertTrue(structuralFields > function);
        assertTrue(emptyVisibleRows > structuralFields);
        assertTrue(obsoleteEarlyReturn < 0 || obsoleteEarlyReturn < function);
    }
}
