package hu.nav.m2m.submitter.dto;

import java.time.Instant;

/**
 * Az M2M eseménynapló kliensnek átadott reprezentációja.
 */
/**
 * Létrehozza a(z) {@code EventDto} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
 *
 * @param eventType a rögzítendő esemény típusa
 * @param navOperation a művelethez átadott {@code navOperation} érték
 * @param requestMessageId a művelethez átadott {@code requestMessageId} érték
 * @param responseCode a művelethez átadott {@code responseCode} érték
 * @param requestHeaders a művelethez átadott {@code requestHeaders} érték
 * @param requestPayload a művelethez átadott {@code requestPayload} érték
 * @param responseHeaders a művelethez átadott {@code responseHeaders} érték
 * @param responsePayload a művelethez átadott {@code responsePayload} érték
 * @param configSnapshot a művelethez átadott {@code configSnapshot} érték
 * @param createdAt a művelethez átadott {@code createdAt} érték
 */
public record EventDto(
        String eventType,
        String navOperation,
        String requestMessageId,
        String responseCode,
        String requestHeaders,
        String requestPayload,
        String responseHeaders,
        String responsePayload,
        String configSnapshot,
        Instant createdAt
) {}
