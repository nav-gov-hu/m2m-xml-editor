package hu.nav.m2m.submitter.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import hu.nav.m2m.submitter.config.NavM2mProperties;
import hu.nav.m2m.submitter.domain.ProxySettings;
import hu.nav.m2m.submitter.dto.ProxyTestRequest;
import hu.nav.m2m.submitter.dto.ProxyTestResponse;
import hu.nav.m2m.submitter.service.nav.NavProxySettingsProvider;
import hu.nav.m2m.submitter.service.nav.NavRestTemplateFactory;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

class ProxyConnectionTestServiceSecurityTest {

    @Test
    void proxyTestUsesConfiguredM2mEndpointInsteadOfRequestControlledTarget() {
        NavProxySettingsProvider settingsProvider = mock(NavProxySettingsProvider.class);
        NavRestTemplateFactory restTemplateFactory = mock(NavRestTemplateFactory.class);
        RestTemplate restTemplate = mock(RestTemplate.class);
        ProxySettings current = new ProxySettings();
        when(settingsProvider.getSettings()).thenReturn(current);
        when(restTemplateFactory.create(org.mockito.ArgumentMatchers.any(ProxySettings.class))).thenReturn(restTemplate);
        when(restTemplate.exchange(eq("https://m2m.example.test/rest-api/1.1"), eq(HttpMethod.GET), eq(null), eq(String.class)))
                .thenReturn(new ResponseEntity<>("ok", HttpStatus.OK));

        NavM2mProperties properties = new NavM2mProperties();
        properties.getEndpoints().setCommonBaseUrl("https://m2m.example.test/rest-api/1.1");
        ProxyConnectionTestService service = new ProxyConnectionTestService(settingsProvider, restTemplateFactory, properties);

        ProxyTestResponse response = service.test(new ProxyTestRequest());

        assertTrue(response.isSuccess());
        assertEquals("https://m2m.example.test/rest-api/1.1", response.getTestUrl());
        verify(restTemplate).exchange(eq("https://m2m.example.test/rest-api/1.1"), eq(HttpMethod.GET), eq(null), eq(String.class));
    }
}
