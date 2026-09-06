package hu.nav.m2m.submitter.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void knownClientErrorReturnsBadRequestWithErrorId() {
        MockHttpServletRequest request = request("POST", "/api/m2m/submissions");

        ResponseEntity<Map<String, Object>> response = handler.handleClientError(
                new IllegalArgumentException("Érvénytelen kérés."),
                request
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().get("status"));
        assertEquals("IllegalArgumentException", response.getBody().get("error"));
        assertEquals("Érvénytelen kérés.", response.getBody().get("message"));
        assertNotNull(response.getBody().get("errorId"));
    }

    @Test
    void unexpectedErrorReturnsInternalServerErrorWithErrorId() {
        MockHttpServletRequest request = request("GET", "/api/m2m/submissions/1");

        ResponseEntity<Map<String, Object>> response = handler.handleUnexpectedError(
                new RuntimeException("Váratlan hiba."),
                request
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().get("status"));
        assertEquals("RuntimeException", response.getBody().get("error"));
        assertEquals("Váratlan hiba.", response.getBody().get("message"));
        assertNotNull(response.getBody().get("errorId"));
    }

    private MockHttpServletRequest request(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRequestURI(uri);
        return request;
    }
}
