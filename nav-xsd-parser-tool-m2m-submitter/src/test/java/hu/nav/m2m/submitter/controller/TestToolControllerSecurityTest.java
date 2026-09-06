package hu.nav.m2m.submitter.controller;

import hu.nav.m2m.submitter.config.NavM2mProperties;
import hu.nav.m2m.submitter.service.M2mSignatureService;
import hu.nav.m2m.submitter.service.RuntimeSignatureKeyService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TestToolControllerSecurityTest {

    @Test
    void controllerMustRemainAdminOnly() {
        PreAuthorize preAuthorize;
        try {
            preAuthorize = TestToolController.class.getAnnotation(PreAuthorize.class);
        } catch (RuntimeException ex) {
            fail("A controller security annotációja nem olvasható: " + ex.getMessage());
            return;
        }

        assertNotNull(preAuthorize);
        assertEquals("hasRole('ADMIN')", preAuthorize.value());
    }

    @Test
    void configResponseMustExposeOnlyConfiguredFlagsForSecrets() {
        NavM2mProperties properties = new NavM2mProperties();
        properties.getAuth().setClientSecret("CLIENT_SECRET_VALUE");
        properties.getAuth().setPassword("PASSWORD_VALUE");
        properties.getSignature().setKeyFirstPart("KEY_FIRST_PART_VALUE");
        properties.getSignature().setNonce("NONCE_VALUE");
        properties.getSignature().setKeySecondPart("KEY_SECOND_PART_VALUE");

        RuntimeSignatureKeyService runtimeKeys = mock(RuntimeSignatureKeyService.class);
        when(runtimeKeys.effectiveKeySecondPart()).thenReturn("RUNTIME_KEY_VALUE");
        when(runtimeKeys.effectiveSource()).thenReturn("runtime");
        when(runtimeKeys.snapshot()).thenReturn(Map.of("runtimeKeySecondPart", "RUNTIME_KEY_VALUE"));

        TestToolController controller = new TestToolController(properties, mock(M2mSignatureService.class), runtimeKeys);
        Map<String, Object> response = controller.config();
        String serializedView = response.toString();

        assertFalse(serializedView.contains("CLIENT_SECRET_VALUE"));
        assertFalse(serializedView.contains("PASSWORD_VALUE"));
        assertFalse(serializedView.contains("KEY_FIRST_PART_VALUE"));
        assertFalse(serializedView.contains("NONCE_VALUE"));
        assertFalse(serializedView.contains("KEY_SECOND_PART_VALUE"));
        assertFalse(serializedView.contains("RUNTIME_KEY_VALUE"));
        assertEquals(Boolean.TRUE, response.get("clientSecretConfigured"));
        assertEquals(Boolean.TRUE, response.get("passwordConfigured"));
        assertEquals(Boolean.TRUE, response.get("signatureKeyConfigured"));
    }
}
