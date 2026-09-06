package hu.nav.m2m.submitter.service.nav.audit;

/**
 * Egy NAV HTTP kérés/válasz auditálható, maszkolt trace eseményét reprezentáló értékobjektum.
 */
/**
 * Létrehozza a(z) {@code NavHttpTrace} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
 *
 * @param operation a NAV vagy életciklus művelet neve
 * @param method a művelethez átadott {@code method} érték
 * @param url a cél NAV végpont
 * @param requestHeaders a művelethez átadott {@code requestHeaders} érték
 * @param requestPayload a művelethez átadott {@code requestPayload} érték
 * @param responseStatus a művelethez átadott {@code responseStatus} érték
 * @param responseHeaders a művelethez átadott {@code responseHeaders} érték
 * @param responsePayload a művelethez átadott {@code responsePayload} érték
 * @param configSnapshot a művelethez átadott {@code configSnapshot} érték
 */
public record NavHttpTrace(
        String operation,
        String method,
        String url,
        String requestHeaders,
        String requestPayload,
        String responseStatus,
        String responseHeaders,
        String responsePayload,
        String configSnapshot
) {}
