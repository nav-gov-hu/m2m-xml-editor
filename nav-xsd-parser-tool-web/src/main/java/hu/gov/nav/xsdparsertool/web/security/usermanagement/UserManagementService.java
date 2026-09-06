package hu.gov.nav.xsdparsertool.web.security.usermanagement;

import hu.gov.nav.xsdparsertool.web.support.RepositoryAccess;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hu.gov.nav.xsdparsertool.web.security.entity.AppRoleEntity;
import hu.gov.nav.xsdparsertool.web.security.entity.AppUserEntity;
import hu.gov.nav.xsdparsertool.web.security.repository.AppRoleRepository;
import hu.gov.nav.xsdparsertool.web.security.repository.AppUserRepository;
import hu.gov.nav.xsdparsertool.web.security.service.CurrentUserService;
import hu.gov.nav.xsdparsertool.web.security.service.PasswordPolicyService;
import hu.gov.nav.xsdparsertool.web.security.usermanagement.dto.RoleDto;
import hu.gov.nav.xsdparsertool.web.security.usermanagement.dto.UserDto;
import hu.gov.nav.xsdparsertool.web.security.usermanagement.dto.UserSaveRequest;

/**
 * A kapcsolódó webes üzleti vagy alkalmazási folyamatokat összefogó szolgáltatás.
 *
 * <p>A {@code UserManagementService} osztály a web modul biztonsági és jogosultságkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Service
public class UserManagementService {
    private final AppUserRepository users;
    private final AppRoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;
    private final PasswordPolicyService passwordPolicyService;

    /**
     * Létrehozza a {@code UserManagementService} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param users a művelet felhasználói kontextusa vagy felhasználóneve
     * @param roles a művelet bemeneti {@code roles} értéke
     * @param passwordEncoder a művelet bemeneti {@code passwordEncoder} értéke
     * @param currentUserService a művelet felhasználói kontextusa vagy felhasználóneve
     * @param passwordPolicyService a művelet bemeneti {@code passwordPolicyService} értéke
     */
    public UserManagementService(AppUserRepository users, AppRoleRepository roles, PasswordEncoder passwordEncoder,
            CurrentUserService currentUserService, PasswordPolicyService passwordPolicyService) {
        this.users = users; this.roles = roles; this.passwordEncoder = passwordEncoder;
        this.currentUserService = currentUserService; this.passwordPolicyService = passwordPolicyService;
    }

    /**
     * A {@code list} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a művelet eredményeként előállított elemek listája
     */
    @Transactional(readOnly = true)
    public List<UserDto> list() {
        return RepositoryAccess.findAll(users).stream().sorted(Comparator.comparing(AppUserEntity::getUsername, String.CASE_INSENSITIVE_ORDER))
                .map(this::toDto).toList();
    }

    /**
     * A {@code get} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param id a célobjektum vagy erőforrás azonosítója
     * @return a feloldott vagy lekért érték
     */
    @Transactional(readOnly = true)
    public UserDto get(Long id) { return toDto(find(id)); }

    /**
     * A {@code listRoles} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @return a művelet eredményeként előállított elemek listája
     */
    @Transactional(readOnly = true)
    public List<RoleDto> listRoles() {
        return RepositoryAccess.findAll(roles).stream().sorted(Comparator.comparing(AppRoleEntity::getRoleCode))
                .map(r -> new RoleDto(r.getId(), r.getRoleCode(), r.getRoleName())).toList();
    }

    /**
     * Az első telepítési admin létrehozása a setup minimális, 8 karakteres komplexitási szabályával.
     * A normál felhasználókezelés továbbra is a konfigurált rendszerjelszó-szabályzatot használja.
     *
     * @param request a kezdő admin adatai
     * @return a létrehozott admin
     */
    @Transactional
    public UserDto createInitialAdmin(UserSaveRequest request) {
        if (users.count() != 0) throw new IllegalStateException("Kezdő admin csak üres felhasználói adatbázisban hozható létre.");
        String username = required(request.username(), "A felhasználónév megadása kötelező.");
        String password = required(request.password(), "A kezdő admin jelszavának megadása kötelező.");
        validateInitialAdminPassword(password);
        AppUserEntity user = new AppUserEntity();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setCreatedAt(LocalDateTime.now());
        user.setCreatedBy("setup");
        apply(user, request, true);
        return toDto(users.save(user));
    }

    /** Ellenőrzi a setup 8 karakteres, négy karakterosztályt megkövetelő adminjelszó-szabályát.
     * @param password az ellenőrzendő jelszó
     */
    private void validateInitialAdminPassword(String password) {
        if (password.length() < 8
                || password.chars().noneMatch(Character::isLowerCase)
                || password.chars().noneMatch(Character::isUpperCase)
                || password.chars().noneMatch(Character::isDigit)
                || password.chars().noneMatch(ch -> !Character.isLetterOrDigit(ch))) {
            throw new IllegalArgumentException("A kezdő admin jelszava legalább 8 karakteres legyen, és tartalmazzon kisbetűt, nagybetűt, számot és speciális karaktert.");
        }
    }

    /**
     * Új normál felhasználót hoz létre a konfigurált rendszerjelszó-szabályzat szerint.
     * @param request a létrehozandó felhasználó adatai
     * @return a létrehozott felhasználó
     */
    @Transactional
    public UserDto create(UserSaveRequest request) {
        String username = required(request.username(), "A felhasználónév megadása kötelező.");
        if (users.existsByUsernameIgnoreCase(username)) throw new IllegalArgumentException("A felhasználónév már létezik.");
        String password = required(request.password(), "Helyi felhasználónál a jelszó megadása kötelező.");
        passwordPolicyService.validateNewPassword(userPlaceholder(username), password);
        AppUserEntity user = new AppUserEntity();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setCreatedAt(LocalDateTime.now());
        user.setCreatedBy(actor());
        apply(user, request, true);
        return toDto(users.save(user));
    }

    /**
     * A {@code update} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param id a célobjektum vagy erőforrás azonosítója
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet feldolgozási eredménye
     */
    @Transactional
    public UserDto update(Long id, UserSaveRequest request) {
        AppUserEntity user = find(id);
        String username = required(request.username(), "A felhasználónév megadása kötelező.");
        users.findByUsernameIgnoreCase(username).filter(found -> !found.getId().equals(id))
                .ifPresent(found -> { throw new IllegalArgumentException("A felhasználónév már létezik."); });
        if (isCurrentUser(user) && Boolean.FALSE.equals(request.enabled()))
            throw new IllegalArgumentException("A saját felhasználó nem tiltható le.");
        user.setUsername(username);
        if (request.password() != null && !request.password().isBlank()) {
            passwordPolicyService.validateNewPassword(user, request.password());
            passwordPolicyService.rememberCurrentPassword(user);
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        apply(user, request, false);
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy(actor());
        return toDto(users.save(user));
    }

    /**
     * A {@code setEnabled} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param id a célobjektum vagy erőforrás azonosítója
     * @param enabled a művelet bemeneti {@code enabled} értéke
     * @return a művelet feldolgozási eredménye
     */
    @Transactional
    public UserDto setEnabled(Long id, boolean enabled) {
        AppUserEntity user = find(id);
        if (!enabled && isCurrentUser(user)) throw new IllegalArgumentException("A saját felhasználó nem tiltható le.");
        user.setEnabled(enabled); user.setUpdatedAt(LocalDateTime.now()); user.setUpdatedBy(actor());
        return toDto(users.save(user));
    }

    /**
     * A {@code apply} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param user a művelet felhasználói kontextusa vagy felhasználóneve
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @param creating a művelet bemeneti {@code creating} értéke
     */
    private void apply(AppUserEntity user, UserSaveRequest request, boolean creating) {
        user.setDisplayName(trimToNull(request.displayName()));
        user.setEmail(trimToNull(request.email()));
        user.setEnabled(request.enabled() == null || request.enabled());
        user.setPasswordChangeRequired(request.passwordChangeRequired() == null ? creating : request.passwordChangeRequired());
        Set<String> requested = request.roles() == null ? Set.of() : request.roles().stream()
                .filter(v -> v != null && !v.isBlank()).map(v -> v.trim().toUpperCase(Locale.ROOT)).collect(Collectors.toCollection(LinkedHashSet::new));
        if (requested.isEmpty()) throw new IllegalArgumentException("Legalább egy jogosultságot ki kell választani.");
        Set<AppRoleEntity> resolved = requested.stream().map(code -> roles.findByRoleCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Ismeretlen jogosultság: " + code))).collect(Collectors.toCollection(LinkedHashSet::new));
        user.setRoles(resolved);
    }

    /**
     * A {@code find} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param id a célobjektum vagy erőforrás azonosítója
     * @return a feloldott vagy lekért érték
     */
    private AppUserEntity find(Long id) { return RepositoryAccess.findById(users, id).orElseThrow(() -> new IllegalArgumentException("A felhasználó nem található.")); }
    /**
     * A {@code isCurrentUser} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @param user a művelet felhasználói kontextusa vagy felhasználóneve
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    private boolean isCurrentUser(AppUserEntity user) { String current = currentUserService.getCurrentUsername(); return current != null && current.equalsIgnoreCase(user.getUsername()); }
    /**
     * A {@code actor} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a művelet feldolgozási eredménye
     */
    private String actor() { String value = currentUserService.getCurrentUsername(); return value == null ? "system" : value; }
    /**
     * A {@code required} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param value a művelet bemeneti {@code value} értéke
     * @param message a művelet bemeneti {@code message} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String required(String value, String message) { String result = trimToNull(value); if (result == null) throw new IllegalArgumentException(message); return result; }
    /**
     * A {@code trimToNull} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param value a művelet bemeneti {@code value} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String trimToNull(String value) { if (value == null) return null; String result=value.trim(); return result.isEmpty()?null:result; }
    /**
     * A {@code userPlaceholder} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @param username a művelet felhasználói kontextusa vagy felhasználóneve
     * @return a művelet feldolgozási eredménye
     */
    private AppUserEntity userPlaceholder(String username) { AppUserEntity user = new AppUserEntity(); user.setUsername(username); return user; }
    /**
     * A {@code toDto} művelet előállítja a hívó réteg által használt reprezentációt.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param u a művelet bemeneti {@code u} értéke
     * @return a művelet feldolgozási eredménye
     */
    private UserDto toDto(AppUserEntity u) {
        List<String> roleCodes=u.getRoles().stream().map(AppRoleEntity::getRoleCode).sorted().toList();
        return new UserDto(u.getId(),u.getUsername(),u.getDisplayName(),u.getEmail(),u.isEnabled(),u.isPasswordChangeRequired(),u.getFailedLoginAttempts(),u.getLockedUntil(),roleCodes,u.getCreatedAt(),u.getCreatedBy(),u.getUpdatedAt(),u.getUpdatedBy());
    }
}
