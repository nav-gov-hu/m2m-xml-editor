package hu.nav.m2m.submitter.dto;

import java.util.List;

/**
 * A NAV online validáció strukturált hibalistáját és technikai kiegészítő adatait összefogó válasz.
 */
/**
 * Létrehozza a(z) {@code ValidationErrorDetailsResponse} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
 *
 * @param validationId a művelethez átadott {@code validationId} érték
 * @param validationStatus a művelethez átadott {@code validationStatus} érték
 * @param resultCode a NAV eredménykód
 * @param resultMessage a művelethez átadott {@code resultMessage} érték
 * @param startedAt a művelethez átadott {@code startedAt} érték
 * @param finishedAt a művelethez átadott {@code finishedAt} érték
 * @param lastCheckedAt a művelethez átadott {@code lastCheckedAt} érték
 * @param messageId a NAV kérés egyedi messageId értéke
 * @param correlationId a művelethez átadott {@code correlationId} érték
 * @param errorCount a művelethez átadott {@code errorCount} érték
 * @param errors a művelethez átadott {@code errors} érték
 */
public record ValidationErrorDetailsResponse(
        String validationId,
        String validationStatus,
        String resultCode,
        String resultMessage,
        String startedAt,
        String finishedAt,
        String lastCheckedAt,
        String messageId,
        String correlationId,
        int errorCount,
        List<ValidationErrorItem> errors
) {
}
