package hu.gov.nav.xsdparsertool.web.form;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ChainRepeatFrontendContractTest {

    private static String source;

    @BeforeAll
    static void loadSource() throws Exception {
        ClassPathResource resource = new ClassPathResource("static/js/form/form-renderer-runtime.js");
        source = resource.getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void addingVirtualRepeatOccurrenceRehydratesMaterializedValuesFromCurrentXmlBeforeRender() {
        int addFunction = source.indexOf("function addVirtualRepeatOccurrence");
        int rebuildCall = source.indexOf("rebuildCurrentFormDataFromXmlForRepeatRender();", addFunction);
        int renderCall = source.indexOf("renderForm(currentFormDefinition, currentFormData", rebuildCall);

        assertTrue(addFunction >= 0);
        assertTrue(rebuildCall > addFunction);
        assertTrue(renderCall > rebuildCall);
    }

    @Test
    void deletingVirtualRepeatOccurrenceAlsoRehydratesMaterializedValuesBeforeRender() {
        int deleteFunction = source.indexOf("function deleteRepeatOccurrence");
        int elseBranch = source.indexOf("}else{", deleteFunction);
        int rebuildCall = source.indexOf("rebuildCurrentFormDataFromXmlForRepeatRender();", elseBranch);
        int renderCall = source.indexOf("renderForm(currentFormDefinition, currentFormData", rebuildCall);

        assertTrue(deleteFunction >= 0);
        assertTrue(elseBranch > deleteFunction);
        assertTrue(rebuildCall > elseBranch);
        assertTrue(renderCall > rebuildCall);
    }
}
