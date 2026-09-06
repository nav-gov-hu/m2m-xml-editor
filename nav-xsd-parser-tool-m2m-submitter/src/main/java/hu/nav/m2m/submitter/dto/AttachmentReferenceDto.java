package hu.nav.m2m.submitter.dto;

/**
 * Az XML-ben felismert csatolmányhivatkozás REST reprezentációja.
 */
/**
 * Létrehozza a(z) {@code AttachmentReferenceDto} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
 *
 * @param sequenceNo a művelethez átadott {@code sequenceNo} érték
 * @param elementName a művelethez átadott {@code elementName} érték
 * @param fileId a művelethez átadott {@code fileId} érték
 * @param fileName a művelethez átadott {@code fileName} érték
 * @param fileSize a művelethez átadott {@code fileSize} érték
 */
public record AttachmentReferenceDto(
        int sequenceNo,
        String elementName,
        String fileId,
        String fileName,
        Long fileSize
) {}
