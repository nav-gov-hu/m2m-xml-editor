package hu.gov.nav.xsdparsertool.multiform;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MultiformSchemaAnalyzerTest {
    @Test
    void discovers2608MainAndRepeatingPartsWhenTestXsdIsProvided() {
        String path = System.getProperty("multiform.test.xsd");
        if (path == null) return;
        MultiformDescriptor d = new MultiformSchemaAnalyzer().analyze(Path.of(path));
        assertEquals("Doc_2608", d.documentElement().getLocalPart());
        assertEquals("Form_2608A", d.mainPart().elementName().getLocalPart());
        assertEquals("Form_2608A_Type", d.mainPart().typeName().getLocalPart());
        assertEquals("Form_2608M", d.repeatingPart().elementName().getLocalPart());
        assertEquals("Form_2608M_Type", d.repeatingPart().typeName().getLocalPart());
        assertTrue(d.repeatingPart().unbounded());
    }
}
