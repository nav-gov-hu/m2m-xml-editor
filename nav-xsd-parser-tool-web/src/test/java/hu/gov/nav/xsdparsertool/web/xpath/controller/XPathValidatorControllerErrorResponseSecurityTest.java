package hu.gov.nav.xsdparsertool.web.xpath.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class XPathValidatorControllerErrorResponseSecurityTest {

    @Test
    void genericExceptionMustNotExposeTechnicalExceptionMessage() {
        XPathValidatorController controller = new XPathValidatorController(null, null);
        String sensitiveTechnicalMessage = "java.nio.file.AccessDeniedException: C:\\secret\\config.properties";

        ResponseEntity<String> response = controller.generic(new RuntimeException(sensitiveTechnicalMessage));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Váratlan belső hiba történt az XPath validáció során.", response.getBody());
        assertFalse(response.getBody().contains("AccessDeniedException"));
        assertFalse(response.getBody().contains("C:\\secret"));
    }
    @Test
    void missingXPathRuleReturnsSafeUserMessage() {
        XPathValidatorController controller = new XPathValidatorController(null, null);
        var ex = new hu.gov.nav.xsdparsertool.web.xpath.service.XPathValidationOrchestratorService.MissingXPathRuleException(
                "Az ellenőrzés sikertelen: Az Űrlap állományhoz nincs XPath állomány regisztrálva a rendszerbe.");

        ResponseEntity<String> response = controller.missingXPathRule(ex);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertEquals("Az ellenőrzés sikertelen: Az Űrlap állományhoz nincs XPath állomány regisztrálva a rendszerbe.", response.getBody());
    }

}
