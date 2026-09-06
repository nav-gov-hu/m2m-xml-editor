package hu.nav.m2m.submitter.service.nav;

import hu.nav.m2m.submitter.domain.ProxySettings;
import org.apache.hc.client5.http.config.RequestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

class NavRestTemplateFactorySecurityTest {

    private final NavRestTemplateFactory factory = new NavRestTemplateFactory(() -> new ProxySettings());

    @Test
    void disabledTlsVerificationMustBeRejected() {
        ProxySettings settings = new ProxySettings();
        settings.setSslVerificationDisabled(true);

        assertThrows(IllegalStateException.class, () -> factory.create(settings));
    }

    @Test
    void invalidProxyConfigurationMustFailClosed() {
        ProxySettings settings = new ProxySettings();
        settings.setEnabled(true);
        settings.setProxyUrl("proxy.internal");
        settings.setProxyPort(70000);

        assertThrows(IllegalStateException.class, () -> factory.create(settings));
    }

    @Test
    void defaultClientMustUseHardenedApacheRequestFactory() {
        RestTemplate restTemplate = factory.create(new ProxySettings());

        assertInstanceOf(HttpComponentsClientHttpRequestFactory.class, restTemplate.getRequestFactory());
    }

    @Test
    void requestTimeoutsMustRemainExplicitlyBounded() {
        RequestConfig config = factory.requestConfig(new ProxySettings()).build();

        assertEquals(10_000L, config.getConnectTimeout().toMilliseconds());
        assertEquals(10_000L, config.getConnectionRequestTimeout().toMilliseconds());
        assertEquals(60_000L, config.getResponseTimeout().toMilliseconds());
    }
}
