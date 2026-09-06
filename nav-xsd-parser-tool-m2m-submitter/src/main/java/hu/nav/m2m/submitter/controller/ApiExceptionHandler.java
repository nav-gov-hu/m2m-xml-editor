package hu.nav.m2m.submitter.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Az M2M REST vezérlőkben keletkező kivételeket egységes HTTP hibaválasszá alakítja.
 *
 * <p>Az advice kizárólag az M2M submitter controller csomagra érvényes. Ez megakadályozza,
 * hogy más modulok, például a {@code text/event-stream} SSE végpontok hibáit JSON válasszá
 * próbálja alakítani.</p>
 */
@RestControllerAdvice(basePackages = "hu.nav.m2m.submitter.controller")
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /**
     * A felhasználói kérésből vagy az aktuális M2M állapotból következő, ismert hibákat kezeli.
     *
     * @param exception a feldolgozás közben kapott ismert kivétel
     * @param request az aktuális HTTP kérés
     * @return egységes 400-as hibaválasz
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, Object>> handleClientError(RuntimeException exception,
                                                                  HttpServletRequest request) {
        String errorId = UUID.randomUUID().toString();
        LOGGER.warn(
                "M2M API kérés elutasítva. errorId={}, method={}, uri={}, exception={}, message={}",
                errorId,
                request.getMethod(),
                request.getRequestURI(),
                exception.getClass().getSimpleName(),
                safeMessage(exception)
        );
        return errorResponse(HttpStatus.BAD_REQUEST, errorId, exception);
    }

    /**
     * A nem várt M2M REST hibákat kezeli és teljes stack trace-szel naplózza.
     *
     * @param exception a nem várt kivétel
     * @param request az aktuális HTTP kérés
     * @return egységes 500-as hibaválasz
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpectedError(Exception exception,
                                                                      HttpServletRequest request) {
        String errorId = UUID.randomUUID().toString();
        LOGGER.error(
                "Nem várt M2M API hiba. errorId={}, method={}, uri={}, exception={}, message={}",
                errorId,
                request.getMethod(),
                request.getRequestURI(),
                exception.getClass().getSimpleName(),
                safeMessage(exception),
                exception
        );
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, errorId, exception);
    }

    private ResponseEntity<Map<String, Object>> errorResponse(HttpStatus status,
                                                               String errorId,
                                                               Exception exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("errorId", errorId);
        body.put("error", exception.getClass().getSimpleName());
        body.put("message", safeMessage(exception));
        return ResponseEntity.status(status).body(body);
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "Ismeretlen hiba" : message;
    }
}
