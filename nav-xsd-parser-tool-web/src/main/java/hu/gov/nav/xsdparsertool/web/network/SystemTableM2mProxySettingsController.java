package hu.gov.nav.xsdparsertool.web.network;

import hu.nav.m2m.submitter.dto.ProxySettingsDto;
import hu.nav.m2m.submitter.dto.ProxyTestRequest;
import hu.nav.m2m.submitter.dto.ProxyTestResponse;
import hu.nav.m2m.submitter.service.ProxyConnectionTestService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A webalkalmazás központi M2M proxy- és TLS-konfigurációjának REST végpontja.
 */
@RestController
@RequestMapping("/api/m2m-proxy-settings")
public class SystemTableM2mProxySettingsController {
    private final SystemTableM2mProxySettingsService settingsService;
    private final ProxyConnectionTestService testService;

    /**
     * Létrehozza a központi M2M proxy/TLS REST vezérlőt.
     *
     * @param settingsService központi konfigurációs szolgáltatás
     * @param testService M2M hálózati kapcsolatpróba szolgáltatás
     */
    public SystemTableM2mProxySettingsController(SystemTableM2mProxySettingsService settingsService,
                                                 ProxyConnectionTestService testService) {
        this.settingsService = settingsService;
        this.testService = testService;
    }

    /**
     * Visszaadja a webalkalmazás által ténylegesen használt M2M proxy/TLS konfigurációt.
     *
     * @return az aktuális konfiguráció
     */
    @GetMapping
    public ProxySettingsDto get() {
        return settingsService.get();
    }

    /**
     * Elmenti a validált M2M proxy/TLS konfigurációt a központi tárolókba.
     *
     * @param dto a mentendő konfiguráció
     * @return a mentés utáni maszkolt konfiguráció
     */
    @PostMapping
    public ProxySettingsDto save(@Valid @RequestBody ProxySettingsDto dto) {
        return settingsService.save(validated(dto));
    }

    /**
     * A megadott M2M proxy/TLS beállításokkal kapcsolatpróbát hajt végre.
     *
     * @param request a tesztelendő beállítások
     * @return a kapcsolatpróba eredménye
     */
    @PostMapping("/test")
    public ProxyTestResponse test(@RequestBody ProxyTestRequest request) {
        return testService.test(request);
    }

    private static ProxySettingsDto validated(ProxySettingsDto input) {
        if (input == null) {
            throw new IllegalArgumentException("Az M2M proxy beállítások hiányoznak.");
        }
        ProxySettingsDto out = new ProxySettingsDto();
        out.setEnabled(input.isEnabled());
        out.setProxyUrl(optionalText(input.getProxyUrl(), 2048, "Érvénytelen proxy URL."));
        Integer port = input.getProxyPort();
        if (port != null && (port < 1 || port > 65535)) {
            throw new IllegalArgumentException("Érvénytelen proxy port.");
        }
        out.setProxyPort(port);
        out.setUsername(optionalText(input.getUsername(), 256, "Érvénytelen proxy felhasználónév."));
        out.setPassword(secret(input.getPassword()));
        out.setClearPassword(input.isClearPassword());
        out.setSslVerificationDisabled(false);
        out.setTrustStorePath(optionalText(input.getTrustStorePath(), 2048, "Érvénytelen truststore útvonal."));
        out.setTrustStorePassword(secret(input.getTrustStorePassword()));
        out.setClearTrustStorePassword(input.isClearTrustStorePassword());
        String type = optionalText(input.getTrustStoreType(), 16, "Érvénytelen truststore típus.");
        if (type != null && !(type.equalsIgnoreCase("JKS") || type.equalsIgnoreCase("PKCS12"))) {
            throw new IllegalArgumentException("Nem támogatott truststore típus.");
        }
        out.setTrustStoreType(type);
        return out;
    }

    private static String optionalText(String raw, int max, String message) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        String sanitized = value.replaceAll("[\\r\\n\\u0000]", "");
        if (!sanitized.equals(value) || sanitized.length() > max) {
            throw new IllegalArgumentException(message);
        }
        return sanitized;
    }

    private static String secret(String raw) {
        if (raw == null) {
            return null;
        }
        String sanitized = raw.replace("\0", "");
        if (!sanitized.equals(raw) || sanitized.length() > 4096) {
            throw new IllegalArgumentException("Érvénytelen titokérték.");
        }
        return sanitized;
    }
}
