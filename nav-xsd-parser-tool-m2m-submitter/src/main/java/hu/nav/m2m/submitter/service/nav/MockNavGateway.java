package hu.nav.m2m.submitter.service.nav;

import hu.nav.m2m.submitter.domain.CompressionType;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import hu.nav.m2m.submitter.service.M2mSignatureService;
import hu.nav.m2m.submitter.service.ManagedStoragePathPolicy;
import java.util.UUID;

/**
 * Fejlesztési és tesztcélú NAV gateway implementáció, amely külső NAV kapcsolat nélkül szimulálja az M2M műveleteket.
 */
@Component("mockNavGateway")
public class MockNavGateway implements NavGateway {
    private final ManagedStoragePathPolicy storagePathPolicy;

    /**
     * Létrehozza a(z) {@code MockNavGateway} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
     *
     * @param storagePathPolicy a művelethez átadott {@code storagePathPolicy} érték
     */
    public MockNavGateway(ManagedStoragePathPolicy storagePathPolicy) {
        this.storagePathPolicy = storagePathPolicy;
    }
    /**
     * Feltölti vagy feltöltésre előkészíti a megadott fájlt a NAV filestore irányába, és az eredményt a beküldési állapothoz kapcsolja.
     *
     * @param file a feldolgozandó fájl
     * @param fileName a művelethez átadott {@code fileName} érték
     * @param sha256Hex a művelethez átadott {@code sha256Hex} érték
     * @param sizeBytes a művelethez átadott {@code sizeBytes} érték
     * @param messageId a NAV kérés egyedi messageId értéke
     * @param correlationId a művelethez átadott {@code correlationId} érték
     * @return a művelet eredménye
     */
    @Override
    public UploadedFile uploadFile(Path file, String fileName, String sha256Hex, long sizeBytes, String messageId, String correlationId) {
        return new UploadedFile("MOCK-FILE-" + UUID.randomUUID(), "OK", "VIRUS_SCAN_OK");
    }


    /**
     * Előkészíti vagy létrehozza az adott NAV M2M művelethez szükséges adatot, majd a következő feldolgozási lépésnek adja tovább.
     *
     * @param bizonylatTipus a művelethez átadott {@code bizonylatTipus} érték
     * @param bizonylatVerzio a művelethez átadott {@code bizonylatVerzio} érték
     * @param payloadFile a művelethez átadott {@code payloadFile} érték
     * @param compression a művelethez átadott {@code compression} érték
     * @param attachments a feldolgozandó csatolmányok
     * @param signature a művelethez átadott {@code signature} érték
     * @param signatureDebug a művelethez átadott {@code signatureDebug} érték
     * @param operationHash a művelethez átadott {@code operationHash} érték
     * @param validationCertificate a művelethez átadott {@code validationCertificate} érték
     * @param messageId a NAV kérés egyedi messageId értéke
     * @param correlationId a művelethez átadott {@code correlationId} érték
     * @return a művelet eredménye
     */
    @Override
    public BizonylatCreateResult createBizonylat(String bizonylatTipus, String bizonylatVerzio, Path payloadFile, CompressionType compression, List<UploadedFile> attachments, String signature, M2mSignatureService.SignatureDebug signatureDebug, String operationHash, String validationCertificate, String messageId, String correlationId) {
        return new BizonylatCreateResult("MOCK-UGY-" + shortId(), "SIKERES", "MOCK-ERK-" + shortId(), "SIKERES",
                "Mock Bizonylat API beküldés sikeres", java.time.Instant.now().toString(), null, null,
                "{\"resultCode\":\"SIKERES\",\"bizonylatStatusz\":\"SIKERES\"}");
    }

    /**
     * Előkészíti vagy létrehozza az adott NAV M2M művelethez szükséges adatot, majd a következő feldolgozási lépésnek adja tovább.
     *
     * @param bizonylatTipus a művelethez átadott {@code bizonylatTipus} érték
     * @param bizonylatVerzio a művelethez átadott {@code bizonylatVerzio} érték
     * @param payloadFile a művelethez átadott {@code payloadFile} érték
     * @param compression a művelethez átadott {@code compression} érték
     * @param signature a művelethez átadott {@code signature} érték
     * @param signatureDebug a művelethez átadott {@code signatureDebug} érték
     * @param operationHash a művelethez átadott {@code operationHash} érték
     * @param messageId a NAV kérés egyedi messageId értéke
     * @param correlationId a művelethez átadott {@code correlationId} érték
     * @return a művelet eredménye
     */
    @Override
    public ValidacioResult createValidacio(String bizonylatTipus, String bizonylatVerzio, Path payloadFile, CompressionType compression, String signature, M2mSignatureService.SignatureDebug signatureDebug, String operationHash, String messageId, String correlationId) {
        String id = "MOCK-VALIDACIO-" + shortId();
        return new ValidacioResult(id, "SIKERES", "SIKERES", "Mock online validáció sikeres", null,
                "MOCK-TANUSITVANY-" + shortId(),
                "{\"ugyAzonosito\":\"" + id + "\",\"bizonylatValidacioStatusz\":\"SIKERES\",\"resultCode\":\"SIKERES\"}");
    }

    /**
     * Visszaadja a(z) validacio aktuális értékét.
     *
     * @param ugyAzonosito a művelethez átadott {@code ugyAzonosito} érték
     * @param messageId a NAV kérés egyedi messageId értéke
     * @param correlationId a művelethez átadott {@code correlationId} érték
     * @return a művelet eredménye
     */
    @Override
    public ValidacioResult getValidacio(String ugyAzonosito, String messageId, String correlationId) {
        return new ValidacioResult(ugyAzonosito, "SIKERES", "SIKERES", "Mock validáció státusz sikeres", null,
                "MOCK-TANUSITVANY-" + shortId(),
                "{\"ugyAzonosito\":\"" + ugyAzonosito + "\",\"bizonylatValidacioStatusz\":\"SIKERES\",\"resultCode\":\"SIKERES\"}");
    }

    /**
     * Előkészíti vagy létrehozza az adott NAV M2M művelethez szükséges adatot, majd a következő feldolgozási lépésnek adja tovább.
     *
     * @param bizonylatTipus a művelethez átadott {@code bizonylatTipus} érték
     * @param bizonylatVerzio a művelethez átadott {@code bizonylatVerzio} érték
     * @param payloadFile a művelethez átadott {@code payloadFile} érték
     * @param compression a művelethez átadott {@code compression} érték
     * @param signature a művelethez átadott {@code signature} érték
     * @param signatureDebug a művelethez átadott {@code signatureDebug} érték
     * @param operationHash a művelethez átadott {@code operationHash} érték
     * @param messageId a NAV kérés egyedi messageId értéke
     * @param correlationId a művelethez átadott {@code correlationId} érték
     * @return a művelet eredménye
     */
    @Override
    public KalkulacioResult createKalkulacio(String bizonylatTipus, String bizonylatVerzio, Path payloadFile, CompressionType compression, String signature, M2mSignatureService.SignatureDebug signatureDebug, String operationHash, String messageId, String correlationId) {
        String id = "MOCK-KALKULACIO-" + shortId();
        String xmlBase64 = readBase64(payloadFile);
        String tomorites = compression == CompressionType.GZIP ? "GZIP" : null;
        return new KalkulacioResult(id, "SIKERES", "SIKERES", "Mock online kalkuláció sikeres", xmlBase64, tomorites,
                null, null, null, null,
                "{\"ugyAzonosito\":\"" + id + "\",\"bizonylatKalkulacioStatusz\":\"SIKERES\",\"resultCode\":\"SIKERES\"}");
    }

    /**
     * Visszaadja a(z) kalkulacio aktuális értékét.
     *
     * @param ugyAzonosito a művelethez átadott {@code ugyAzonosito} érték
     * @param messageId a NAV kérés egyedi messageId értéke
     * @param correlationId a művelethez átadott {@code correlationId} érték
     * @return a művelet eredménye
     */
    @Override
    public KalkulacioResult getKalkulacio(String ugyAzonosito, String messageId, String correlationId) {
        return new KalkulacioResult(ugyAzonosito, "FOLYAMATBAN", "SIKERES", "Mock kalkuláció még folyamatban", null, null,
                null, null, null, null,
                "{\"ugyAzonosito\":\"" + ugyAzonosito + "\",\"bizonylatKalkulacioStatusz\":\"FOLYAMATBAN\",\"resultCode\":\"SIKERES\"}");
    }

    /**
     * A bemeneti struktúrából biztonságosan kiolvassa a művelethez szükséges értéket, és hiányzó adat esetén a metódus szerinti fallbacket alkalmazza.
     *
     * @param payloadFile a művelethez átadott {@code payloadFile} érték
     * @return a művelet eredménye
     */
    private String readBase64(Path payloadFile) {
        try {
            return java.util.Base64.getEncoder().encodeToString(storagePathPolicy.readAllBytes(payloadFile.toString()));
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Mock kalkulációs XML beolvasása sikertelen", e);
        }
    }

    /**
     * Visszaadja a(z) beküldési állapot aktuális értékét.
     *
     * @param navIdentifier a művelethez átadott {@code navIdentifier} érték
     * @param messageId a NAV kérés egyedi messageId értéke
     * @param correlationId a művelethez átadott {@code correlationId} érték
     * @return a művelet eredménye
     */
    @Override
    public StatusResult getStatus(String navIdentifier, String messageId, String correlationId) {
        return new StatusResult("SIKERESEN_BEKULDVE", "MOCK-ERK-" + shortId(), "OK", "Mock Bizonylat státusz lekérdezés");
    }

    /**
     * A(z) {@code shortId} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @return a művelet eredménye
     */
    private String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
