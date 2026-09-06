package hu.gov.nav.xsdparsertool.web.githubupdater.api;

import hu.gov.nav.xsdparsertool.web.githubupdater.dto.GitHubProxySettingsDto;
import hu.gov.nav.xsdparsertool.web.githubupdater.service.GitHubProxySettingsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * A GitHub HTTP-proxy és truststore beállításainak REST vezérlője. A bejövő adatokat a perzisztálás előtt hossz-, karakter- és tartománykorlátokkal tisztítja, a tényleges tárolást pedig a {@link hu.gov.nav.xsdparsertool.web.githubupdater.service.GitHubProxySettingsService} szolgáltatásra bízza.
 */
@RestController
@RequestMapping("/api/github-proxy-settings")
public class GitHubProxySettingsController {
    private final GitHubProxySettingsService service;
    /**
     * Létrehozza a(z) {@code GitHubProxySettingsController} példányt a működéshez szükséges kezdeti állapottal és függőségekkel.
     *
     * @param service a művelethez átadott {@code service} érték
     */
    public GitHubProxySettingsController(GitHubProxySettingsService service) { this.service = service; }
    /**
     * Visszaadja a(z) érték aktuális értékét.
     *
     * @return a művelet eredménye
     */
    @GetMapping public GitHubProxySettingsDto get() { return service.get(); }
    /**
     * Elmenti a megadott GitHub proxy-konfigurációt; a törlési jelzők külön szabályozzák a tárolt titkok eltávolítását.
     *
     * @param dto a REST rétegből érkező proxybeállítás DTO
     * @return a művelet eredménye
     */
    @PostMapping public GitHubProxySettingsDto save(@Valid @RequestBody GitHubProxySettingsDto dto) { return service.save(validated(dto)); }

    /**
     * A REST kérésből új, biztonságos DTO-t épít. A szöveges értékeket normalizálja, a portot és a truststore-típust ellenőrzi, a titkokat pedig külön korlátozott feldolgozással veszi át; az eredeti kérésobjektumot nem módosítja.
     *
     * @param input a validálandó bemeneti DTO
     * @return a művelet eredménye
     */
    private static GitHubProxySettingsDto validated(GitHubProxySettingsDto input) {
        if (input == null) throw new IllegalArgumentException("A GitHub proxy beállítások hiányoznak.");
        GitHubProxySettingsDto out = new GitHubProxySettingsDto();
        out.setEnabled(input.isEnabled());
        out.setProxyUrl(optionalText(input.getProxyUrl(), 2048));
        Integer port = input.getProxyPort();
        if (port != null && (port < 1 || port > 65535)) throw new IllegalArgumentException("Érvénytelen proxy port.");
        out.setProxyPort(port);
        out.setUsername(optionalText(input.getUsername(), 256));
        out.setPassword(secret(input.getPassword()));
        out.setClearPassword(input.isClearPassword());
        out.setSslVerificationDisabled(input.isSslVerificationDisabled());
        out.setTrustStorePath(optionalText(input.getTrustStorePath(), 2048));
        out.setTrustStorePassword(secret(input.getTrustStorePassword()));
        out.setClearTrustStorePassword(input.isClearTrustStorePassword());
        String type = optionalText(input.getTrustStoreType(), 16);
        if (type != null && !(type.equalsIgnoreCase("JKS") || type.equalsIgnoreCase("PKCS12"))) throw new IllegalArgumentException("Nem támogatott truststore típus.");
        out.setTrustStoreType(type);
        return out;
    }

    /**
     * Opcionális proxy-konfigurációs szöveget normalizál. Az üres értéket {@code null}-ra alakítja, eltávolítja a tiltott vezérlőkaraktereket, és eltérés vagy túl hosszú érték esetén elutasítja a bemenetet.
     *
     * @param raw a nyers bemeneti szöveg
     * @param max a megengedett maximális hossz
     * @return a művelet eredménye
     */
    private static String optionalText(String raw, int max) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.trim();
        String sanitized = value.replaceAll("[\r\n\u0000]", "");
        if (!sanitized.equals(value) || sanitized.length() > max) throw new IllegalArgumentException("Érvénytelen proxy paraméter.");
        return sanitized;
    }

    /**
     * Titokként kezelt bejövő értéket ellenőriz. A NUL karaktert és a megengedettnél hosszabb értéket elutasítja, de a jelszó egyéb karaktereit nem trimeli, hogy a titok jelentése ne változzon.
     *
     * @param raw a nyers bemeneti szöveg
     * @return a művelet eredménye
     */
    private static String secret(String raw) {
        if (raw == null) return null;
        String sanitized = raw.replace("\0", "");
        if (!sanitized.equals(raw) || sanitized.length() > 4096) throw new IllegalArgumentException("Érvénytelen titokérték.");
        return sanitized;
    }
}
