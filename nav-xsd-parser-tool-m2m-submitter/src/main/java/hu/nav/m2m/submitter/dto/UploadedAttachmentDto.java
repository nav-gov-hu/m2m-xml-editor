package hu.nav.m2m.submitter.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * A feltöltött M2M csatolmány kliensnek átadott reprezentációja.
 */
/**
 * Létrehozza a(z) {@code UploadedAttachmentDto} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
 *
 * @param id a művelethez átadott {@code id} érték
 * @param originalFileName a művelethez átadott {@code originalFileName} érték
 * @param fileSize a művelethez átadott {@code fileSize} érték
 * @param sha256Hex a művelethez átadott {@code sha256Hex} érték
 * @param navFileId a művelethez átadott {@code navFileId} érték
 * @param xmlReferencePresent a művelethez átadott {@code xmlReferencePresent} érték
 * @param navUploadedAt a művelethez átadott {@code navUploadedAt} érték
 * @param navExpiresAt a művelethez átadott {@code navExpiresAt} érték
 * @param navLastRefreshedAt a művelethez átadott {@code navLastRefreshedAt} érték
 * @param navUploadResultCode a művelethez átadott {@code navUploadResultCode} érték
 * @param navUploadResultMessage a művelethez átadott {@code navUploadResultMessage} érték
 * @param lifecycleState a művelethez átadott {@code lifecycleState} érték
 * @param lifecycleLabel a művelethez átadott {@code lifecycleLabel} érték
 * @param refreshAllowed a művelethez átadott {@code refreshAllowed} érték
 * @param lifecycleReason a művelethez átadott {@code lifecycleReason} érték
 * @param localFileAvailable a művelethez átadott {@code localFileAvailable} érték
 */
public record UploadedAttachmentDto(
        UUID id, String originalFileName, Long fileSize, String sha256Hex, String navFileId,
        boolean xmlReferencePresent, Instant navUploadedAt, Instant navExpiresAt, Instant navLastRefreshedAt,
        String navUploadResultCode, String navUploadResultMessage,
        String lifecycleState, String lifecycleLabel, boolean refreshAllowed,
        String lifecycleReason, boolean localFileAvailable
) {}
