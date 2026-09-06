package hu.nav.m2m.submitter.service.nav.audit;

import hu.nav.m2m.submitter.config.NavM2mProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.junit.jupiter.api.Assertions.*;

class NavHttpAuditFormatterSecurityTest {

    @Test
    void authorizationAndTokenHeadersMustBeFullyMasked() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer secret-token-value");
        headers.add("X-Api-Key", "very-secret-api-key");

        String formatted = NavHttpAuditFormatter.headers(headers);

        assertFalse(formatted.contains("secret-token-value"));
        assertFalse(formatted.contains("very-secret-api-key"));
        assertTrue(formatted.contains("****"));
    }

    @Test
    void configSnapshotMustNeverContainCredentialValues() {
        NavM2mProperties properties = new NavM2mProperties();
        properties.getAuth().setClientSecret("CLIENT_SECRET_VALUE");
        properties.getAuth().setPassword("PASSWORD_VALUE");
        properties.getSignature().setKeyFirstPart("KEY_FIRST_PART_VALUE");
        properties.getSignature().setNonce("NONCE_VALUE");
        properties.getSignature().setKeySecondPart("KEY_SECOND_PART_VALUE");

        String snapshot = NavHttpAuditFormatter.configSnapshot(properties);

        assertFalse(snapshot.contains("CLIENT_SECRET_VALUE"));
        assertFalse(snapshot.contains("PASSWORD_VALUE"));
        assertFalse(snapshot.contains("KEY_FIRST_PART_VALUE"));
        assertFalse(snapshot.contains("NONCE_VALUE"));
        assertFalse(snapshot.contains("KEY_SECOND_PART_VALUE"));
        assertTrue(snapshot.contains("clientSecretConfigured=true"));
        assertTrue(snapshot.contains("passwordConfigured=true"));
    }

    @Test
    void fullTraceMustHideRequestAndResponsePayloads() {
        String trace = NavHttpAuditFormatter.fullTraceBlock(
                "submit", "POST", "https://example.invalid", "headers",
                "REQUEST_SECRET_PAYLOAD", "200 OK", "headers", "RESPONSE_SECRET_PAYLOAD");

        assertFalse(trace.contains("REQUEST_SECRET_PAYLOAD"));
        assertFalse(trace.contains("RESPONSE_SECRET_PAYLOAD"));
        assertTrue(trace.contains("payload elrejtve"));
    }
}
