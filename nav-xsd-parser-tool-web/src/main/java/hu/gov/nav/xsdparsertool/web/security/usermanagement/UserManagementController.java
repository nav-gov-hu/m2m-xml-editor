package hu.gov.nav.xsdparsertool.web.security.usermanagement;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import hu.gov.nav.xsdparsertool.core.security.AuthorizationRules;
import hu.gov.nav.xsdparsertool.web.security.PasswordPolicyProperties;
import hu.gov.nav.xsdparsertool.web.security.usermanagement.dto.PasswordPolicyDto;
import hu.gov.nav.xsdparsertool.web.security.usermanagement.dto.RoleDto;
import hu.gov.nav.xsdparsertool.web.security.usermanagement.dto.UserDto;
import hu.gov.nav.xsdparsertool.web.security.usermanagement.dto.UserSaveRequest;

/**
 * A webes végpontokat kiszolgáló vezérlő, amely a HTTP-kéréseket a megfelelő alkalmazási szolgáltatásokhoz irányítja.
 *
 * <p>A {@code UserManagementController} osztály a web modul biztonsági és jogosultságkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@RestController
@RequestMapping("/api/users")
@PreAuthorize(AuthorizationRules.ADMIN_ONLY)
public class UserManagementController {
    private static final Pattern SAFE_USERNAME = Pattern.compile("^[\\p{L}\\p{N}._@\\/+\\-]{1,128}$");
    private static final Pattern SAFE_ROLE = Pattern.compile("^[A-Z0-9_]{1,64}$");
    private final UserManagementService service;
    private final PasswordPolicyProperties passwordPolicy;
    /**
     * Létrehozza a {@code UserManagementController} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param service a művelet bemeneti {@code service} értéke
     * @param passwordPolicy a művelet bemeneti {@code passwordPolicy} értéke
     */
    public UserManagementController(UserManagementService service, PasswordPolicyProperties passwordPolicy) { this.service = service; this.passwordPolicy = passwordPolicy; }
    /**
     * A {@code list} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a művelet eredményeként előállított elemek listája
     */
    @GetMapping public List<UserDto> list() { return service.list(); }
    /**
     * A {@code get} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param id a célobjektum vagy erőforrás azonosítója
     * @return a feloldott vagy lekért érték
     */
    @GetMapping("/{id}") public UserDto get(@PathVariable String id) { return service.get(parsePositiveId(id)); }
    /**
     * A {@code roles} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @return a művelet eredményeként előállított elemek listája
     */
    @GetMapping("/roles") public List<RoleDto> roles() { return service.listRoles(); }
    /**
     * A {@code passwordPolicy} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a művelet feldolgozási eredménye
     */
    @GetMapping("/password-policy") public PasswordPolicyDto passwordPolicy() { return new PasswordPolicyDto(passwordPolicy.getMinimumLength(), passwordPolicy.getMaximumLength(), passwordPolicy.getHistorySize(), passwordPolicy.getMaximumFailedAttempts(), passwordPolicy.getLockDuration().toMinutes(), false); }
    /**
     * A {@code create} művelet létrehozza vagy tartósítja a kért állapotváltozást.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet feldolgozási eredménye
     */
    @PostMapping public UserDto create(@RequestBody UserSaveRequest request) { return service.create(validated(request)); }
    /**
     * A {@code update} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param id a célobjektum vagy erőforrás azonosítója
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet feldolgozási eredménye
     */
    @PutMapping("/{id}") public UserDto update(@PathVariable String id, @RequestBody UserSaveRequest request) { return service.update(parsePositiveId(id), validated(request)); }
    /**
     * A {@code enabled} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param id a célobjektum vagy erőforrás azonosítója
     * @param body a művelet bemeneti {@code body} értéke
     * @return a művelet feldolgozási eredménye
     */
    @PatchMapping("/{id}/enabled") public UserDto enabled(@PathVariable String id, @RequestBody Map<String, Boolean> body) { return service.setEnabled(parsePositiveId(id), Boolean.TRUE.equals(body.get("enabled"))); }

    /**
     * A {@code parsePositiveId} művelet feldolgozza és normalizálja a bemeneti adatot a további feldolgozás számára.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param raw a művelet bemeneti {@code raw} értéke
     * @return a művelet feldolgozási eredménye
     */
    private static Long parsePositiveId(String raw) {
        if (raw == null || !raw.matches("[1-9][0-9]{0,18}")) throw new IllegalArgumentException("Érvénytelen felhasználó-azonosító.");
        try { return Long.valueOf(raw); } catch (NumberFormatException ex) { throw new IllegalArgumentException("Érvénytelen felhasználó-azonosító."); }
    }

    /**
     * A {@code validated} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>Az ellenőrzési eredményt a webes megjelenítés és a további üzleti döntések számára konzisztens formában állítja elő.</p>
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet feldolgozási eredménye
     */
    private static UserSaveRequest validated(UserSaveRequest request) {
        if (request == null) throw new IllegalArgumentException("A felhasználói adatok hiányoznak.");
        String username = requiredSafe(request.username(), SAFE_USERNAME, "Érvénytelen felhasználónév.");
        String displayName = optionalText(request.displayName(), 256, "Érvénytelen megjelenítési név.");
        String email = optionalText(request.email(), 320, "Érvénytelen e-mail cím.");
        if (request.password() != null && request.password().length() > 1024) throw new IllegalArgumentException("A jelszó túl hosszú.");
        Set<String> roles = new LinkedHashSet<>();
        if (request.roles() != null) {
            for (String role : request.roles()) {
                String value = requiredSafe(role == null ? null : role.trim(), SAFE_ROLE, "Érvénytelen jogosultság.");
                roles.add(value);
            }
        }
        return new UserSaveRequest(username, displayName, email, request.enabled(), request.passwordChangeRequired(), request.password(), Set.copyOf(roles));
    }

    /**
     * A {@code requiredSafe} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param raw a művelet bemeneti {@code raw} értéke
     * @param pattern a művelet bemeneti {@code pattern} értéke
     * @param message a művelet bemeneti {@code message} értéke
     * @return a művelet feldolgozási eredménye
     */
    private static String requiredSafe(String raw, Pattern pattern, String message) {
        String value = raw == null ? "" : raw.trim();
        if (!pattern.matcher(value).matches()) throw new IllegalArgumentException(message);
        return value;
    }

    /**
     * A {@code optionalText} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param raw a művelet bemeneti {@code raw} értéke
     * @param maxLength a művelet bemeneti {@code maxLength} értéke
     * @param message a művelet bemeneti {@code message} értéke
     * @return a művelet feldolgozási eredménye
     */
    private static String optionalText(String raw, int maxLength, String message) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.trim();
        if (value.length() > maxLength || value.chars().anyMatch(ch -> ch < 0x20 && ch != '\t')) throw new IllegalArgumentException(message);
        return value;
    }

    /**
     * A {@code badRequest} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param ex a művelet bemeneti {@code ex} értéke
     * @return a művelet feldolgozási eredménye
     */
    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,String>> badRequest(IllegalArgumentException ex) { return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage())); }
}
