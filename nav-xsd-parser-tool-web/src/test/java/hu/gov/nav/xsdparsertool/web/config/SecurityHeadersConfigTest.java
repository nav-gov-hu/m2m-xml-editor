package hu.gov.nav.xsdparsertool.web.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class SecurityHeadersConfigTest {

    @Test
    void addsApplicationWideHstsHeader() throws Exception {
        SecurityHeadersConfig filter = new SecurityHeadersConfig();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/config");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(
                "max-age=31536000; includeSubDomains",
                response.getHeader("Strict-Transport-Security"));
    }
}
