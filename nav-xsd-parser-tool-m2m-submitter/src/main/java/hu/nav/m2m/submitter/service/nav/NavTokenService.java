package hu.nav.m2m.submitter.service.nav;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hu.nav.m2m.submitter.config.NavM2mProperties;
import hu.nav.m2m.submitter.dto.EventDto;
import hu.nav.m2m.submitter.dto.TokenTestResponse;
import hu.nav.m2m.submitter.service.nav.audit.NavHttpAuditFormatter;
import hu.nav.m2m.submitter.service.nav.audit.NavHttpAuditHolder;
import hu.nav.m2m.submitter.service.nav.audit.NavHttpAuditLogger;
import hu.nav.m2m.submitter.service.nav.audit.NavHttpTrace;
import org.springframework.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A NAV hozzáférési token lekérését és diagnosztikai tesztelését végző szolgáltatás, auditált HTTP kommunikációval.
 */
@Service
public class NavTokenService {
    private static final Logger log = LoggerFactory.getLogger(NavTokenService.class);
    private final NavM2mProperties properties;
    private final NavRestTemplateFactory restTemplateFactory;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String accessToken;
    private Instant expiresAt = Instant.EPOCH;

    /**
     * Létrehozza a(z) {@code NavTokenService} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
     *
     * @param properties az M2M külső konfiguráció
     * @param restTemplateFactory a művelethez átadott {@code restTemplateFactory} érték
     */
    public NavTokenService(NavM2mProperties properties, NavRestTemplateFactory restTemplateFactory) {
        this.properties = properties;
        this.restTemplateFactory = restTemplateFactory;
    }

    /**
     * NAV access tokent kér vagy szerez a konfigurált hitelesítési adatokkal; a HTTP kommunikációt auditálja, de a titkos tokenértéket nem teszi naplózható payload részévé.
     *
     * @return a művelet eredménye
     */
    public synchronized String getAccessToken() {
        if (accessToken != null && Instant.now().isBefore(expiresAt.minusSeconds(60))) {
            return accessToken;
        }
        return requestToken();
    }

    /**
     * A(z) {@code testToken} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @return a művelet eredménye
     */
    public synchronized TokenTestResponse testToken() {
        NavHttpAuditHolder.clear();
        try {
            String token = requestToken();
            List<EventDto> traces = drainTokenTraceEvents();
            return new TokenTestResponse(true, "NAV token kérés sikeres", "TOKEN_CREATION_SUCCESSFUL", null, token, expiresAt, traces);
        } catch (RuntimeException e) {
            List<EventDto> traces = drainTokenTraceEvents();
            String resultCode = extractResultFieldFromTraces(traces, "resultCode");
            String resultMessage = extractResultFieldFromTraces(traces, "resultMessage");
            return new TokenTestResponse(false, e.getMessage(), resultCode, resultMessage, null, null, traces);
        }
    }

    /**
     * Összeállítja és elküldi a tényleges tokenkérést, feldolgozza a NAV válaszát és egységes hibát képez a sikertelen transport vagy protokoll válaszokból.
     *
     * @return a művelet eredménye
     */
    private String requestToken() {
        String url = trim(properties.getEndpoints().getCommonBaseUrl()) + properties.getEndpoints().getTokenPath();
        String messageId = UUID.randomUUID().toString();
        String correlationId = UUID.randomUUID().toString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.add("messageId", messageId);
        headers.add("correlationId", correlationId);
        // Több NAV minta és gateway log is használ X-* aliasokat; a kötelező header továbbra is a messageId.
        headers.add("X-Message-Id", messageId);
        headers.add("X-Correlation-Id", correlationId);

        Map<String, Object> requestData = new HashMap<>();
        requestData.put("clientId", properties.getAuth().getClientId());
        requestData.put("clientSecret", properties.getAuth().getClientSecret());
        requestData.put("username", properties.getAuth().getUsername());
        requestData.put("password", properties.getAuth().getPassword());

        Map<String, Object> body = new HashMap<>();
        body.put("requestData", requestData);

        String requestPayload = tokenRequestBody();
        boolean navResponseAudited = false;
        try {
            ResponseEntity<JsonNode> response = restTemplateFactory.create().exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), JsonNode.class);
            JsonNode node = response.getBody();
            if (node == null) {
                throw new IllegalStateException("NAV token válasz üres");
            }
            audit("TOKEN_REQUEST", "POST", url, headers, requestPayload,
                    NavHttpAuditFormatter.status(response), NavHttpAuditFormatter.responseHeaders(response), tokenResponseBody(node));
            navResponseAudited = true;
            String token = firstText(node, "accessToken", "access_token", "token", "idToken");
            if (token == null || token.isBlank()) {
                String resultCode = firstText(node, "resultCode", "result_code");
                String resultMessage = firstText(node, "resultMessage", "result_message");
                throw NavOperationExceptionFactory.tokenFailure(resultCode, resultMessage);
            }
            long seconds = firstLong(node, 900L, "expires", "expiresIn", "expires_in", "expiration");
            this.accessToken = token;
            this.expiresAt = Instant.now().plusSeconds(seconds);
            return token;
        } catch (RuntimeException e) {
            if (!navResponseAudited) {
                auditException("TOKEN_REQUEST", "POST", url, headers, requestPayload, e);
            }
            throw e;
        }
    }

    /**
     * A NAV HTTP művelet request/response adataiból maszkolt audit trace eseményt készít és eltárolja.
     *
     * @param operation a NAV vagy életciklus művelet neve
     * @param method a művelethez átadott {@code method} érték
     * @param url a cél NAV végpont
     * @param requestHeaders a művelethez átadott {@code requestHeaders} érték
     * @param requestPayload a művelethez átadott {@code requestPayload} érték
     * @param responseStatus a művelethez átadott {@code responseStatus} érték
     * @param responseHeaders a művelethez átadott {@code responseHeaders} érték
     * @param responsePayload a művelethez átadott {@code responsePayload} érték
     */
    private void audit(String operation, String method, String url, HttpHeaders requestHeaders, String requestPayload, String responseStatus, String responseHeaders, String responsePayload) {
        String formattedRequestHeaders = requestHeaders == null ? null : NavHttpAuditFormatter.headers(requestHeaders);
        String formattedResponseHeaders = NavHttpAuditFormatter.limit(responseHeaders);
        NavHttpAuditLogger.trace(NavHttpAuditFormatter.fullTraceBlock(
                operation,
                method,
                url,
                formattedRequestHeaders,
                requestPayload,
                responseStatus,
                responseHeaders,
                responsePayload));
        NavHttpAuditHolder.add(new NavHttpTrace(
                operation,
                method,
                url,
                formattedRequestHeaders,
                NavHttpAuditFormatter.payloadSummary(requestPayload),
                responseStatus,
                formattedResponseHeaders,
                NavHttpAuditFormatter.payloadSummary(responsePayload),
                NavHttpAuditFormatter.configSnapshot(properties) + "\n" + restTemplateFactory.proxySnapshot()
        ));
    }

    /**
     * A NAV HTTP művelet kivételes befejezését maszkolt audit trace eseményként rögzíti.
     *
     * @param operation a NAV vagy életciklus művelet neve
     * @param method a művelethez átadott {@code method} érték
     * @param url a cél NAV végpont
     * @param requestHeaders a művelethez átadott {@code requestHeaders} érték
     * @param requestPayload a művelethez átadott {@code requestPayload} érték
     * @param e a feldolgozás közben kapott kivétel
     */
    private void auditException(String operation, String method, String url, HttpHeaders requestHeaders, String requestPayload, RuntimeException e) {
        String responseStatus = "EXCEPTION";
        String responseHeaders = null;
        String responsePayload = TransportExceptionFormatter.describe(e);
        if (e instanceof RestClientResponseException r) {
            responseStatus = r.getRawStatusCode() + " " + r.getStatusText();
            responseHeaders = r.getResponseHeaders() == null ? null : NavHttpAuditFormatter.headers(r.getResponseHeaders());
            responsePayload = r.getResponseBodyAsString();
        }
        audit(operation, method, url, requestHeaders, requestPayload, responseStatus, responseHeaders, responsePayload);
    }

    /**
     * A bemeneti domain/transport adatokból a következő feldolgozási réteg által igényelt reprezentációt állítja elő.
     *
     * @return a művelet eredménye
     */
    private String tokenRequestBody() {
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("clientId", properties.getAuth().getClientId());
        requestData.put("clientSecret", properties.getAuth().getClientSecret());
        requestData.put("username", properties.getAuth().getUsername());
        requestData.put("password", properties.getAuth().getPassword());

        Map<String, Object> body = new HashMap<>();
        body.put("requestData", requestData);
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            return body.toString();
        }
    }

    /**
     * A bemeneti domain/transport adatokból a következő feldolgozási réteg által igényelt reprezentációt állítja elő.
     *
     * @param node a művelethez átadott {@code node} érték
     * @return a művelet eredménye
     */
    private String tokenResponseBody(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return node == null ? null : node.toString();
        }
    }

    /**
     * A(z) {@code drainTokenTraceEvents} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @return a művelet eredménye
     */
    private List<EventDto> drainTokenTraceEvents() {
        Instant now = Instant.now();
        return NavHttpAuditHolder.drain().stream()
                .map(t -> new EventDto(
                        "NAV_HTTP_TRACE",
                        t.operation(),
                        null,
                        t.responseStatus(),
                        t.method() + " " + t.url() + "\n" + nullToEmpty(t.requestHeaders()),
                        NavHttpAuditFormatter.limit(t.requestPayload()),
                        NavHttpAuditFormatter.limit(t.responseHeaders()),
                        NavHttpAuditFormatter.limit(t.responsePayload()),
                        NavHttpAuditFormatter.limit(t.configSnapshot()),
                        now))
                .toList();
    }

    /**
     * A bemeneti struktúrából biztonságosan kiolvassa a művelethez szükséges értéket, és hiányzó adat esetén a metódus szerinti fallbacket alkalmazza.
     *
     * @param traces a művelethez átadott {@code traces} érték
     * @param fieldName a művelethez átadott {@code fieldName} érték
     * @return a művelet eredménye
     */
    private String extractResultFieldFromTraces(List<EventDto> traces, String fieldName) {
        if (traces == null) return null;
        for (EventDto trace : traces) {
            String payload = trace.responsePayload();
            if (payload == null || payload.isBlank()) continue;
            try {
                JsonNode node = objectMapper.readTree(payload);
                JsonNode value = node.get(fieldName);
                if (value != null && !value.isNull()) return value.asText();
            } catch (Exception ignored) {
                // A responsePayload nem JSON, például exception szöveg.
            }
        }
        return null;
    }

    /**
     * Null szöveget üres szöveggé alakít, egyébként változatlanul adja vissza az értéket.
     *
     * @param value a feldolgozandó érték
     * @return a művelet eredménye
     */
    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * A bemeneti struktúrából biztonságosan kiolvassa a művelethez szükséges értéket, és hiányzó adat esetén a metódus szerinti fallbacket alkalmazza.
     *
     * @param node a művelethez átadott {@code node} érték
     * @param names a művelethez átadott {@code names} érték
     * @return a művelet eredménye
     */
    private String firstText(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode v = node.get(name);
            if (v != null && !v.isNull()) return v.asText();
        }
        return null;
    }

    /**
     * A bemeneti struktúrából biztonságosan kiolvassa a művelethez szükséges értéket, és hiányzó adat esetén a metódus szerinti fallbacket alkalmazza.
     *
     * @param node a művelethez átadott {@code node} érték
     * @param defaultValue a művelethez átadott {@code defaultValue} érték
     * @param names a művelethez átadott {@code names} érték
     * @return a művelet eredménye
     */
    private long firstLong(JsonNode node, long defaultValue, String... names) {
        for (String name : names) {
            JsonNode v = node.get(name);
            if (v != null && v.canConvertToLong()) return v.asLong();
        }
        return defaultValue;
    }

    /**
     * A bemeneti szöveget whitespace-szempontból normalizálja, null esetén kontrollált üres értéket ad.
     *
     * @param baseUrl a művelethez átadott {@code baseUrl} érték
     * @return a művelet eredménye
     */
    private String trim(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) throw new IllegalStateException("NAV Common API baseUrl nincs beállítva");
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
