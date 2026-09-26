package hu.gov.nav.xsdparsertool.web.githubupdater.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class ArtifactCatalogParserTest {

    private final ArtifactCatalogParser parser = new ArtifactCatalogParser();

    @Test
    void parsesReleaseMetadataFromCatalogXml() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <artifactCatalog xmlns="https://nav.gov.hu/schema/artifact-catalog/v1">
                  <baseInformation>
                    <catalogSchemaVersion>1.0</catalogSchemaVersion>
                    <generatedAt>2026-09-04T11:52:00+02:00</generatedAt>
                  </baseInformation>
                  <formsInformation>
                    <form>
                      <formId>NAV_PMT25</formId>
                      <formName>PMT25 teszt</formName>
                      <versions>
                        <version>
                          <formVersion>0.25.1</formVersion>
                          <validFrom>2025-06-20T00:00:00+02:00</validFrom>
                          <validTo></validTo>
                          <disabled>false</disabled>
                        </version>
                      </versions>
                    </form>
                  </formsInformation>
                </artifactCatalog>
                """;

        ArtifactCatalogParser.ArtifactCatalog catalog = parser.parse(xml);

        assertEquals("1.0", catalog.schemaVersion());
        assertEquals(Instant.parse("2026-09-04T09:52:00Z"), catalog.generatedAt());
        assertEquals(1, catalog.forms().size());
        ArtifactCatalogParser.FormEntry form = catalog.forms().get(0);
        assertEquals("NAV_PMT25", form.formId());
        assertEquals("PMT25 teszt", form.formName());
        assertEquals(1, form.versions().size());
        ArtifactCatalogParser.VersionEntry version = form.versions().get(0);
        assertEquals("0.25.1", version.formVersion());
        assertEquals(Instant.parse("2025-06-19T22:00:00Z"), version.validFrom());
        assertNull(version.validTo());
        assertFalse(version.disabled());
    }
}
