package hu.nav.m2m.submitter.service.nav.audit;

import hu.nav.m2m.submitter.config.NavM2mProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * A NAV HTTP auditadatok, headerek, konfigurációs snapshotok és payload-részletek maszkolt szöveges formázását végzi.
 */
public final class NavHttpAuditFormatter {
    public static final int LIMIT = 4000;

    /**
     * Létrehozza a(z) {@code NavHttpAuditFormatter} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
     */
    private NavHttpAuditFormatter() {}

    /**
     * A(z) {@code limit} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param value a feldolgozandó érték
     * @return a művelet eredménye
     */
    public static String limit(String value) {
        if (value == null) return null;
        if (value.length() <= LIMIT) return value;
        return value.substring(0, LIMIT) + "\n...[levágva, eredeti hossz: " + value.length() + " karakter]";
    }

    /**
     * A(z) {@code headers} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param headers a HTTP headerek
     * @return a művelet eredménye
     */
    public static String headers(HttpHeaders headers) {
        if (headers == null || headers.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder();
        headers.forEach((key, values) -> {
            sb.append(key).append(": ");
            StringJoiner joiner = new StringJoiner(", ");
            for (String value : values) {
                joiner.add(maskHeaderValue(key, value));
            }
            sb.append(joiner).append('\n');
        });
        return limit(sb.toString().trim());
    }

    /**
     * A(z) {@code responseHeaders} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param response a NAV HTTP válasz
     * @return a művelet eredménye
     */
    public static String responseHeaders(ResponseEntity<?> response) {
        return response == null ? null : headers(response.getHeaders());
    }

    /**
     * A technikai állapotot diagnosztikai vagy kliensoldali felhasználásra alkalmas, kontrollált szöveges reprezentációvá alakítja.
     *
     * @param response a NAV HTTP válasz
     * @return a művelet eredménye
     */
    public static String status(ResponseEntity<?> response) {
        if (response == null) return null;
        return response.getStatusCode().value() + " " + response.getStatusCode();
    }

    /**
     * A hálózati diagnosztikához használható konfigurációs összefoglalót készít a titkos értékek felfedése nélkül.
     *
     * @param properties az M2M külső konfiguráció
     * @return a művelet eredménye
     */
    public static String configSnapshot(NavM2mProperties properties) {
        if (properties == null) return "{}";
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("storageDirectory", properties.getStorageDirectory());
        m.put("maxInMemoryBizonylatApiBytes", properties.getMaxInMemoryBizonylatApiBytes());
        m.put("commonBaseUrl", properties.getEndpoints().getCommonBaseUrl());
        m.put("bizonylatBaseUrl", properties.getEndpoints().getBizonylatBaseUrl());
        m.put("tokenPath", properties.getEndpoints().getTokenPath());
        m.put("fileUploadPath", properties.getEndpoints().getFileUploadPath());
        m.put("fileStatusPath", properties.getEndpoints().getFileStatusPath());
        m.put("bizonylatPath", properties.getEndpoints().getBizonylatPath());
        m.put("clientId", properties.getAuth().getClientId());
        m.put("clientSecretConfigured", isConfigured(properties.getAuth().getClientSecret()));
        m.put("username", properties.getAuth().getUsername());
        m.put("passwordConfigured", isConfigured(properties.getAuth().getPassword()));
        m.put("signatureKeyFirstPartConfigured", isConfigured(properties.getSignature().getKeyFirstPart()));
        m.put("nonceConfigured", isConfigured(properties.getSignature().getNonce()));
        m.put("signatureKeySecondPartConfigured", isConfigured(properties.getSignature().getKeySecondPart()));
        m.put("signatureKeyConfigured", properties.getSignature().getKeySecondPart() != null && !properties.getSignature().getKeySecondPart().isBlank());
        m.put("nonceUsedAsKeySecondPart", properties.getSignature().getNonce() != null && properties.getSignature().getNonce().equals(properties.getSignature().getKeySecondPart()));
        m.put("testTaxNumber", properties.getTaxpayer().getTestTaxNumber());
        m.put("realTaxNumber", properties.getTaxpayer().getRealTaxNumber());
        StringBuilder sb = new StringBuilder();
        m.forEach((k, v) -> sb.append(k).append("=").append(v == null ? "" : v).append('\n'));
        return limit(sb.toString().trim());
    }


    /**
     * A(z) {@code fullTraceBlock} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param operation a NAV vagy életciklus művelet neve
     * @param method a művelethez átadott {@code method} érték
     * @param url a cél NAV végpont
     * @param requestHeaders a művelethez átadott {@code requestHeaders} érték
     * @param requestPayload a művelethez átadott {@code requestPayload} érték
     * @param responseStatus a művelethez átadott {@code responseStatus} érték
     * @param responseHeaders a művelethez átadott {@code responseHeaders} érték
     * @param responsePayload a művelethez átadott {@code responsePayload} érték
     * @return a művelet eredménye
     */
    public static String fullTraceBlock(String operation, String method, String url, String requestHeaders, String requestPayload, String responseStatus, String responseHeaders, String responsePayload) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n==================== NAV M2M HTTP TRACE START ====================\n");
        sb.append("OPERATION: ").append(nullToEmpty(operation)).append('\n');
        sb.append("REQUEST: ").append(nullToEmpty(method)).append(' ').append(nullToEmpty(url)).append('\n');
        sb.append("\n--- REQUEST HEADERS ---\n").append(nullToEmpty(requestHeaders)).append('\n');
        sb.append("\n--- REQUEST BODY ---\n").append(payloadSummary(requestPayload)).append('\n');
        sb.append("\n--- RESPONSE STATUS ---\n").append(nullToEmpty(responseStatus)).append('\n');
        sb.append("\n--- RESPONSE HEADERS ---\n").append(nullToEmpty(responseHeaders)).append('\n');
        sb.append("\n--- RESPONSE BODY ---\n").append(payloadSummary(responsePayload)).append('\n');
        sb.append("===================== NAV M2M HTTP TRACE END =====================");
        return sb.toString();
    }

    /**
     * Null szöveget üres szöveggé alakít, egyébként változatlanul adja vissza az értéket.
     *
     * @param value a feldolgozandó érték
     * @return a művelet eredménye
     */
    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * A technikai állapotot diagnosztikai vagy kliensoldali felhasználásra alkalmas, kontrollált szöveges reprezentációvá alakítja.
     *
     * @param value a feldolgozandó érték
     * @return a művelet eredménye
     */
    public static String payloadSummary(String value) {
        if (value == null) return "";
        return "[payload elrejtve, hossz=" + value.length() + "]";
    }

    /**
     * A titkos vagy érzékeny HTTP header értékét maszkolja, hogy auditnaplóban ne jelenjen meg teljes token vagy credential.
     *
     * @param headerName a művelethez átadott {@code headerName} érték
     * @param value a feldolgozandó érték
     * @return a művelet eredménye
     */
    public static String maskHeaderValue(String headerName, String value) {
        if (value == null) return null;
        String name = headerName == null ? "" : headerName;
        if (java.util.regex.Pattern.compile("authorization|cookie|token|secret|api-key|proxy-authorization",
                java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.UNICODE_CASE).matcher(name).find()) {
            return "****";
        }
        return maskMiddle(value);
    }

    /**
     * A(z) {@code maskMiddle} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param value a feldolgozandó érték
     * @return a művelet eredménye
     */
    public static String maskMiddle(String value) {
        if (value == null || value.isBlank()) return value;
        if (value.length() <= 8) return "****";
        return value.substring(0, 3) + "****" + value.substring(value.length() - 3);
    }

    /**
     * A jelenlegi állapot és az M2M életciklusszabályok alapján eldönti, hogy a vizsgált feltétel teljesül-e.
     *
     * @param value a feldolgozandó érték
     * @return igaz, ha a vizsgált feltétel teljesül; egyébként hamis
     */
    private static boolean isConfigured(String value) {
        return value != null && !value.isBlank();
    }
}
