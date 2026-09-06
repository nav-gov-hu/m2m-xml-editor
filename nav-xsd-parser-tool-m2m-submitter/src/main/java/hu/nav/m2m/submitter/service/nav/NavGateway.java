package hu.nav.m2m.submitter.service.nav;

import hu.nav.m2m.submitter.domain.CompressionType;

import java.nio.file.Path;
import java.util.List;
import hu.nav.m2m.submitter.service.M2mSignatureService;

/**
 * A NAV M2M kommunikáció technológiafüggetlen szerződése a feltöltés, bizonylat, validáció, kalkuláció és státusz műveletekhez.
 */
public interface NavGateway {
    /**
     * Feltölt egy fájlt a NAV filestore szolgáltatásába.
     *
     * @param file a feltöltendő lokális fájl
     * @param fileName a NAV felé küldött eredeti fájlnév
     * @param sha256Hex a fájl SHA-256 ellenőrzőösszege
     * @param sizeBytes a fájl mérete bájtban
     * @param messageId a NAV kérés egyedi messageId értéke
     * @param correlationId a teljes beküldési folyamat korrelációs azonosítója
     * @return a NAV által visszaadott fileId és ellenőrzési eredmény
     */
    UploadedFile uploadFile(Path file, String fileName, String sha256Hex, long sizeBytes, String messageId, String correlationId);
    /**
     * Létrehozza a bizonylatot a NAV Bizonylat API megfelelő típus/verzió útvonalán.
     *
     * @param bizonylatTipus az XML-ből feloldott bizonylattípus
     * @param bizonylatVerzio az XML-ből feloldott bizonylatverzió
     * @param payloadFile a NAV-nak küldendő előkészített XML payload
     * @param compression az alkalmazott tömörítés
     * @param attachments a korábban feltöltött csatolmányok NAV fileId adatai
     * @param signature a NAV kérés aláírása
     * @param signatureDebug az aláírás diagnosztikai részletei
     * @param operationHash a műveletspecifikus hash
     * @param validationCertificate opcionális validációs tanúsítvány
     * @param messageId a NAV kérés messageId értéke
     * @param correlationId a folyamat korrelációs azonosítója
     * @return a létrehozás NAV eredménye és ügyazonosítója
     */
    BizonylatCreateResult createBizonylat(String bizonylatTipus, String bizonylatVerzio, Path payloadFile, CompressionType compression, List<UploadedFile> attachments, String signature, M2mSignatureService.SignatureDebug signatureDebug, String operationHash, String validationCertificate, String messageId, String correlationId);
    /** Elindítja az online validációt a NAV Bizonylat API-n a megadott, aláírt payloadra. */
    ValidacioResult createValidacio(String bizonylatTipus, String bizonylatVerzio, Path payloadFile, CompressionType compression, String signature, M2mSignatureService.SignatureDebug signatureDebug, String operationHash, String messageId, String correlationId);
    /** Lekéri a korábban indított online validáció aktuális NAV eredményét. */
    ValidacioResult getValidacio(String ugyAzonosito, String messageId, String correlationId);
    /** Elindítja az online kalkulációt a NAV Bizonylat API-n a megadott, aláírt payloadra. */
    KalkulacioResult createKalkulacio(String bizonylatTipus, String bizonylatVerzio, Path payloadFile, CompressionType compression, String signature, M2mSignatureService.SignatureDebug signatureDebug, String operationHash, String messageId, String correlationId);
    /** Lekéri a korábban indított online kalkuláció aktuális NAV eredményét és az esetleges számított XML payloadot. */
    KalkulacioResult getKalkulacio(String ugyAzonosito, String messageId, String correlationId);
    /** Lekéri a NAV oldali feldolgozás aktuális státuszát a megadott NAV azonosítóhoz. */
    StatusResult getStatus(String navIdentifier, String messageId, String correlationId);

    /**
     * A NAV M2M submitter modul {@code UploadedFile} típusának felelősségét megvalósító típus.
     */
    record UploadedFile(String fileId, String resultCode, String virusScanResultCode) {}
    /**
     * A NAV M2M submitter modul {@code BizonylatCreateResult} típusának felelősségét megvalósító típus.
     */
    record BizonylatCreateResult(String ugyAzonosito, String navStatus, String erkeztetesiSzam, String resultCode,
                                  String message, String befogadasIdopontja, String megjegyzes,
                                  String validaciosHibak, String responseBody) {}
    /**
     * A NAV M2M submitter modul {@code ValidacioResult} típusának felelősségét megvalósító típus.
     */
    record ValidacioResult(String ugyAzonosito, String status, String resultCode, String message,
                            String validaciosHibak, String validaciosTanusitvany, String responseBody) {}
    /**
     * A NAV M2M submitter modul {@code KalkulacioResult} típusának felelősségét megvalósító típus.
     */
    record KalkulacioResult(String ugyAzonosito, String status, String resultCode, String message,
                             String bizonylatXmlBase64, String tomorites, String hibaKod, String hibaUzenet,
                             String mezoAzonosito, String szabalyAzonosito, String responseBody) {}
    /**
     * A NAV M2M submitter modul {@code StatusResult} típusának felelősségét megvalósító típus.
     */
    record StatusResult(String navStatus, String erkeztetesiSzam, String resultCode, String message) {}
}
