package hu.nav.m2m.submitter.service.nav;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hu.nav.m2m.submitter.config.NavM2mProperties;
import hu.nav.m2m.submitter.dto.EventDto;
import hu.nav.m2m.submitter.service.M2mSignatureService;
import hu.nav.m2m.submitter.service.RuntimeSignatureKeyService;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A NAV regisztrációs és nonce beváltási HTTP folyamatot kezeli auditálással és válaszfeldolgozással.
 */
@Service
public class NavRegistrationService {
    private static final Logger log = LoggerFactory.getLogger(NavRegistrationService.class);
    private final NavM2mProperties properties;
    private final NavRestTemplateFactory restTemplateFactory;
    private final NavTokenService tokenService;
    private final M2mSignatureService signatureService;
    private final RuntimeSignatureKeyService runtimeSignatureKeyService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Létrehozza a(z) {@code NavRegistrationService} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
     *
     * @param properties az M2M külső konfiguráció
     * @param restTemplateFactory a művelethez átadott {@code restTemplateFactory} érték
     * @param tokenService a művelethez átadott {@code tokenService} érték
     * @param signatureService a művelethez átadott {@code signatureService} érték
     * @param runtimeSignatureKeyService a művelethez átadott {@code runtimeSignatureKeyService} érték
     */
    public NavRegistrationService(NavM2mProperties properties,
                                  NavRestTemplateFactory restTemplateFactory,
                                  NavTokenService tokenService,
                                  M2mSignatureService signatureService,
                                  RuntimeSignatureKeyService runtimeSignatureKeyService) {
        this.properties = properties;
        this.restTemplateFactory = restTemplateFactory;
        this.tokenService = tokenService;
        this.signatureService = signatureService;
        this.runtimeSignatureKeyService = runtimeSignatureKeyService;
    }

    /**
     * A kapott nonce értéket a NAV regisztrációs végponton beváltja, a válaszból kiolvassa a runtime aláírási kulcsrészt és azt a RuntimeSignatureKeyService-ben tárolja.
     *
     * @return a művelet eredménye
     */
    public Map<String, Object> redeemNonce() {
        NavHttpAuditHolder.clear();
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            String accessToken = tokenService.getAccessToken();
            String url = commonBaseUrl() + properties.getEndpoints().getNoncePath();
            String messageId = UUID.randomUUID().toString();
            String correlationId = UUID.randomUUID().toString();

            HttpHeaders headers = jsonHeaders(messageId, correlationId);
            headers.setBearerAuth(accessToken);

            Map<String, Object> requestData = new LinkedHashMap<>();
            requestData.put("nonce", properties.getSignature().getNonce());
            Map<String, Object> body = Map.of("requestData", requestData);
            String requestPayload = json(body);

            ResponseEntity<JsonNode> response = restTemplateFactory.create().exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), JsonNode.class);
            JsonNode node = response.getBody();
            audit("REDEEM_NONCE", "POST", url, headers, requestPayload,
                    NavHttpAuditFormatter.status(response), NavHttpAuditFormatter.responseHeaders(response), json(node));

            String resultCode = text(node, "resultCode", "result_code");
            String resultMessage = text(node, "resultMessage", "result_message");
            String keySecondPart = text(node, "signatureKeySecondPart", "signature_key_second_part");
            boolean success = "REDEEM_NONCE_SUCCESSFUL".equals(resultCode);
            if (success && keySecondPart != null && !keySecondPart.isBlank()) {
                runtimeSignatureKeyService.storeRedeemedSecondPart(keySecondPart);
            }
            result.put("success", success);
            result.put("message", success
                    ? "Nonce beváltás sikeres. A signatureKeySecondPart futásidőben eltárolva, és az aktiválás/beküldés ezt használja az alkalmazás újraindításáig vagy új nonce beváltásig."
                    : "Nonce beváltás nem sikerült.");
            result.put("resultCode", resultCode);
            result.put("resultMessage", resultMessage);
            result.put("signatureKeySecondPart", keySecondPart);
            result.put("runtimeSignatureKey", runtimeSignatureKeyService.snapshot());
            result.put("note", "A program nem írja vissza a külső YAML fájlt. A signatureKeySecondPart csak futásidőben van megőrizve, új nonce beváltásig vagy alkalmazás újraindításig.");
        } catch (RuntimeException e) {
            auditException("REDEEM_NONCE", "POST", commonBaseUrl() + properties.getEndpoints().getNoncePath(), null, null, e);
            result.put("success", false);
            result.put("resultCode", TransportExceptionFormatter.probableArea(e));
            result.put("resultMessage", TransportExceptionFormatter.describe(e));
            result.put("message", "NAV kommunikációs hiba: " + TransportExceptionFormatter.describe(e));
        }
        result.put("traces", drainTraceEvents());
        return result;
    }

    /**
     * A(z) {@code activateUserRegistration} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @return a művelet eredménye
     */
    public Map<String, Object> activateUserRegistration() {
        NavHttpAuditHolder.clear();
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            String accessToken = tokenService.getAccessToken();
            String url = commonBaseUrl() + properties.getEndpoints().getActivationPath();
            String messageId = UUID.randomUUID().toString();
            String correlationId = UUID.randomUUID().toString();

            M2mSignatureService.SignatureDebug debug = signatureService.createSignatureDebug(messageId, "");

            HttpHeaders headers = jsonHeaders(messageId, correlationId);
            headers.setBearerAuth(accessToken);

            Map<String, Object> requestData = new LinkedHashMap<>();
            requestData.put("signature", debug.digestBytesBase64Upper());
            Map<String, Object> body = Map.of("requestData", requestData);
            String requestPayload = json(body) + "\n\n--- ACTIVATION_SIGNATURE_DEBUG ---\n"
                    + "operationSpecificData=<empty>\n"
                    + "timestamp=" + debug.timestamp() + "\n"
                    + "messageId=" + messageId + "\n"
                    + "signatureKey=" + debug.signatureKey() + "\n"
                    + "signatureBase=" + debug.signatureBase() + "\n"
                    + "digestBytesBase64Upper=" + debug.digestBytesBase64Upper() + "\n"
                    + "warning=" + debug.signatureKeyWarning();

            ResponseEntity<JsonNode> response = restTemplateFactory.create().exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), JsonNode.class);
            JsonNode node = response.getBody();
            audit("ACTIVATE_USER_REGISTRATION", "POST", url, headers, requestPayload,
                    NavHttpAuditFormatter.status(response), NavHttpAuditFormatter.responseHeaders(response), json(node));

            String resultCode = text(node, "resultCode", "result_code");
            String resultMessage = text(node, "resultMessage", "result_message");
            result.put("success", "ACTIVATE_USER_REGISTRATION_SUCCESSFUL".equals(resultCode));
            result.put("resultCode", resultCode);
            result.put("resultMessage", resultMessage);
            result.put("signatureDebug", Map.of(
                    "messageId", messageId,
                    "timestamp", debug.timestamp(),
                    "operationSpecificData", "",
                    "signatureBase", debug.signatureBase(),
                    "sha256DigestBytesBase64", debug.digestBytesBase64(),
                    "sha256DigestBytesBase64Upper", debug.digestBytesBase64Upper(),
                    "hexStringBase64CompareOnly", debug.base64OfHexStringCompareOnly(),
                    "configuredKeySecondPart", debug.configuredKeySecondPart(),
                    "keySecondPartSource", debug.keySecondPartSource(),
                    "warning", debug.signatureKeyWarning()
            ));
        } catch (RuntimeException e) {
            auditException("ACTIVATE_USER_REGISTRATION", "POST", commonBaseUrl() + properties.getEndpoints().getActivationPath(), null, null, e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        result.put("traces", drainTraceEvents());
        return result;
    }

    /**
     * A(z) {@code jsonHeaders} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param messageId a NAV kérés egyedi messageId értéke
     * @param correlationId a művelethez átadott {@code correlationId} érték
     * @return a művelet eredménye
     */
    private HttpHeaders jsonHeaders(String messageId, String correlationId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.add("messageId", messageId);
        headers.add("correlationId", correlationId);
        headers.add("X-Message-Id", messageId);
        headers.add("X-Correlation-Id", correlationId);
        return headers;
    }

    /**
     * A(z) {@code commonBaseUrl} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @return a művelet eredménye
     */
    private String commonBaseUrl() {
        String base = properties.getEndpoints().getCommonBaseUrl();
        if (base == null || base.isBlank()) throw new IllegalStateException("NAV Common API baseUrl nincs beállítva");
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
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
        String responsePayload = e.getClass().getSimpleName() + ": " + e.getMessage();
        if (e instanceof RestClientResponseException r) {
            responseStatus = r.getRawStatusCode() + " " + r.getStatusText();
            responseHeaders = r.getResponseHeaders() == null ? null : NavHttpAuditFormatter.headers(r.getResponseHeaders());
            responsePayload = r.getResponseBodyAsString();
        }
        audit(operation, method, url, requestHeaders, requestPayload, responseStatus, responseHeaders, responsePayload);
    }

    /**
     * Kiveszi az aktuális művelethez összegyűjtött HTTP trace eseményeket és átadja a magasabb naplózási rétegnek.
     *
     * @return a művelet eredménye
     */
    private List<EventDto> drainTraceEvents() {
        Instant now = Instant.now();
        return NavHttpAuditHolder.drain().stream()
                .map(t -> new EventDto("NAV_HTTP_TRACE", t.operation(), null, t.responseStatus(),
                        t.method() + " " + t.url() + "\n" + nullToEmpty(t.requestHeaders()),
                        NavHttpAuditFormatter.limit(t.requestPayload()),
                        NavHttpAuditFormatter.limit(t.responseHeaders()),
                        NavHttpAuditFormatter.limit(t.responsePayload()),
                        NavHttpAuditFormatter.limit(t.configSnapshot()), now))
                .toList();
    }

    /**
     * A(z) {@code text} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param node a művelethez átadott {@code node} érték
     * @param names a művelethez átadott {@code names} érték
     * @return a művelet eredménye
     */
    private String text(JsonNode node, String... names) {
        if (node == null) return null;
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && !value.isNull()) return value.asText();
        }
        return null;
    }

    /**
     * A(z) {@code json} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param value a feldolgozandó érték
     * @return a művelet eredménye
     */
    private String json(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (Exception e) { return String.valueOf(value); }
    }

    /**
     * Null szöveget üres szöveggé alakít, egyébként változatlanul adja vissza az értéket.
     *
     * @param value a feldolgozandó érték
     * @return a művelet eredménye
     */
    private String nullToEmpty(String value) { return value == null ? "" : value; }
}
