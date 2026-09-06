package hu.gov.nav.xsdparsertool.web.certificate.api;

import hu.gov.nav.xsdparsertool.core.security.AuthorizationRules;
import hu.gov.nav.xsdparsertool.web.certificate.dto.*;
import hu.gov.nav.xsdparsertool.web.certificate.service.CertificateManagementService;
import hu.gov.nav.xsdparsertool.web.security.service.CurrentUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;

import java.util.List;
import java.util.regex.Pattern;

/**
 * A webes végpontokat kiszolgáló vezérlő, amely a HTTP-kéréseket a megfelelő alkalmazási szolgáltatásokhoz irányítja.
 *
 * <p>A {@code CertificateManagementController} osztály a web modul REST API területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@RestController
@RequestMapping("/api/admin/certificates")
@PreAuthorize(AuthorizationRules.ADMIN_ONLY)
public class CertificateManagementController {
    private static final Pattern SAFE_HOST = Pattern.compile("^[A-Za-z0-9.-]{1,253}$");
    private static final Pattern SAFE_ALIAS = Pattern.compile("^[\\p{L}\\p{N}._@ +\\-]{1,128}$");

    private final CertificateManagementService service;

    /**
     * Létrehozza a {@code CertificateManagementController} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param service a művelet bemeneti {@code service} értéke
     */
    public CertificateManagementController(CertificateManagementService service) {
        this.service = service;
    }

    /**
     * A {@code list} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a művelet eredményeként előállított elemek listája
     */
    @GetMapping
    public List<CertificateDto> list() { return service.list(); }

    /**
     * A {@code importFile} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param file a feldolgozásban részt vevő fájl vagy elérési út
     * @param password a művelet bemeneti {@code password} értéke
     * @return a művelet eredményeként előállított elemek listája
     * @throws Exception ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @PostMapping("/import")
    public List<CertificateDto> importFile(@RequestParam("file") MultipartFile file,
                                           @RequestParam(value = "password", required = false) String password) throws Exception {
        return service.importFile(file, password, user());
    }

    /**
     * A {@code remote} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet eredményeként előállított elemek listája
     * @throws Exception ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @PostMapping("/remote")
    public List<CertificateDto> remote(@Valid @RequestBody RemoteCertificateRequest request) throws Exception {
        if (request == null) throw new IllegalArgumentException("A tanúsítványkérés hiányzik.");
        String host = safeHost(request.host());
        int port = request.port() == null ? 443 : request.port();
        if (port < 1 || port > 65535) throw new IllegalArgumentException("Érvénytelen port.");
        String alias = safeAlias(request.alias());
        boolean importCertificate = Boolean.TRUE.equals(request.importCertificate());
        return service.fetchRemote(host, port, alias, importCertificate, user());
    }

    /**
     * A {@code delete} művelet lezárja, felszabadítja vagy eltávolítja a kijelölt erőforrást a vonatkozó szabályok szerint.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param id a célobjektum vagy erőforrás azonosítója
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        service.delete(parsePositiveId(id), user());
    }

    /**
     * A {@code user} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @return a művelet feldolgozási eredménye
     */
    private String user() {
        String username = CurrentUserService.resolveAuthenticatedUsername();
        return username == null ? "system" : username;
    }

    /**
     * A {@code parsePositiveId} művelet feldolgozza és normalizálja a bemeneti adatot a további feldolgozás számára.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param raw a művelet bemeneti {@code raw} értéke
     * @return a művelet feldolgozási eredménye
     */
    private static Long parsePositiveId(String raw) {
        if (raw == null || !raw.matches("[1-9][0-9]{0,18}")) throw new IllegalArgumentException("Érvénytelen azonosító.");
        try { return Long.valueOf(raw); } catch (NumberFormatException ex) { throw new IllegalArgumentException("Érvénytelen azonosító."); }
    }

    /**
     * A {@code safeHost} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param raw a művelet bemeneti {@code raw} értéke
     * @return a művelet feldolgozási eredménye
     */
    private static String safeHost(String raw) {
        String value = raw == null ? "" : raw.trim();
        String sanitized = value.replaceAll("[^A-Za-z0-9.-]", "");
        if (!sanitized.equals(value) || !SAFE_HOST.matcher(sanitized).matches()
                || sanitized.contains("..") || sanitized.startsWith(".") || sanitized.endsWith(".")) {
            throw new IllegalArgumentException("Érvénytelen hosztnév.");
        }
        return sanitized;
    }

    /**
     * A {@code safeAlias} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param raw a művelet bemeneti {@code raw} értéke
     * @return a művelet feldolgozási eredménye
     */
    private static String safeAlias(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.trim();
        String sanitized = value.replaceAll("[^\\p{L}\\p{N}._@ +\\-]", "");
        if (!sanitized.equals(value) || !SAFE_ALIAS.matcher(sanitized).matches()) {
            throw new IllegalArgumentException("Érvénytelen alias.");
        }
        return sanitized;
    }
}
