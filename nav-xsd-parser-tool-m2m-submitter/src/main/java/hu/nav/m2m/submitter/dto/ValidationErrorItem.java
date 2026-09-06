package hu.nav.m2m.submitter.dto;

/**
 * Egy NAV online validációs hiba normalizált reprezentációja.
 */
/**
 * Létrehozza a(z) {@code ValidationErrorItem} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
 *
 * @param errorCode a művelethez átadott {@code errorCode} érték
 * @param message a művelethez átadott {@code message} érték
 * @param severity a művelethez átadott {@code severity} érték
 * @param element a művelethez átadott {@code element} érték
 * @param ruleId a művelethez átadott {@code ruleId} érték
 * @param path a feldolgozandó vagy ellenőrzendő fájlútvonal
 * @param additionalInformation a művelethez átadott {@code additionalInformation} érték
 */
public record ValidationErrorItem(
        String errorCode,
        String message,
        String severity,
        String element,
        String ruleId,
        String path,
        String additionalInformation
) {
}
