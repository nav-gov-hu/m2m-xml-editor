package hu.gov.nav.xsdparsertool.web.setup;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SetupServiceApiKeyParsingTest {

    @Test
    void parsesFourPartM2mApiKey() {
        SetupService.M2mApiKeyParts parts = SetupService.parseM2mApiKey("user123-secret456-signingPart-nonce789");

        assertEquals("user123", parts.userId());
        assertEquals("secret456", parts.password());
        assertEquals("signingPart", parts.signatureKeyFirstPart());
        assertEquals("nonce789", parts.nonce());
    }

    @Test
    void rejectsApiKeyWithMissingOrExtraParts() {
        assertThrows(IllegalArgumentException.class, () -> SetupService.parseM2mApiKey("user-password-key"));
        assertThrows(IllegalArgumentException.class, () -> SetupService.parseM2mApiKey("user-password-key-nonce-extra"));
        assertThrows(IllegalArgumentException.class, () -> SetupService.parseM2mApiKey("user--key-nonce"));
    }
}
