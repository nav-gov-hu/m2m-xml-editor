package hu.gov.nav.xsdparsertool.web.systemconfig.service;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class ConfigurationCatalogAuthenticationRemovalTest {

    @Test
    void activeDirectoryAndHybridAuthenticationOptionsAreNotExposed() {
        assertFalse(ConfigurationCatalog.ITEMS.stream().anyMatch(spec ->
                spec.key().equals("nav.xsdparsertool.security.authentication-mode")
                        || spec.key().startsWith("nav.xsdparsertool.security.active-directory.")
                        || spec.key().startsWith("nav.xsdparsertool.ad-role.")));
    }
}
