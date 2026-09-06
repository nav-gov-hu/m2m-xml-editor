package hu.gov.nav.xsdparsertool.web.systemconfig.transfer;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** A teljes, hordozható rendszerkonfiguráció export/import dokumentuma. */
public record ConfigurationTransferDocument(
        String format,
        Instant exportedAt,
        String sourceDatabaseType,
        String secretKeyFingerprint,
        Map<String, String> bootstrap,
        Map<String, String> values,
        Map<String, EncryptedSecret> secrets,
        List<TrustedCertificate> trustedCertificates,
        Map<String, Map<String, String>> propertyFiles,
        Map<String, String> textFiles) {

    /** Titkosított adatbázis-secret változatlan exportreprezentációja. */
    public record EncryptedSecret(String encryptedValue, int encryptionVersion) {}

    /** Megbízható X.509 tanúsítvány teljes, DB-független exportreprezentációja. */
    public record TrustedCertificate(
            String alias,
            String subjectDn,
            String issuerDn,
            String serialNumber,
            String sha256Fingerprint,
            Instant validFrom,
            Instant validUntil,
            String sourceHost,
            Integer sourcePort,
            String status,
            String certificateDerBase64,
            Instant createdAt,
            String createdBy) {}
}
