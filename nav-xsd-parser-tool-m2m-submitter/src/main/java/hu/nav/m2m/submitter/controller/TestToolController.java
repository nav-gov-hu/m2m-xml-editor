package hu.nav.m2m.submitter.controller;

import hu.nav.m2m.submitter.config.NavM2mProperties;
import hu.nav.m2m.submitter.service.M2mSignatureService;
import hu.nav.m2m.submitter.service.RuntimeSignatureKeyService;
import hu.nav.m2m.submitter.util.Sha256Util;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Diagnosztikai REST végpontokat biztosít konfiguráció-, hash- és aláírásellenőrzéshez; célja a NAV M2M integráció technikai hibakeresésének támogatása.
 */
@RestController
@RequestMapping("/api/test-tool")
@Tag(name = "NAV M2M teszt eszköz", description = "Konfiguráció, hash és aláírás kalkulátor ellenőrző végpontok")
@PreAuthorize("hasRole('ADMIN')")
public class TestToolController {
    private final NavM2mProperties properties;
    private final M2mSignatureService signatureService;
    private final RuntimeSignatureKeyService runtimeSignatureKeyService;

    /**
     * Létrehozza a(z) {@code TestToolController} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
     *
     * @param properties az M2M külső konfiguráció
     * @param signatureService a művelethez átadott {@code signatureService} érték
     * @param runtimeSignatureKeyService a művelethez átadott {@code runtimeSignatureKeyService} érték
     */
    public TestToolController(NavM2mProperties properties, M2mSignatureService signatureService, RuntimeSignatureKeyService runtimeSignatureKeyService) {
        this.properties = properties;
        this.signatureService = signatureService;
        this.runtimeSignatureKeyService = runtimeSignatureKeyService;
    }

    /**
     * A(z) {@code config} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @return a művelet eredménye
     */
    @GetMapping("/config")
    @Operation(summary = "Aktuális NAV M2M konfiguráció megjelenítése")
    public Map<String, Object> config() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("commonBaseUrl", properties.getEndpoints().getCommonBaseUrl());
        root.put("bizonylatBaseUrl", properties.getEndpoints().getBizonylatBaseUrl());
        root.put("tokenPath", properties.getEndpoints().getTokenPath());
        root.put("noncePath", properties.getEndpoints().getNoncePath());
        root.put("activationPath", properties.getEndpoints().getActivationPath());
        root.put("fileUploadPath", properties.getEndpoints().getFileUploadPath());
        root.put("fileStatusPath", properties.getEndpoints().getFileStatusPath());
        root.put("bizonylatPath", properties.getEndpoints().getBizonylatPath());
        root.put("clientId", properties.getAuth().getClientId());
        root.put("clientSecretConfigured", configured(properties.getAuth().getClientSecret()));
        root.put("username", properties.getAuth().getUsername());
        root.put("passwordConfigured", configured(properties.getAuth().getPassword()));
        root.put("signatureKeyFirstPartConfigured", configured(properties.getSignature().getKeyFirstPart()));
        root.put("nonceConfigured", configured(properties.getSignature().getNonce()));
        root.put("signatureKeySecondPartConfigured", configured(runtimeSignatureKeyService.effectiveKeySecondPart()));
        root.put("configuredSignatureKeySecondPartConfigured", configured(properties.getSignature().getKeySecondPart()));
        root.put("runtimeSignatureKeySecondPartConfigured", configured((String) runtimeSignatureKeyService.snapshot().get("runtimeKeySecondPart")));
        root.put("signatureKeySecondPartSource", runtimeSignatureKeyService.effectiveSource());
        root.put("signatureKeyConfigured", runtimeSignatureKeyService.effectiveKeySecondPart() != null && !runtimeSignatureKeyService.effectiveKeySecondPart().isBlank());
        root.put("nonceUsedAsKeySecondPart", properties.getSignature().getNonce() != null && properties.getSignature().getNonce().equals(runtimeSignatureKeyService.effectiveKeySecondPart()));
        root.put("testTaxNumber", properties.getTaxpayer().getTestTaxNumber());
        root.put("realTaxNumber", properties.getTaxpayer().getRealTaxNumber());
        root.put("storageDirectory", properties.getStorageDirectory());
        root.put("maxInMemoryBizonylatApiBytes", properties.getMaxInMemoryBizonylatApiBytes());
        return root;
    }

    /**
     * A(z) {@code hashFile} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @return igaz, ha a vizsgált feltétel teljesül; egyébként hamis
     * @throws Exception ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    @PostMapping(value = "/hash-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Fájl SHA-256 hash számítása")
    public Map<String, Object> hashFile(@RequestPart("file") MultipartFile file) throws Exception {
        try (InputStream in = file.getInputStream()) {
            String hex = Sha256Util.sha256Hex(in);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("fileName", file.getOriginalFilename());
            result.put("size", file.getSize());
            result.put("sha256Hex", hex);
            result.put("sha256HexUpper", hex.toUpperCase(java.util.Locale.ROOT));
            return result;
        }
    }

    /**
     * A(z) {@code signature} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param request a REST vagy szolgáltatási művelet bemeneti kérése
     * @return a művelet eredménye
     */
    @PostMapping("/signature")
    @Operation(summary = "NAV M2M aláírás kalkulátor")
    public Map<String, Object> signature(@RequestBody Map<String, String> request) {
        String messageId = request.getOrDefault("messageId", UUID.randomUUID().toString());
        String operationData = request.getOrDefault("operationData", "");
        M2mSignatureService.SignatureDebug debug = signatureService.createSignatureDebug(messageId, operationData);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("messageId", messageId);
        result.put("operationData", operationData);
        result.put("timestamp", debug.timestamp());
        result.put("signatureKeyFirstPartConfigured", configured(debug.keyFirstPart()));
        result.put("nonceConfigured", configured(debug.nonce()));
        result.put("signatureKeySecondPartConfigured", configured(debug.keySecondPart()));
        result.put("configuredSignatureKeySecondPartConfigured", configured(debug.configuredKeySecondPart()));
        result.put("signatureKeySecondPartSource", debug.keySecondPartSource());
        result.put("signatureKeyConfigured", configured(debug.signatureKey()));
        result.put("nonceUsedAsKeySecondPart", debug.nonceUsedAsKeySecondPart());
        result.put("signatureKeyWarning", debug.signatureKeyWarning());
        result.put("signatureBaseSha256", Sha256Util.sha256Hex(debug.signatureBase().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        result.put("note", "A diagnosztikai végpont nem ad vissza titkot vagy felhasználható aláírásértéket.");
        return result;
    }
    /**
     * Megvizsgálja, hogy az adott konfigurációs érték ténylegesen használható-e.
     *
     * @param value a feldolgozandó érték
     * @return igaz, ha a vizsgált feltétel teljesül; egyébként hamis
     */
    private static boolean configured(String value) {
        return value != null && !value.isBlank();
    }
}
