package hu.gov.nav.xsdparsertool.web.xpath.service;

import net.sf.saxon.s9api.SaxonApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;

class XsltValidationServiceSecurityTest {

    @TempDir Path tempDir;

    @Test
    void externalHttpStylesheetIncludeMustBeRejected() throws Exception {
        Path xsl = tempDir.resolve("main.xsl");
        Files.writeString(xsl, """
                <?xml version="1.0" encoding="UTF-8"?>
                <xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
                  <xsl:include href="https://127.0.0.1:9/evil.xsl"/>
                  <xsl:template match="/"><result/></xsl:template>
                </xsl:stylesheet>
                """);
        Path xml = tempDir.resolve("input.xml");
        Files.writeString(xml, "<root/>");

        assertThrows(SaxonApiException.class,
                () -> new XsltValidationService().validate(xsl, xml, null, null, null, null, null));
    }

    @Test
    void stylesheetIncludeMayNotEscapeAllowedDirectory() throws Exception {
        Path rulesDir = Files.createDirectory(tempDir.resolve("rules"));
        Path outside = tempDir.resolve("outside.xsl");
        Files.writeString(outside, """
                <xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"/>
                """);
        Path xsl = rulesDir.resolve("main.xsl");
        Files.writeString(xsl, """
                <?xml version="1.0" encoding="UTF-8"?>
                <xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
                  <xsl:include href="../outside.xsl"/>
                  <xsl:template match="/"><result/></xsl:template>
                </xsl:stylesheet>
                """);
        Path xml = tempDir.resolve("input.xml");
        Files.writeString(xml, "<root/>");

        assertThrows(SaxonApiException.class,
                () -> new XsltValidationService().validate(xsl, xml, null, null, null, null, null));
    }

    @Test
    void localStylesheetIncludeInsideAllowedDirectoryIsAccepted() throws Exception {
        Path helper = tempDir.resolve("helper.xsl");
        Files.writeString(helper, """
                <xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
                  <xsl:template name="emit"><result>ok</result></xsl:template>
                </xsl:stylesheet>
                """);
        Path xsl = tempDir.resolve("main.xsl");
        Files.writeString(xsl, """
                <xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
                  <xsl:include href="helper.xsl"/>
                  <xsl:template match="/"><xsl:call-template name="emit"/></xsl:template>
                </xsl:stylesheet>
                """);
        Path xml = tempDir.resolve("input.xml");
        Files.writeString(xml, "<root/>");

        var result = new XsltValidationService().validate(xsl, xml, null, null, null, null, null);

        org.junit.jupiter.api.Assertions.assertTrue(result.rawOutputXml().contains("<result>ok</result>"));
        org.junit.jupiter.api.Assertions.assertTrue(result.issues().isEmpty());
    }
}
