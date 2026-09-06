package hu.nav.m2m.submitter.dto;

import java.time.Instant;
import java.util.List;

/**
 * A NAV tokenkapcsolat diagnosztikai ellenőrzésének válasza.
 */
/**
 * Létrehozza a(z) {@code TokenTestResponse} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
 *
 * @param success a művelethez átadott {@code success} érték
 * @param message a művelethez átadott {@code message} érték
 * @param resultCode a NAV eredménykód
 * @param resultMessage a művelethez átadott {@code resultMessage} érték
 * @param accessToken a művelethez átadott {@code accessToken} érték
 * @param expiresAt a művelethez átadott {@code expiresAt} érték
 * @param traces a művelethez átadott {@code traces} érték
 */
public record TokenTestResponse(
        boolean success,
        String message,
        String resultCode,
        String resultMessage,
        String accessToken,
        Instant expiresAt,
        List<EventDto> traces
) {}
