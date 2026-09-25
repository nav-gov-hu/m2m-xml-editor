package hu.gov.nav.xsdparsertool.web.githubproxy;

import hu.gov.nav.xsdparsertool.web.githubupdater.domain.GitHubProxySettings;
import hu.gov.nav.xsdparsertool.web.network.SystemTableM2mProxySettingsService;
import hu.nav.m2m.submitter.domain.ProxySettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SystemTableGitHubNetworkSettingsProviderTest {

    @Test
    void githubClientUsesGeneralNetworkProxySettings() {
        SystemTableM2mProxySettingsService generalProxy = mock(SystemTableM2mProxySettingsService.class);
        ProxySettings source = new ProxySettings();
        source.setEnabled(true);
        source.setProxyUrl("proxy.example.local");
        source.setProxyPort(3128);
        source.setUsername("proxy-user");
        source.setPassword("proxy-password");
        source.setTrustStorePath("/opt/app/truststore.p12");
        source.setTrustStoreType("PKCS12");
        source.setTrustStorePassword("trust-password");
        when(generalProxy.getEntity()).thenReturn(source);

        GitHubProxySettings actual = new SystemTableGitHubNetworkSettingsProvider(generalProxy).load();

        assertTrue(actual.isEnabled());
        assertEquals("proxy.example.local", actual.getProxyUrl());
        assertEquals(3128, actual.getProxyPort());
        assertEquals("proxy-user", actual.getUsername());
        assertEquals("proxy-password", actual.getPassword());
        assertEquals("/opt/app/truststore.p12", actual.getTrustStorePath());
        assertEquals("PKCS12", actual.getTrustStoreType());
        assertEquals("trust-password", actual.getTrustStorePassword());
    }
}
