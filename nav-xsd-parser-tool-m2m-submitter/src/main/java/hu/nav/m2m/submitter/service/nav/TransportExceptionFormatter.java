package hu.nav.m2m.submitter.service.nav;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * HTTP/proxy/TLS hibák teljes ok-láncát emberileg olvasható formában adja vissza.
 */
public final class TransportExceptionFormatter {
    /**
     * Létrehozza a(z) {@code TransportExceptionFormatter} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
     */
    private TransportExceptionFormatter() {
    }

    /**
     * A technikai állapotot diagnosztikai vagy kliensoldali felhasználásra alkalmas, kontrollált szöveges reprezentációvá alakítja.
     *
     * @param throwable a művelethez átadott {@code throwable} érték
     * @return a művelet eredménye
     */
    public static String describe(Throwable throwable) {
        if (throwable == null) {
            return "Ismeretlen kommunikációs hiba";
        }
        Set<String> parts = new LinkedHashSet<>();
        Throwable current = throwable;
        int depth = 0;
        while (current != null && depth < 12) {
            String type = current.getClass().getSimpleName();
            String message = current.getMessage();
            String part = message == null || message.isBlank() ? type : type + ": " + message.trim();
            parts.add(part);
            current = current.getCause();
            depth++;
        }
        return String.join(" -> ", parts);
    }

    /**
     * A(z) {@code probableArea} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param throwable a művelethez átadott {@code throwable} érték
     * @return a művelet eredménye
     */
    public static String probableArea(Throwable throwable) {
        String text = describe(throwable);
        if (containsIgnoreCase(text, "407") || containsIgnoreCase(text, "proxy authentication")) {
            return "PROXY_AUTHENTICATION";
        }
        if (containsIgnoreCase(text, "unknownhost") || containsIgnoreCase(text, "unresolvedaddress") || containsIgnoreCase(text, "name or service not known")) {
            return "PROXY_DNS";
        }
        if (containsIgnoreCase(text, "connect timed out") || containsIgnoreCase(text, "connection timed out")) {
            return "PROXY_CONNECT_TIMEOUT";
        }
        if (containsIgnoreCase(text, "connection refused")) {
            return "PROXY_CONNECTION_REFUSED";
        }
        if (containsIgnoreCase(text, "pkix") || containsIgnoreCase(text, "sslhandshake") || containsIgnoreCase(text, "certificate")) {
            return "TLS_CERTIFICATE";
        }
        if (containsIgnoreCase(text, "read timed out") || containsIgnoreCase(text, "response timeout")) {
            return "NAV_RESPONSE_TIMEOUT";
        }
        return "HTTP_TRANSPORT";
    }

    /**
     * A(z) {@code containsIgnoreCase} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param value a feldolgozandó érték
     * @param needle a művelethez átadott {@code needle} érték
     * @return igaz, ha a vizsgált feltétel teljesül; egyébként hamis
     */
    private static boolean containsIgnoreCase(String value, String needle) {
        if (value == null || needle == null || needle.isEmpty()) {
            return false;
        }
        return java.util.regex.Pattern.compile(java.util.regex.Pattern.quote(needle),
                java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.UNICODE_CASE)
                .matcher(value).find();
    }
}
