package hu.nav.m2m.submitter.service.nav;

import com.fasterxml.jackson.core.Base64Variants;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hu.nav.m2m.submitter.config.NavM2mProperties;
import hu.nav.m2m.submitter.domain.CompressionType;
import hu.nav.m2m.submitter.service.M2mSignatureService;
import hu.nav.m2m.submitter.service.ManagedStoragePathPolicy;
import hu.nav.m2m.submitter.service.nav.audit.NavHttpAuditFormatter;
import hu.nav.m2m.submitter.service.nav.audit.NavHttpAuditHolder;
import hu.nav.m2m.submitter.service.nav.audit.NavHttpAuditLogger;
import hu.nav.m2m.submitter.service.nav.audit.NavHttpTrace;
import hu.nav.m2m.submitter.util.Sha256Util;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Base64;

/**
 * A NAV M2M/Bizonylat API tényleges HTTP gateway implementációja, feltöltés, bizonylat, validáció, kalkuláció és státusz műveletekkel.
 */
@Component("realNavGateway")
public class RealNavGateway implements NavGateway {
    private static final Logger log = LoggerFactory.getLogger(RealNavGateway.class);
    private final NavM2mProperties properties;
    private final NavTokenService tokenService;
    private final M2mSignatureService signatureService;
    private final NavRestTemplateFactory restTemplateFactory;
    private final ManagedStoragePathPolicy storagePathPolicy;
    private final JsonFactory jsonFactory = new JsonFactory();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Létrehozza a(z) {@code RealNavGateway} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
     *
     * @param properties az M2M külső konfiguráció
     * @param tokenService a művelethez átadott {@code tokenService} érték
     * @param signatureService a művelethez átadott {@code signatureService} érték
     * @param restTemplateFactory a művelethez átadott {@code restTemplateFactory} érték
     * @param storagePathPolicy a művelethez átadott {@code storagePathPolicy} érték
     */
    public RealNavGateway(NavM2mProperties properties, NavTokenService tokenService, M2mSignatureService signatureService, NavRestTemplateFactory restTemplateFactory, ManagedStoragePathPolicy storagePathPolicy) {
        this.properties = properties;
        this.tokenService = tokenService;
        this.signatureService = signatureService;
        this.restTemplateFactory = restTemplateFactory;
        this.storagePathPolicy = storagePathPolicy;
    }

    /**
     * Feltölti a csatolmányfájlt a NAV filestore végpontjára, ellenőrzi a fájl méretét/hashét és a válaszból visszaadja a NAV fileId-t.
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
        String actualSha256Hex = recomputeSha256Hex(file);
        boolean hashMismatch = sha256Hex != null && !sha256Hex.equalsIgnoreCase(actualSha256Hex);
        String effectiveSha256Hex = actualSha256Hex;
        long actualSizeBytes = fileSize(file);

        M2mSignatureService.SignatureDebug signatureDebug = signatureService.createSignatureDebug(messageId, effectiveSha256Hex);
        String signature = signatureDebug.signatureBase64Upper();
        String rawSignatureUrlEncodedCompareOnly = URLEncoder.encode(signature, StandardCharsets.UTF_8);
        String url = base(properties.getEndpoints().getCommonBaseUrl())
                + properties.getEndpoints().getFileUploadPath()
                + "?sha256hash=" + effectiveSha256Hex
                + "&signature=" + signature;
        URI uploadUri = URI.create(url);

        HttpHeaders headers = authHeaders(messageId, correlationId);
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        FileSystemResource resource = new FileSystemResource(file);
        String requestPayload = "binary file upload\n" +
                "fileName=" + fileName + "\n" +
                "filePath=" + file + "\n" +
                "fileSizeFromDb=" + sizeBytes + "\n" +
                "fileSizeActual=" + actualSizeBytes + "\n" +
                "storedOrPassed.sha256hash=" + sha256Hex + "\n" +
                "actualFile.sha256hash=" + actualSha256Hex + "\n" +
                "hashMismatch.storedVsActual=" + hashMismatch + "\n" +
                "hashUsedForQueryAndSignature=" + effectiveSha256Hex + "\n" +
                "\n--- REQUEST_BODY_BASE64_FULL ---\n" +
                readFileAsBase64ForTrace(file) + "\n" +
                "\n--- REQUEST_QUERY_PARAMETERS ---\n" +
                "query.sha256hash=" + effectiveSha256Hex + "\n" +
                "query.signature=" + signature + "\n" +
                "query.signature.urlEncodedCompareOnly=" + rawSignatureUrlEncodedCompareOnly + "\n" +
                "query.signature.sentEncoding=RAW_NOT_URL_ENCODED\n" +
                "query.signature.isBase64=true\n" +
                "query.signature.length=" + signature.length() + "\n" +
                "query.fullUrl=" + url + "\n" +
                "\n--- FILE_UPLOAD_SIGNATURE_DEBUG ---\n" +
                "doc.source=M2M altalanos interfesz specifikacio 5.1.1 Alairas + 6.4.1 Fajlfeltoltes\n" +
                "doc.formula=BASE64(SHA-256(messageId+timestamp+operationSpecificData+signatureKey)).toUpperCase()\n" +
                "doc.timestampFormat=UTC yyyyMMddHHmmss\n" +
                "doc.operationSpecificDataForFileUpload=sha256hash\n" +
                "doc.uppercaseRequired=true\n" +
                "postman.equivalent.timestamp=new Date().toISOString().replace(/[^\\d]/g, '').substring(0,14)\n" +
                "postman.equivalent.toSign=message_id+timestamp+file_hash+signature_key_first_part+signature_key_second_part\n" +
                "project.expected.signature=Base64(SHA256_DIGEST_BYTES(toSign)).toUpperCase()\n" +
                "\n--- SIGNATURE_SENT_BY_PROGRAM ---\n" +
                "signature.algorithm=Base64Upper(SHA256_DIGEST_BYTES(signatureBase UTF-8 bytes))\n" +
                "signature.important=A kikuldott signature a SHA-256 digest byte tomb kozvetlen Base64-e, nagybetusitve. A hex string Base64 csak osszehasonlitas.\n" +
                "signature.basePattern=messageId+timestamp+sha256hash+keyFirstPart+keySecondPart\n" +
                "signature.timestamp=" + signatureDebug.timestamp() + "\n" +
                "signature.messageId=" + messageId + "\n" +
                "signature.operationSpecificData.sha256hash=" + effectiveSha256Hex + "\n" +
                "signature.keyFirstPart=" + signatureDebug.keyFirstPart() + "\n" +
                "signature.nonce=" + signatureDebug.nonce() + "\n" +
                "signature.keySecondPart=" + signatureDebug.keySecondPart() + "\n" +
                "signature.configuredKeySecondPart=" + signatureDebug.configuredKeySecondPart() + "\n" +
                "signature.keySecondPartSource=" + signatureDebug.keySecondPartSource() + "\n" +
                "signature.nonceUsedAsKeySecondPart=" + signatureDebug.nonceUsedAsKeySecondPart() + "\n" +
                "signature.signatureKeyWarning=" + signatureDebug.signatureKeyWarning() + "\n" +
                "signature.signatureKey=keyFirstPart+keySecondPart=" + signatureDebug.signatureKey() + "\n" +
                "signature.base=" + signatureDebug.signatureBase() + "\n" +
                "postman.toSign=" + signatureDebug.signatureBase() + "\n" +
                "signature.sha256.hex.lower=" + signatureDebug.signatureSha256HexLower() + "\n" +
                "signature.sha256.hex.upper=" + signatureDebug.signatureSha256HexUpper() + "\n" +
                "signature.sha256.hexStringBase64.compareOnly=" + signatureDebug.base64OfHexStringCompareOnly() + "\n" +
                "signature.sha256.hexStringBase64.upper.compareOnly=" + signatureDebug.base64OfHexStringCompareOnlyUpper() + "\n" +
                "signature.sha256.digestBytesBase64=" + signatureDebug.digestBytesBase64() + "\n" +
                "signature.sha256.digestBytesBase64.upper=" + signatureDebug.digestBytesBase64Upper() + "\n" +
                "signature.query.signature=" + signature + "\n" +
                "signature.query.signature.equals.calculatedBase64Upper=" + String.valueOf(signature.equals(signatureDebug.signatureBase64Upper())) + "\n" +
                "\n--- PREVIOUS_WRONG_FORMULA_FOR_COMPARISON_ONLY ---\n" +
                "old.basePattern=keyPart1+messageId+sha256hash+timestamp+keyPart2\n" +
                "old.base=" + signatureDebug.oldBase() + "\n" +
                "old.sha256.hex.lower=" + signatureDebug.oldSha256HexLower() + "\n" +
                "old.sha256.hex.upper=" + signatureDebug.oldSha256HexUpper() + "\n" +
                "old.sha256.hexStringBase64.compareOnly=" + signatureDebug.oldBase64() + "\n" +
                "old.sha256.hexStringBase64.upper.compareOnly=" + signatureDebug.oldBase64Upper() + "\n" +
                "old.sha256.digestBytesBase64=" + signatureDebug.oldDigestBytesBase64Value() + "\n" +
                "old.sha256.digestBytesBase64.upper=" + signatureDebug.oldDigestBytesBase64UpperValue() + "\n" +
                "old.query.signature.equals.oldBase64Upper=" + String.valueOf(signature.equals(signatureDebug.oldBase64Upper())) + "\n";
        try {
            ResponseEntity<JsonNode> response = restTemplateFactory.create().exchange(uploadUri, HttpMethod.POST, new HttpEntity<>(resource, headers), JsonNode.class);
            JsonNode node = requireBody(response, "Common File upload");
            audit("COMMON_FILE_UPLOAD", "POST", url, headers, requestPayload, response, node.toString());
            String fileId = firstText(node, "fileId", "documentFileId", "id");
            return new UploadedFile(fileId, firstTextOr(node, "OK", "resultCode", "result_code"), firstTextOr(node, null, "virusScanResultCode", "virus_scan_result_code"));
        } catch (RuntimeException e) {
            auditException("COMMON_FILE_UPLOAD", "POST", url, headers, requestPayload, e);
            throw e;
        }
    }


    /**
     * A(z) {@code recomputeSha256Hex} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param file a feldolgozandó fájl
     * @return a művelet eredménye
     */
    private String recomputeSha256Hex(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            return Sha256Util.sha256Hex(in);
        } catch (IOException e) {
            throw new IllegalStateException("Nem sikerült SHA-256 hash-t számolni a feltöltendő fájlra: " + file, e);
        }
    }

    /**
     * A(z) {@code fileSize} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param file a feldolgozandó fájl
     * @return a kiszámított darabszám vagy méret
     */
    private long fileSize(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            throw new IllegalStateException("Nem sikerült lekérdezni a feltöltendő fájl méretét: " + file, e);
        }
    }


    /**
     * A Bizonylat API beküldést a dokumentumtípus/verzió alapján képzett route-on indítja, az XML payloadot és szükséges auth/aláírás fejléceket elküldve.
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
        try {
            if (Files.size(payloadFile) > properties.getMaxInMemoryBizonylatApiBytes()) {
                throw new IllegalStateException("A Bizonylat API JSON/Base64 payload túl nagy a beállított limithez: " + Files.size(payloadFile));
            }
            String url = base(properties.getEndpoints().getBizonylatBaseUrl()) + properties.getEndpoints().getBizonylatPath();
            JsonNode node = postBizonylatStreaming("BIZONYLAT_CREATE", url, bizonylatTipus, bizonylatVerzio, payloadFile, compression == null ? CompressionType.NONE : compression, signature, signatureDebug, operationHash, validationCertificate, messageId, correlationId);
            return new BizonylatCreateResult(
                    firstText(node, "ugyAzonosito", "caseId"),
                    firstText(node, "bizonylatStatusz", "status", "navStatus"),
                    firstText(node, "erkeztetesiSzam", "arrivalNumber"),
                    firstTextOr(node, "OK", "resultCode", "result_code"),
                    firstText(node, "resultMessage", "message"),
                    firstText(node, "befogadasIdopontja", "acceptedAt"),
                    firstText(node, "megjegyzes", "note"),
                    firstText(node, "validaciosHibak", "validationErrors"),
                    node.toString());
        } catch (IOException e) {
            throw new IllegalStateException("Bizonylat API payload előkészítés sikertelen", e);
        }
    }

    /**
     * Elindítja az online validációs műveletet a Bizonylat API-n, és visszaadja a NAV által adott műveleti azonosítót/állapotot.
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
        try {
            ensureBizonylatPayloadSize(payloadFile, "Validacio API");
            String url = base(properties.getEndpoints().getBizonylatBaseUrl()) + properties.getEndpoints().getValidacioPath();
            JsonNode node = postBizonylatStreaming("VALIDACIO_CREATE", url, bizonylatTipus, bizonylatVerzio, payloadFile, compression == null ? CompressionType.NONE : compression, signature, signatureDebug, operationHash, null, messageId, correlationId);
            return validacioResult(node);
        } catch (IOException e) {
            throw new IllegalStateException("Validacio API payload előkészítés sikertelen", e);
        }
    }

    /**
     * Lekéri a korábban indított online validáció aktuális eredményét, majd a NAV válaszát normalizált gateway eredménnyé alakítja.
     *
     * @param ugyAzonosito a művelethez átadott {@code ugyAzonosito} érték
     * @param messageId a NAV kérés egyedi messageId értéke
     * @param correlationId a művelethez átadott {@code correlationId} érték
     * @return a művelet eredménye
     */
    @Override
    public ValidacioResult getValidacio(String ugyAzonosito, String messageId, String correlationId) {
        String url = base(properties.getEndpoints().getBizonylatBaseUrl()) + properties.getEndpoints().getValidacioPath()
                + "/" + encodePathSegment(ugyAzonosito);
        JsonNode node = exchangeJson("VALIDACIO_GET", url, HttpMethod.GET, null, messageId, correlationId);
        return validacioResult(node);
    }

    /**
     * Elindítja az online kalkulációt a NAV Bizonylat API-n.
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
        try {
            ensureBizonylatPayloadSize(payloadFile, "Kalkulacio API");
            String url = base(properties.getEndpoints().getBizonylatBaseUrl()) + properties.getEndpoints().getKalkulacioPath();
            JsonNode node = postBizonylatStreaming("KALKULACIO_CREATE", url, bizonylatTipus, bizonylatVerzio, payloadFile, compression == null ? CompressionType.NONE : compression, signature, signatureDebug, operationHash, null, messageId, correlationId);
            return kalkulacioResult(node);
        } catch (IOException e) {
            throw new IllegalStateException("Kalkulacio API payload előkészítés sikertelen", e);
        }
    }

    /**
     * Lekéri a kalkuláció aktuális eredményét, beleértve a tömörített vagy Base64-ben érkező számított XML payloadot.
     *
     * @param ugyAzonosito a művelethez átadott {@code ugyAzonosito} érték
     * @param messageId a NAV kérés egyedi messageId értéke
     * @param correlationId a művelethez átadott {@code correlationId} érték
     * @return a művelet eredménye
     */
    @Override
    public KalkulacioResult getKalkulacio(String ugyAzonosito, String messageId, String correlationId) {
        String url = base(properties.getEndpoints().getBizonylatBaseUrl()) + properties.getEndpoints().getKalkulacioPath()
                + "/" + encodePathSegment(ugyAzonosito);
        JsonNode node = exchangeJson("KALKULACIO_GET", url, HttpMethod.GET, null, messageId, correlationId);
        return kalkulacioResult(node);
    }

    /**
     * A(z) {@code validacioResult} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param node a művelethez átadott {@code node} érték
     * @return a művelet eredménye
     */
    private ValidacioResult validacioResult(JsonNode node) {
        return new ValidacioResult(
                firstText(node, "ugyAzonosito", "caseId"),
                firstText(node, "bizonylatValidacioStatusz", "validacioStatusz", "status"),
                firstTextOr(node, "OK", "resultCode", "result_code"),
                firstText(node, "resultMessage", "message"),
                firstText(node, "validaciosHibak", "validationErrors"),
                firstText(node, "validaciosTanusitvany", "validationCertificate"),
                node.toString());
    }

    /**
     * A(z) {@code kalkulacioResult} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param node a művelethez átadott {@code node} érték
     * @return a művelet eredménye
     */
    private KalkulacioResult kalkulacioResult(JsonNode node) {
        JsonNode hiba = node == null ? null : node.get("kalkulaciosHiba");
        return new KalkulacioResult(
                firstText(node, "ugyAzonosito", "caseId"),
                firstText(node, "bizonylatKalkulacioStatusz", "kalkulacioStatusz", "status"),
                firstTextOr(node, "OK", "resultCode", "result_code"),
                firstText(node, "resultMessage", "message"),
                firstText(node, "bizonylatXml", "calculatedXml"),
                firstText(node, "tomorites", "compression"),
                firstText(hiba, "hibaKod", "errorCode"),
                firstText(hiba, "hibaUzenet", "errorMessage"),
                firstText(hiba, "mezoAzonosito", "fieldId"),
                firstText(hiba, "szabalyAzonosito", "ruleId"),
                node.toString());
    }

    /**
     * Ellenőrzi a művelet kötelező előfeltételeit és inkonzisztens vagy nem engedélyezett állapot esetén kontrollált kivétellel megszakítja a feldolgozást.
     *
     * @param payloadFile a művelethez átadott {@code payloadFile} érték
     * @param operation a NAV vagy életciklus művelet neve
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private void ensureBizonylatPayloadSize(Path payloadFile, String operation) throws IOException {
        long size = Files.size(payloadFile);
        if (size > properties.getMaxInMemoryBizonylatApiBytes()) {
            throw new IllegalStateException(operation + " JSON/Base64 payload túl nagy a beállított limithez: " + size);
        }
    }

    /**
     * A(z) {@code encodePathSegment} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param value a feldolgozandó érték
     * @return a művelet eredménye
     */
    private String encodePathSegment(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("A NAV ügyazonosító kötelező.");
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
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
        String url = base(properties.getEndpoints().getBizonylatBaseUrl())
                + properties.getEndpoints().getBizonylatPath()
                + "/" + navIdentifier;
        JsonNode node = exchangeJson("GET_STATUS", url, HttpMethod.GET, null, messageId, correlationId);
        String status = firstText(node, "bizonylatStatusz", "status", "navStatus");
        return new StatusResult(status, firstText(node, "erkeztetesiSzam", "arrivalNumber"), firstTextOr(node, "OK", "resultCode", "result_code"), node.toString());
    }

    /**
     * A nagyobb Bizonylat XML payloadot streaming HTTP kérésként küldi, miközben a diagnosztikai audit számára kontrollált request információt készít.
     *
     * @param operation a NAV vagy életciklus művelet neve
     * @param url a cél NAV végpont
     * @param tipus a művelethez átadott {@code tipus} érték
     * @param verzio a művelethez átadott {@code verzio} érték
     * @param payloadFile a művelethez átadott {@code payloadFile} érték
     * @param compression a művelethez átadott {@code compression} érték
     * @param signature a művelethez átadott {@code signature} érték
     * @param signatureDebug a művelethez átadott {@code signatureDebug} érték
     * @param operationHash a művelethez átadott {@code operationHash} érték
     * @param validationCertificate a művelethez átadott {@code validationCertificate} érték
     * @param messageId a NAV kérés egyedi messageId értéke
     * @param correlationId a művelethez átadott {@code correlationId} érték
     * @return a művelet eredménye
     */
    private JsonNode postBizonylatStreaming(String operation, String url, String tipus, String verzio, Path payloadFile, CompressionType compression, String signature, M2mSignatureService.SignatureDebug signatureDebug, String operationHash, String validationCertificate, String messageId, String correlationId) {
        HttpHeaders previewHeaders = authHeaders(messageId, correlationId);
        previewHeaders.setContentType(MediaType.APPLICATION_JSON);
        String requestPayload = buildBizonylatXmlDiagnostic(payloadFile, compression, operationHash)
                + "\n\n--- EXACT_JSON_REQUEST_BODY ---\n"
                + buildFullBizonylatRequestPayload(tipus, verzio, payloadFile, compression, signature)
                + "\n\n--- BIZONYLAT_API_SIGNATURE_DEBUG ---\n"
                + "doc.source=M2M altalanos interfesz specifikacio 5.1.1 Alairas + Bizonylat API signature mező\n"
                + "doc.formula=BASE64(SHA-256(messageId+timestamp+operationSpecificData+signatureKey)).toUpperCase()\n"
                + "doc.timestampFormat=UTC yyyyMMddHHmmss\n"
                + "doc.operationSpecificDataForBizonylatApi=a beküldött payload SHA-256 hash értéke (RAW: XML byte-ok, GZIP: GZIP byte-ok)\n"
                + "doc.uppercaseRequired=true\n"
                + "signature.algorithm=Base64Upper(SHA256_DIGEST_BYTES(signatureBase UTF-8 bytes))\n"
                + "signature.important=A kikuldott signature a SHA-256 digest byte tomb kozvetlen Base64-e, nagybetusitve. A hex string Base64 csak osszehasonlitas.\n"
                + "signature.basePattern=messageId+timestamp+payloadSha256Hash+signatureKey\n"
                + "signature.timestamp=" + signatureDebug.timestamp() + "\n"
                + "signature.messageId=" + messageId + "\n"
                + "signature.operationSpecificData.payloadSha256Hash=" + operationHash + "\n"
                + "payload.compression=" + compression + "\n"
                + "payload.hashInput=" + (compression == CompressionType.GZIP ? "GZIP_BYTES" : "RAW_XML_BYTES") + "\n"
                + "signature.keyFirstPart=" + signatureDebug.keyFirstPart() + "\n"
                + "signature.nonce=" + signatureDebug.nonce() + "\n"
                + "signature.keySecondPart=" + signatureDebug.keySecondPart() + "\n"
                + "signature.configuredKeySecondPart=" + signatureDebug.configuredKeySecondPart() + "\n"
                + "signature.keySecondPartSource=" + signatureDebug.keySecondPartSource() + "\n"
                + "signature.nonceUsedAsKeySecondPart=" + signatureDebug.nonceUsedAsKeySecondPart() + "\n"
                + "signature.signatureKeyWarning=" + signatureDebug.signatureKeyWarning() + "\n"
                + "signature.signatureKey=keyFirstPart+keySecondPart=" + signatureDebug.signatureKey() + "\n"
                + "signature.base=" + signatureDebug.signatureBase() + "\n"
                + "signature.sha256.hex.lower=" + signatureDebug.signatureSha256HexLower() + "\n"
                + "signature.sha256.hex.upper=" + signatureDebug.signatureSha256HexUpper() + "\n"
                + "signature.sha256.hexStringBase64.compareOnly=" + signatureDebug.base64OfHexStringCompareOnly() + "\n"
                + "signature.sha256.hexStringBase64.upper.compareOnly=" + signatureDebug.base64OfHexStringCompareOnlyUpper() + "\n"
                + "signature.sha256.digestBytesBase64=" + signatureDebug.digestBytesBase64() + "\n"
                + "signature.sha256.digestBytesBase64.upper=" + signatureDebug.digestBytesBase64Upper() + "\n"
                + "request.body.signature=" + signature + "\n"
                + "request.body.signature.equals.calculatedBase64Upper=" + String.valueOf(signature.equals(signatureDebug.signatureBase64Upper())) + "\n"
                + "\n--- PREVIOUS_WRONG_FORMULA_FOR_COMPARISON_ONLY ---\n"
                + "old.basePattern=keyPart1+messageId+payloadSha256Hash+timestamp+keyPart2\n"
                + "old.base=" + signatureDebug.oldBase() + "\n"
                + "old.sha256.hexStringBase64.upper.compareOnly=" + signatureDebug.oldBase64Upper() + "\n"
                + "old.sha256.digestBytesBase64=" + signatureDebug.oldDigestBytesBase64Value() + "\n"
                + "old.sha256.digestBytesBase64.upper=" + signatureDebug.oldDigestBytesBase64UpperValue() + "\n";
        try {
            JsonNode node = restTemplateFactory.create().execute(url, HttpMethod.POST, request -> {
                HttpHeaders headers = request.getHeaders();
                headers.putAll(previewHeaders);
                headers.setContentType(MediaType.APPLICATION_JSON);
                try (JsonGenerator g = jsonFactory.createGenerator(request.getBody()); InputStream in = Files.newInputStream(payloadFile)) {
                    g.writeStartObject();
                    g.writeObjectFieldStart("requestData");
                    g.writeStringField("bizonylatTipus", tipus);
                    g.writeStringField("bizonylatVerzio", verzio);
                    g.writeFieldName("bizonylatXml");
                    long payloadSize = Files.size(payloadFile);
                    if (payloadSize > Integer.MAX_VALUE) {
                        throw new IllegalStateException("A Bizonylat API payload túl nagy a Jackson streaming Base64 íráshoz: " + payloadSize + " byte");
                    }
                    g.writeBinary(Base64Variants.MIME_NO_LINEFEEDS, in, (int) payloadSize);
                    g.writeStringField("signature", signature);
                    if (validationCertificate != null && !validationCertificate.isBlank()) {
                        g.writeStringField("validaciosTanusitvany", validationCertificate);
                    }
                    if (compression == CompressionType.GZIP) {
                        g.writeStringField("tomorites", "GZIP");
                    } else {
                        g.writeNullField("tomorites");
                    }
                    g.writeEndObject();
                    g.writeEndObject();
                }
            }, response -> {
                HttpHeaders responseHeaders = response.getHeaders();
                int rawStatus = response.getStatusCode().value();
                try (InputStream body = response.getBody()) {
                    JsonNode responseNode = objectMapper.readTree(body);
                    audit(operation, "POST", url, previewHeaders, requestPayload,
                            rawStatus + " " + response.getStatusText(), NavHttpAuditFormatter.headers(responseHeaders),
                            appendDecodedValidationErrors(responseNode.toString(), responseNode));
                    return responseNode;
                }
            });
            if (node == null) throw new IllegalStateException(operation + " válasz üres");
            return node;
        } catch (RuntimeException e) {
            auditException(operation, "POST", url, previewHeaders, requestPayload, e);
            throw e;
        }
    }

    /**
     * A bemeneti domain/transport adatokból a következő feldolgozási réteg által igényelt reprezentációt állítja elő.
     *
     * @param payloadFile a művelethez átadott {@code payloadFile} érték
     * @param compression a művelethez átadott {@code compression} érték
     * @param operationHash a művelethez átadott {@code operationHash} érték
     * @return a művelet eredménye
     */
    private String buildBizonylatXmlDiagnostic(Path payloadFile, CompressionType compression, String operationHash) {
        try {
            byte[] bytes = storagePathPolicy.readAllBytes(payloadFile.toString());
            boolean utf8Bom = bytes.length >= 3
                    && (bytes[0] & 0xff) == 0xef
                    && (bytes[1] & 0xff) == 0xbb
                    && (bytes[2] & 0xff) == 0xbf;
            int firstContentOffset = utf8Bom ? 3 : 0;
            while (firstContentOffset < bytes.length) {
                int value = bytes[firstContentOffset] & 0xff;
                if (value == 0x20 || value == 0x09 || value == 0x0a || value == 0x0d) {
                    firstContentOffset++;
                } else {
                    break;
                }
            }
            boolean startsWithXmlMarkup = firstContentOffset < bytes.length
                    && bytes[firstContentOffset] == '<';
            int hexLength = Math.min(bytes.length, 64);
            StringBuilder firstBytesHex = new StringBuilder(hexLength * 3);
            for (int i = 0; i < hexLength; i++) {
                if (i > 0) firstBytesHex.append(' ');
                firstBytesHex.append(String.format(Locale.ROOT, "%02X", bytes[i] & 0xff));
            }
            String xmlText = new String(bytes, StandardCharsets.UTF_8);
            return "--- BIZONYLAT_XML_DIAGNOSTIC ---\n"
                    + "operationPayloadPath=" + payloadFile.toAbsolutePath().normalize() + "\n"
                    + "payloadSizeBytes=" + bytes.length + "\n"
                    + "payloadCompression=" + compression + "\n"
                    + "payloadSha256=" + operationHash + "\n"
                    + "utf8BomPresent=" + utf8Bom + "\n"
                    + "firstNonWhitespaceByteOffset=" + firstContentOffset + "\n"
                    + "startsWithXmlMarkup=" + startsWithXmlMarkup + "\n"
                    + "firstBytesHex=" + firstBytesHex + "\n"
                    + "\n--- DECODED_XML_SENT_TO_NAV ---\n"
                    + xmlText;
        } catch (IOException e) {
            return "--- BIZONYLAT_XML_DIAGNOSTIC ---\n"
                    + "A kiküldött XML diagnosztikai kiolvasása sikertelen: " + e.getMessage();
        }
    }

    /**
     * A bemeneti domain/transport adatokból a következő feldolgozási réteg által igényelt reprezentációt állítja elő.
     *
     * @param tipus a művelethez átadott {@code tipus} érték
     * @param verzio a művelethez átadott {@code verzio} érték
     * @param payloadFile a művelethez átadott {@code payloadFile} érték
     * @param compression a művelethez átadott {@code compression} érték
     * @param signature a művelethez átadott {@code signature} érték
     * @return a művelet eredménye
     */
    private String buildFullBizonylatRequestPayload(String tipus, String verzio, Path payloadFile, CompressionType compression, String signature) {
        Map<String, Object> requestData = new LinkedHashMap<>();
        requestData.put("bizonylatTipus", tipus);
        requestData.put("bizonylatVerzio", verzio);
        requestData.put("bizonylatXml", readFileAsBase64ForTrace(payloadFile));
        requestData.put("signature", signature);
        requestData.put("tomorites", compression == CompressionType.GZIP ? "GZIP" : null);
        Map<String, Object> wrapper = new LinkedHashMap<>();
        wrapper.put("requestData", requestData);
        try {
            return objectMapper.writeValueAsString(wrapper);
        } catch (Exception e) {
            return String.valueOf(wrapper);
        }
    }

    /**
     * A bemeneti struktúrából biztonságosan kiolvassa a művelethez szükséges értéket, és hiányzó adat esetén a metódus szerinti fallbacket alkalmazza.
     *
     * @param file a feldolgozandó fájl
     * @return a művelet eredménye
     */
    private String readFileAsBase64ForTrace(Path file) {
        try {
            return Base64.getEncoder().encodeToString(storagePathPolicy.readAllBytes(file.toString()));
        } catch (IOException e) {
            return "<body read failed: " + e.getMessage() + ">";
        } catch (OutOfMemoryError e) {
            return "<body too large to log in memory: " + file + ">";
        }
    }


    /**
     * A(z) {@code requestData} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param requestData a művelethez átadott {@code requestData} érték
     * @return a művelet eredménye
     */
    private Map<String, Object> requestData(Map<String, Object> requestData) {
        Map<String, Object> wrapper = new LinkedHashMap<>();
        wrapper.put("requestData", requestData);
        return wrapper;
    }

    /**
     * A(z) {@code postJson} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param operation a NAV vagy életciklus művelet neve
     * @param url a cél NAV végpont
     * @param body a NAV HTTP válasz törzse
     * @param messageId a NAV kérés egyedi messageId értéke
     * @param correlationId a művelethez átadott {@code correlationId} érték
     * @return a művelet eredménye
     */
    private JsonNode postJson(String operation, String url, Map<String, Object> body, String messageId, String correlationId) {
        return exchangeJson(operation, url, HttpMethod.POST, body, messageId, correlationId);
    }

    /**
     * Közös JSON HTTP végrehajtási pont: elküldi a kérést, auditálja a választ, egységesen kezeli a NAV és transport hibákat, majd ellenőrzött választ ad vissza.
     *
     * @param operation a NAV vagy életciklus művelet neve
     * @param url a cél NAV végpont
     * @param method a művelethez átadott {@code method} érték
     * @param body a NAV HTTP válasz törzse
     * @param messageId a NAV kérés egyedi messageId értéke
     * @param correlationId a művelethez átadott {@code correlationId} érték
     * @return a művelet eredménye
     */
    private JsonNode exchangeJson(String operation, String url, HttpMethod method, Map<String, Object> body, String messageId, String correlationId) {
        HttpHeaders headers = authHeaders(messageId, correlationId);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        String requestPayload = toJsonLimited(body);
        try {
            ResponseEntity<JsonNode> response = restTemplateFactory.create().exchange(url, method, new HttpEntity<>(body, headers), JsonNode.class);
            JsonNode node = requireBody(response, method + " " + url);
            audit(operation, method.name(), url, headers, requestPayload, response, appendDecodedValidationErrors(node.toString(), node));
            return node;
        } catch (RuntimeException e) {
            auditException(operation, method.name(), url, headers, requestPayload, e);
            throw e;
        }
    }

    /**
     * A(z) {@code appendDecodedValidationErrors} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param responsePayload a művelethez átadott {@code responsePayload} érték
     * @param node a művelethez átadott {@code node} érték
     * @return a művelet eredménye
     */
    private String appendDecodedValidationErrors(String responsePayload, JsonNode node) {
        if (node == null) {
            return responsePayload;
        }
        String encoded = firstText(node, "validaciosHibak", "validationErrors");
        if (encoded == null || encoded.isBlank() || "null".equalsIgnoreCase(encoded.trim())) {
            return responsePayload;
        }
        String decoded = decodeBzip2Base64(encoded.trim());
        if (decoded == null || decoded.isBlank()) {
            return responsePayload + "\n\n--- DECODED VALIDACIOS HIBAK ---\n"
                    + "A validaciosHibak mező dekódolása nem sikerült. A mező Base64 + BZip2 formátumúként várt.";
        }
        return responsePayload + "\n\n--- DECODED VALIDACIOS HIBAK ---\n" + decoded;
    }

    /**
     * A NAV válaszban kapott kódolt vagy tömörített tartalmat a várt formátum szerint visszaalakítja további XML-feldolgozáshoz.
     *
     * @param encoded a művelethez átadott {@code encoded} érték
     * @return a művelet eredménye
     */
    private String decodeBzip2Base64(String encoded) {
        try {
            byte[] compressed = Base64.getDecoder().decode(encoded);
            try (InputStream raw = new ByteArrayInputStream(compressed);
                 BZip2CompressorInputStream bzip2 = new BZip2CompressorInputStream(raw);
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                bzip2.transferTo(out);
                return out.toString(StandardCharsets.UTF_8);
            }
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Összeállítja a NAV művelethez szükséges hitelesítési és aláírási HTTP headereket az aktuális tokenből és signature-ből.
     *
     * @param messageId a NAV kérés egyedi messageId értéke
     * @param correlationId a művelethez átadott {@code correlationId} érték
     * @return a művelet eredménye
     */
    private HttpHeaders authHeaders(String messageId, String correlationId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenService.getAccessToken());
        if (messageId != null) {
            headers.add("messageId", messageId);
            headers.add("X-Message-Id", messageId);
        }
        if (correlationId != null) {
            headers.add("correlationId", correlationId);
            headers.add("X-Correlation-Id", correlationId);
        }
        return headers;
    }

    /**
     * Ellenőrzi a művelet kötelező előfeltételeit és inkonzisztens vagy nem engedélyezett állapot esetén kontrollált kivétellel megszakítja a feldolgozást.
     *
     * @param response a NAV HTTP válasz
     * @param operation a NAV vagy életciklus művelet neve
     * @return a művelet eredménye
     */
    private JsonNode requireBody(ResponseEntity<JsonNode> response, String operation) {
        JsonNode node = response.getBody();
        if (node == null) throw new IllegalStateException(operation + " válasz üres");
        return node;
    }

    /**
     * A NAV HTTP művelet request/response adataiból maszkolt audit trace eseményt készít és eltárolja.
     *
     * @param operation a NAV vagy életciklus művelet neve
     * @param method a művelethez átadott {@code method} érték
     * @param url a cél NAV végpont
     * @param requestHeaders a művelethez átadott {@code requestHeaders} érték
     * @param requestPayload a művelethez átadott {@code requestPayload} érték
     * @param response a NAV HTTP válasz
     * @param responsePayload a művelethez átadott {@code responsePayload} érték
     */
    private void audit(String operation, String method, String url, HttpHeaders requestHeaders, String requestPayload, ResponseEntity<?> response, String responsePayload) {
        audit(operation, method, url, requestHeaders, requestPayload,
                NavHttpAuditFormatter.status(response), NavHttpAuditFormatter.responseHeaders(response), responsePayload);
    }

    /**
     * A NAV HTTP művelet request/response adataiból maszkolt audit trace eseményt készít és eltárolja.
     *
     * @param operation a NAV vagy életciklus művelet neve
     * @param method a művelethez átadott {@code method} érték
     * @param url a cél NAV végpont
     * @param requestHeaders a művelethez átadott {@code requestHeaders} érték
     * @param requestPayload a művelethez átadott {@code requestPayload} érték
     * @param responseStatus a művelethez átadott {@code responseStatus} érték
     * @param responseHeaders a művelethez átadott {@code responseHeaders} érték
     * @param responsePayload a művelethez átadott {@code responsePayload} érték
     */
    private void audit(String operation, String method, String url, HttpHeaders requestHeaders, String requestPayload, String responseStatus, String responseHeaders, String responsePayload) {
        String formattedRequestHeaders = NavHttpAuditFormatter.headers(requestHeaders);
        String formattedResponseHeaders = NavHttpAuditFormatter.limit(responseHeaders);
        NavHttpAuditLogger.trace(NavHttpAuditFormatter.fullTraceBlock(
                operation,
                method,
                url,
                formattedRequestHeaders,
                requestPayload,
                responseStatus,
                responseHeaders,
                responsePayload));
        NavHttpAuditHolder.add(new NavHttpTrace(
                operation,
                method,
                url,
                formattedRequestHeaders,
                NavHttpAuditFormatter.payloadSummary(requestPayload),
                responseStatus,
                formattedResponseHeaders,
                NavHttpAuditFormatter.payloadSummary(responsePayload),
                NavHttpAuditFormatter.configSnapshot(properties) + "\n" + restTemplateFactory.proxySnapshot()
        ));
    }

    /**
     * A NAV HTTP művelet kivételes befejezését maszkolt audit trace eseményként rögzíti.
     *
     * @param operation a NAV vagy életciklus művelet neve
     * @param method a művelethez átadott {@code method} érték
     * @param url a cél NAV végpont
     * @param requestHeaders a művelethez átadott {@code requestHeaders} érték
     * @param requestPayload a művelethez átadott {@code requestPayload} érték
     * @param e a feldolgozás közben kapott kivétel
     */
    private void auditException(String operation, String method, String url, HttpHeaders requestHeaders, String requestPayload, RuntimeException e) {
        String responseStatus = "EXCEPTION";
        String responseHeaders = null;
        String responsePayload = e.getClass().getSimpleName() + ": " + e.getMessage();
        if (e instanceof RestClientResponseException r) {
            responseStatus = r.getStatusCode().value() + " " + r.getStatusText();
            responseHeaders = r.getResponseHeaders() == null ? null : NavHttpAuditFormatter.headers(r.getResponseHeaders());
            responsePayload = r.getResponseBodyAsString();
        }
        audit(operation, method, url, requestHeaders, requestPayload, responseStatus, responseHeaders, responsePayload);
    }

    /**
     * A bemeneti domain/transport adatokból a következő feldolgozási réteg által igényelt reprezentációt állítja elő.
     *
     * @param body a NAV HTTP válasz törzse
     * @return a művelet eredménye
     */
    private String toJsonLimited(Map<String, Object> body) {
        if (body == null) return "<no body>";
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            return String.valueOf(body);
        }
    }

    /**
     * A(z) {@code maskSensitive} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param value a feldolgozandó érték
     * @return a művelet eredménye
     */
    @SuppressWarnings("unchecked")
    private Object maskSensitive(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((k, v) -> {
                String key = String.valueOf(k);
                if (isSensitiveKey(key)) copy.put(key, NavHttpAuditFormatter.maskMiddle(String.valueOf(v)));
                else copy.put(key, maskSensitive(v));
            });
            return copy;
        }
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>();
            for (Object item : list) copy.add(maskSensitive(item));
            return copy;
        }
        return value;
    }

    /**
     * A jelenlegi állapot és az M2M életciklusszabályok alapján eldönti, hogy a vizsgált feltétel teljesül-e.
     *
     * @param key a művelethez átadott {@code key} érték
     * @return igaz, ha a vizsgált feltétel teljesül; egyébként hamis
     */
    private boolean isSensitiveKey(String key) {
        String k = key == null ? "" : key;
        return java.util.regex.Pattern.compile("password|secret|token|signature",
                java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.UNICODE_CASE).matcher(k).find();
    }


    /**
     * A bemeneti struktúrából biztonságosan kiolvassa a művelethez szükséges értéket, és hiányzó adat esetén a metódus szerinti fallbacket alkalmazza.
     *
     * @param node a művelethez átadott {@code node} érték
     * @param names a művelethez átadott {@code names} érték
     * @return a művelet eredménye
     */
    private String firstText(JsonNode node, String... names) {
        if (node == null) return null;
        for (String name : names) {
            JsonNode v = node.get(name);
            if (v != null && !v.isNull()) return v.asText();
        }
        return null;
    }

    /**
     * A bemeneti struktúrából biztonságosan kiolvassa a művelethez szükséges értéket, és hiányzó adat esetén a metódus szerinti fallbacket alkalmazza.
     *
     * @param node a művelethez átadott {@code node} érték
     * @param defaultValue a művelethez átadott {@code defaultValue} érték
     * @param names a művelethez átadott {@code names} érték
     * @return a művelet eredménye
     */
    private String firstTextOr(JsonNode node, String defaultValue, String... names) {
        String value = firstText(node, names);
        return value == null ? defaultValue : value;
    }

    /**
     * A(z) {@code base} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param baseUrl a művelethez átadott {@code baseUrl} érték
     * @return a művelet eredménye
     */
    private String base(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) throw new IllegalStateException("NAV baseUrl nincs beállítva");
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
