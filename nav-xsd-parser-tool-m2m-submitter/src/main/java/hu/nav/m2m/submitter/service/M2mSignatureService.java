package hu.nav.m2m.submitter.service;

import hu.nav.m2m.submitter.config.NavM2mProperties;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

/**
 * A NAV M2M kérésaláíráshoz szükséges időbélyeges SHA-256 alapú aláírások és diagnosztikai részletek előállítója.
 */
@Service
public class M2mSignatureService {
    private static final DateTimeFormatter NAV_UTC_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);

    private final NavM2mProperties properties;
    private final RuntimeSignatureKeyService runtimeSignatureKeyService;

    /**
     * Létrehozza a(z) {@code M2mSignatureService} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
     *
     * @param properties az M2M külső konfiguráció
     * @param runtimeSignatureKeyService a művelethez átadott {@code runtimeSignatureKeyService} érték
     */
    public M2mSignatureService(NavM2mProperties properties, RuntimeSignatureKeyService runtimeSignatureKeyService) {
        this.properties = properties;
        this.runtimeSignatureKeyService = runtimeSignatureKeyService;
    }

    /**
     * NAV altalanos interfeszleiras 5.1.1. Alairas szerint:
     * base64(SHA-256(messageId + timestamp + operationSpecificValue + signatureKey)), majd nagybetusites.
     * A timestamp UTC ido, YYYYMMDDHHmmss formatumban.
     */
    public String createSignature(String messageId, String operationSpecificValue) {
        return createSignatureDebug(messageId, operationSpecificValue).signatureDigestBytesBase64Upper();
    }

    /**
     * Tesztelheto / dokumentacios ellenorzeshez hasznalhato valtozat, ahol a timestamp fixen megadhato.
     */
    public String createSignatureWithTimestamp(String messageId, String timestamp, String operationSpecificValue) {
        String keyFirstPart = safe(properties.getSignature().getKeyFirstPart());
        String keySecondPart = safe(runtimeSignatureKeyService.effectiveKeySecondPart());
        String signatureBase = safe(messageId) + safe(timestamp) + safe(operationSpecificValue) + keyFirstPart + keySecondPart;
        return digest(signatureBase).digestBytesBase64Upper();
    }


    /**
     * Reszletes debug informacio a request loghoz.
     * A signature* mezok a tenylegesen kikuldott, dokumentacio szerinti alairast mutatjak.
     * Az old* mezok csak osszehasonlitasra maradtak bent, mert a korabbi verzio ezt a hibas sorrendet hasznalta.
     */
    public SignatureDebug createSignatureDebug(String messageId, String operationSpecificValue) {
        String timestamp = NAV_UTC_TIMESTAMP_FORMAT.format(Instant.now());
        String keyFirstPart = safe(properties.getSignature().getKeyFirstPart());
        String nonce = safe(properties.getSignature().getNonce());
        String configuredKeySecondPart = safe(properties.getSignature().getKeySecondPart());
        String keySecondPart = safe(runtimeSignatureKeyService.effectiveKeySecondPart());
        String keySecondPartSource = runtimeSignatureKeyService.effectiveSource();
        String signatureKey = keyFirstPart + keySecondPart;
        boolean nonceUsedAsKeySecondPart = !nonce.isBlank() && nonce.equals(keySecondPart);
        String signatureKeyWarning = keySecondPart.isBlank()
                ? "MISSING_SIGNATURE_KEY_SECOND_PART: nincs runtime vagy konfiguralt key-second-part. Nonce beváltás szükséges."
                : (nonceUsedAsKeySecondPart
                    ? "NONCE_USED_AS_KEY_SECOND_PART: az effektív key-second-part megegyezik a nonce értékkel, ez hibás aláírást okozhat."
                    : "OK");

        String signatureBase = safe(messageId)
                + timestamp
                + safe(operationSpecificValue)
                + signatureKey;
        DigestResult signatureDigest = digest(signatureBase);

        String oldBase = keyFirstPart
                + safe(messageId)
                + safe(operationSpecificValue)
                + timestamp
                + keySecondPart;
        DigestResult oldDigest = digest(oldBase);

        return new SignatureDebug(
                timestamp,
                keyFirstPart,
                nonce,
                keySecondPart,
                configuredKeySecondPart,
                keySecondPartSource,
                signatureKey,
                nonceUsedAsKeySecondPart,
                signatureKeyWarning,
                signatureBase,
                signatureDigest.hexLower(),
                signatureDigest.hexUpper(),
                signatureDigest.digestBytesBase64(),
                signatureDigest.digestBytesBase64Upper(),
                signatureDigest.base64OfHexLowerText(),
                signatureDigest.base64OfHexLowerTextUpper(),
                oldBase,
                oldDigest.hexLower(),
                oldDigest.hexUpper(),
                oldDigest.digestBytesBase64(),
                oldDigest.digestBytesBase64Upper(),
                oldDigest.base64OfHexLowerText(),
                oldDigest.base64OfHexLowerTextUpper()
        );
    }

    /**
     * A(z) {@code sha256Base64Upper} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param text a hash- vagy szövegfeldolgozás bemenete
     * @return a kiszámított ellenőrzőösszeg
     */
    public String sha256Base64Upper(String text) {
        return digest(text).digestBytesBase64Upper();
    }

    /**
     * SHA-256 hash-t számít a megadott tartalomból, és kisbetűs hexadecimális szövegként adja vissza.
     *
     * @param text a hash- vagy szövegfeldolgozás bemenete
     * @return a kiszámított ellenőrzőösszeg
     */
    public String sha256Hex(String text) {
        return digest(text).hexLower();
    }

    /**
     * NAV altalanos interfeszleiras 5.1.1 es a dokumentacios pelda szerinti algoritmus:
     * 1) signatureBase UTF-8 byte-jain SHA-256 digest keszul.
     * 2) A SHA-256 digest byte tombot kozvetlenul Base64 kodoljuk.
     * 3) A Base64 eredmenyt nagybetusitjuk.
     *
     * Dokumentacios ellenorzo pelda:
     * base=7eae9ecf-f735-4a4f-aa49-e85ea411a3132024051012384726549118-0ddc-4e30-81bc-eaddd6f54b21FA12BC4567CA12BC4588
     * expected=LOHXJMDUZR4ETMOA9Y6XMLGHWCRR3/OJQ6T6JBDKQOG=
     *
     * Fontos: a logban tovabbra is kiirjuk a SHA-256 hex string Base64 erteket is
     * osszehasonlitasra, de a kikuldott signature a digestBytesBase64Upper.
     */
    private DigestResult digest(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] digestBytes = digest.digest(safe(text).getBytes(StandardCharsets.UTF_8));
            String hexLower = HexFormat.of().formatHex(digestBytes);
            String hexUpper = hexLower.toUpperCase(Locale.ROOT);
            String digestBytesBase64 = Base64.getEncoder().encodeToString(digestBytes);
            String digestBytesBase64Upper = digestBytesBase64.toUpperCase(Locale.ROOT);
            String base64OfHexLowerText = Base64.getEncoder().encodeToString(hexLower.getBytes(StandardCharsets.UTF_8));
            String base64OfHexLowerTextUpper = base64OfHexLowerText.toUpperCase(Locale.ROOT);
            return new DigestResult(hexLower, hexUpper, digestBytesBase64, digestBytesBase64Upper, base64OfHexLowerText, base64OfHexLowerTextUpper);
        } catch (Exception e) {
            throw new IllegalStateException("Hash/alairas kepzes sikertelen", e);
        }
    }

    /**
     * Null értéket biztonságos üres szövegre normalizál a további feldolgozás előtt.
     *
     * @param value a feldolgozandó érték
     * @return a művelet eredménye
     */
    private String safe(String value) {
        return value == null ? "" : value;
    }

    /**
     * A NAV M2M submitter modul {@code DigestResult} típusának felelősségét megvalósító típus.
     */
    /**
     * Létrehozza a(z) {@code DigestResult} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
     *
     * @param hexLower a művelethez átadott {@code hexLower} érték
     * @param hexUpper a művelethez átadott {@code hexUpper} érték
     * @param digestBytesBase64 a művelethez átadott {@code digestBytesBase64} érték
     * @param digestBytesBase64Upper a művelethez átadott {@code digestBytesBase64Upper} érték
     * @param base64OfHexLowerText a művelethez átadott {@code base64OfHexLowerText} érték
     * @param base64OfHexLowerTextUpper a művelethez átadott {@code base64OfHexLowerTextUpper} érték
     */
    private record DigestResult(
            String hexLower,
            String hexUpper,
            String digestBytesBase64,
            String digestBytesBase64Upper,
            String base64OfHexLowerText,
            String base64OfHexLowerTextUpper
    ) {}

    /**
     * A NAV M2M submitter modul {@code SignatureDebug} típusának felelősségét megvalósító típus.
     */
    public record SignatureDebug(
            String timestamp,
            String keyFirstPart,
            String nonce,
            String keySecondPart,
            String configuredKeySecondPart,
            String keySecondPartSource,
            String signatureKey,
            boolean nonceUsedAsKeySecondPart,
            String signatureKeyWarning,
            String signatureBase,
            String signatureSha256HexLower,
            String signatureSha256HexUpper,
            String signatureDigestBytesBase64,
            String signatureDigestBytesBase64Upper,
            String signatureBase64OfHex,
            String signatureBase64OfHexUpper,
            String oldBase,
            String oldSha256HexLower,
            String oldSha256HexUpper,
            String oldDigestBytesBase64,
            String oldDigestBytesBase64Upper,
            String oldBase64OfHex,
            String oldBase64OfHexUpper
    ) {
        /**
         * A(z) {@code Base64} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
         *
         * @param bytes a feldolgozandó bájttömb
         */
        /** Kikuldendo signature: Base64(SHA-256 digest bytes).toUpperCase(). */
        public String signatureBase64Upper() { return signatureDigestBytesBase64Upper; }
        /**
         * A(z) {@code signatureBase64} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
         *
         * @return a művelet eredménye
         */
        public String signatureBase64() { return signatureDigestBytesBase64; }
        /**
         * A(z) {@code oldBase64Upper} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
         *
         * @return a művelet eredménye
         */
        public String oldBase64Upper() { return oldBase64OfHexUpper; }
        /**
         * A(z) {@code oldBase64} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
         *
         * @return a művelet eredménye
         */
        public String oldBase64() { return oldBase64OfHex; }

        /**
         * A(z) {@code Base64} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
         *
         * @param bytes a feldolgozandó bájttömb
         */
        /** Kikuldott NAV-kompatibilis ertek: kozvetlen Base64(SHA-256 digest bytes). */
        public String digestBytesBase64() { return signatureDigestBytesBase64; }
        /**
         * A(z) {@code digestBytesBase64Upper} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
         *
         * @return a művelet eredménye
         */
        public String digestBytesBase64Upper() { return signatureDigestBytesBase64Upper; }
        /**
         * A(z) {@code oldDigestBytesBase64Value} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
         *
         * @return a művelet eredménye
         */
        public String oldDigestBytesBase64Value() { return oldDigestBytesBase64; }
        /**
         * A(z) {@code oldDigestBytesBase64UpperValue} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
         *
         * @return a művelet eredménye
         */
        public String oldDigestBytesBase64UpperValue() { return oldDigestBytesBase64Upper; }

        /**
         * A(z) {@code Base64} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
         *
         * @param karakterei a művelethez átadott {@code karakterei} érték
         */
        /** Osszehasonlito ertek: Base64(SHA-256 hex string karakterei), nem ezt kuldjuk. */
        public String base64OfHexStringCompareOnly() { return signatureBase64OfHex; }
        /**
         * A(z) {@code base64OfHexStringCompareOnlyUpper} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
         *
         * @return a művelet eredménye
         */
        public String base64OfHexStringCompareOnlyUpper() { return signatureBase64OfHexUpper; }

        /** Visszafele kompatibilis elnevezesek a korabbi debug kodhoz. */
        public String wrongBase64OfHexLowerText() { return signatureBase64OfHex; }
        /**
         * A(z) {@code oldWrongBase64OfHexLowerText} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
         *
         * @return a művelet eredménye
         */
        public String oldWrongBase64OfHexLowerText() { return oldBase64OfHex; }
    }
}
