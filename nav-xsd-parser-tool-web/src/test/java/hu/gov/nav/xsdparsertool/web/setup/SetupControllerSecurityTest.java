package hu.gov.nav.xsdparsertool.web.setup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class SetupControllerSecurityTest {

    @Test
    void completeRejectsNonLoopbackRequestBeforeWritingConfiguration() throws Exception {
        SetupStateService state = mock(SetupStateService.class);
        SetupService service = mock(SetupService.class);
        ApplicationRestartService restartService = mock(ApplicationRestartService.class);
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getRemoteAddr()).thenReturn("192.0.2.15");

        SetupController controller = new SetupController(state, service, restartService);
        SetupRequest request = new SetupRequest("/tmp/nav-data", "STANDALONE", "admin", "Admin", null, "secret", "secret", null, null, null, null);

        ResponseEntity<?> response = controller.complete(request, httpRequest);

        assertEquals(403, response.getStatusCode().value());
        verify(service, never()).completeSelected(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), isNull(), anyString(), anyString(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(true));
    }

    @Test
    void completeAllowsIpv4LoopbackRequest() throws Exception {
        SetupStateService state = mock(SetupStateService.class);
        SetupService service = mock(SetupService.class);
        ApplicationRestartService restartService = mock(ApplicationRestartService.class);
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        SetupRequest request = new SetupRequest("/tmp/nav-data", "STANDALONE", "admin", "Admin", null, "secret", "secret", null, null, null, null);
        SetupResult setupResult = new SetupResult(true, false, false, "COMPLETED", "ok");
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(service.completeSelected(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), isNull(), anyString(), anyString(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(true)))
                .thenReturn(setupResult);

        SetupController controller = new SetupController(state, service, restartService);
        ResponseEntity<?> response = controller.complete(request, httpRequest);

        assertEquals(200, response.getStatusCode().value());
        verify(service).completeSelected(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), isNull(), anyString(), anyString(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(true));
    }

    @Test
    void completeAllowsIpv6LoopbackRequest() throws Exception {
        SetupStateService state = mock(SetupStateService.class);
        SetupService service = mock(SetupService.class);
        ApplicationRestartService restartService = mock(ApplicationRestartService.class);
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        SetupRequest request = new SetupRequest("/tmp/nav-data", "STANDALONE", "admin", "Admin", null, "secret", "secret", null, null, null, null);
        SetupResult setupResult = new SetupResult(true, false, false, "COMPLETED", "ok");
        when(httpRequest.getRemoteAddr()).thenReturn("::1");
        when(service.completeSelected(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), isNull(), anyString(), anyString(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(true)))
                .thenReturn(setupResult);

        SetupController controller = new SetupController(state, service, restartService);
        ResponseEntity<?> response = controller.complete(request, httpRequest);

        assertEquals(200, response.getStatusCode().value());
        verify(service).completeSelected(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), isNull(), anyString(), anyString(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(true));
    }
    @Test
    void completeExplainsManualRestartWhenAutomaticRestartIsUnavailable() throws Exception {
        SetupStateService state = mock(SetupStateService.class);
        SetupService service = mock(SetupService.class);
        ApplicationRestartService restartService = mock(ApplicationRestartService.class);
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        SetupRequest request = new SetupRequest("/tmp/nav-data", "STANDALONE", "admin", "Admin", null, "secret", "secret", null, null, null, null);
        SetupResult setupResult = new SetupResult(true, true, false, "COMPLETED", "restart");
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(service.completeSelected(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), isNull(), anyString(), anyString(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(true)))
                .thenReturn(setupResult);
        when(restartService.scheduleRestart()).thenReturn(false);

        SetupController controller = new SetupController(state, service, restartService);
        ResponseEntity<?> response = controller.complete(request, httpRequest);

        assertEquals(200, response.getStatusCode().value());
        SetupResult result = (SetupResult) response.getBody();
        assertEquals(false, result.restartScheduled());
        assertTrue(result.message().contains("IDE-ből vagy mvn spring-boot:run"));
        assertTrue(result.message().contains("indítsa újra kézzel"));
    }

    @Test
    void completeKeepsAutomaticRestartMessageWhenRestartWasScheduled() throws Exception {
        SetupStateService state = mock(SetupStateService.class);
        SetupService service = mock(SetupService.class);
        ApplicationRestartService restartService = mock(ApplicationRestartService.class);
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        SetupRequest request = new SetupRequest("/tmp/nav-data", "STANDALONE", "admin", "Admin", null, "secret", "secret", null, null, null, null);
        SetupResult setupResult = new SetupResult(true, true, false, "COMPLETED", "restart");
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(service.completeSelected(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), isNull(), anyString(), anyString(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(true)))
                .thenReturn(setupResult);
        when(restartService.scheduleRestart()).thenReturn(true);

        SetupController controller = new SetupController(state, service, restartService);
        ResponseEntity<?> response = controller.complete(request, httpRequest);

        SetupResult result = (SetupResult) response.getBody();
        assertTrue(result.restartScheduled());
        assertEquals("A beállítások mentése sikeres. Az alkalmazás automatikusan újraindul.", result.message());
    }

}
