package hu.nav.m2m.submitter.controller;

import hu.nav.m2m.submitter.dto.ProxySettingsDto;
import hu.nav.m2m.submitter.dto.ProxyTestRequest;
import hu.nav.m2m.submitter.dto.ProxyTestResponse;
import hu.nav.m2m.submitter.service.ProxyConnectionTestService;
import hu.nav.m2m.submitter.service.ProxySettingsService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * REST vezérlő az M2M proxy- és truststore-beállítások lekérdezéséhez, mentéséhez és kapcsolatpróbájához.
 */
@RestController
@RequestMapping("/api/proxy-settings")
@Tag(name = "Proxy beállítások", description = "NAV HTTP kliens proxy beállításai H2 adatbázisban")
public class ProxySettingsController {
    private final ProxySettingsService service;
    private final ProxyConnectionTestService testService;

    /**
     * Létrehozza a(z) {@code ProxySettingsController} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
     *
     * @param service a művelethez átadott {@code service} érték
     * @param testService a művelethez átadott {@code testService} érték
     */
    public ProxySettingsController(ProxySettingsService service, ProxyConnectionTestService testService) {
        this.service = service;
        this.testService = testService;
    }

    /**
     * Lekéri a kért M2M erőforrást vagy aktuális konfigurációt.
     *
     * @return a művelet eredménye
     */
    @GetMapping
    @Operation(summary = "Aktuális proxy beállítások lekérdezése")
    public ProxySettingsDto get() {
        return service.get();
    }

    /**
     * Validálás után elmenti a megadott beállítást vagy domain állapotot.
     *
     * @param dto a művelethez átadott {@code dto} érték
     * @return a művelet eredménye
     */
    @PostMapping
    @Operation(summary = "Proxy beállítások mentése")
    public ProxySettingsDto save(@Valid @RequestBody ProxySettingsDto dto) {
        return service.save(validated(dto));
    }

    /**
     * Ellenőrzi a művelet kötelező előfeltételeit és inkonzisztens vagy nem engedélyezett állapot esetén kontrollált kivétellel megszakítja a feldolgozást.
     *
     * @param input a feldolgozandó bemenet
     * @return a művelet eredménye
     */
    private static ProxySettingsDto validated(ProxySettingsDto input) {
        if (input == null) throw new IllegalArgumentException("A proxy beállítások hiányoznak.");
        ProxySettingsDto out = new ProxySettingsDto();
        out.setEnabled(input.isEnabled());
        out.setProxyUrl(optionalText(input.getProxyUrl(), 2048, "Érvénytelen proxy URL."));
        Integer port = input.getProxyPort();
        if (port != null && (port < 1 || port > 65535)) throw new IllegalArgumentException("Érvénytelen proxy port.");
        out.setProxyPort(port);
        out.setUsername(optionalText(input.getUsername(), 256, "Érvénytelen proxy felhasználónév."));
        out.setPassword(secret(input.getPassword()));
        out.setClearPassword(input.isClearPassword());
        out.setSslVerificationDisabled(input.isSslVerificationDisabled());
        out.setTrustStorePath(optionalText(input.getTrustStorePath(), 2048, "Érvénytelen truststore útvonal."));
        out.setTrustStorePassword(secret(input.getTrustStorePassword()));
        out.setClearTrustStorePassword(input.isClearTrustStorePassword());
        String type = optionalText(input.getTrustStoreType(), 16, "Érvénytelen truststore típus.");
        if (type != null && !(type.equalsIgnoreCase("JKS") || type.equalsIgnoreCase("PKCS12"))) throw new IllegalArgumentException("Nem támogatott truststore típus.");
        out.setTrustStoreType(type);
        return out;
    }

    /**
     * A(z) {@code optionalText} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param raw a művelethez átadott {@code raw} érték
     * @param max a művelethez átadott {@code max} érték
     * @param message a művelethez átadott {@code message} érték
     * @return a művelet eredménye
     */
    private static String optionalText(String raw, int max, String message) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.trim();
        String sanitized = value.replaceAll("[\r\n\u0000]", "");
        if (!sanitized.equals(value) || sanitized.length() > max) throw new IllegalArgumentException(message);
        return sanitized;
    }

    /**
     * A(z) {@code secret} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param raw a művelethez átadott {@code raw} érték
     * @return a művelet eredménye
     */
    private static String secret(String raw) {
        if (raw == null) return null;
        String sanitized = raw.replace("\0", "");
        if (!sanitized.equals(raw) || sanitized.length() > 4096) throw new IllegalArgumentException("Érvénytelen titokérték.");
        return sanitized;
    }

    /**
     * A megadott vagy aktuális konfigurációval diagnosztikai kapcsolatpróbát hajt végre, majd strukturált eredményt ad vissza.
     *
     * @param request a REST vagy szolgáltatási művelet bemeneti kérése
     * @return a művelet eredménye
     */
    @PostMapping("/test")
    @Operation(summary = "Proxy kapcsolat tesztelése a megadott teszt URL-lel")
    public ProxyTestResponse test(@RequestBody ProxyTestRequest request) {
        return testService.test(request);
    }
}
