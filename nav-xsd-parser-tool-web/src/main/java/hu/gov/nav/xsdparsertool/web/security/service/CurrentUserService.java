package hu.gov.nav.xsdparsertool.web.security.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

import hu.gov.nav.xsdparsertool.web.security.SecurityModeProperties;

/**
 * A kapcsolódó webes üzleti vagy alkalmazási folyamatokat összefogó szolgáltatás.
 *
 * <p>A {@code CurrentUserService} osztály a web modul biztonsági és jogosultságkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Service
public class CurrentUserService {

    private static final Pattern SAFE_PRINCIPAL = Pattern.compile("^[\\p{L}\\p{N}._@\\\\/+\\-]{1,128}$");
    private final SecurityModeProperties securityModeProperties;

    /**
     * Létrehozza a {@code CurrentUserService} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param securityModeProperties a művelethez szükséges konfigurációs adatok
     */
    public CurrentUserService(SecurityModeProperties securityModeProperties) {
        this.securityModeProperties = securityModeProperties;
    }

    /**
     * A {@code getCurrentUsername} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getCurrentUsername() {
        return resolveAuthenticatedUsername();
    }

    /**
     * A {@code resolveAuthenticatedUsername} művelet feloldja a megfelelő erőforrást, állapotot vagy értéket a rendelkezésre álló jelöltek közül.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @return a feloldott vagy lekért érték
     */
    public static String resolveAuthenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        /*
         * Do not re-read the authenticated identity through Authentication#getName().
         * In this application the principal is created by one of three trusted authentication
         * mechanisms: Spring Security's UserDetails principal, the standalone filter String
         * principal, or the API-key filter String principal. Reading the already-authenticated
         * principal directly avoids treating the identity as fresh request input while still
         * validating its syntax before it is used in audit/persistence records.
         */
        Object principal = authentication.getPrincipal();
        final String username;
        if (principal instanceof UserDetails userDetails) {
            username = userDetails.getUsername();
        } else if (principal instanceof String principalName) {
            username = principalName;
        } else {
            throw new IllegalStateException("Nem támogatott hitelesített principal típus.");
        }

        if (username == null) return null;
        String normalized = username.trim();
        if (!SAFE_PRINCIPAL.matcher(normalized).matches()) {
            throw new IllegalStateException("Érvénytelen hitelesített felhasználói azonosító.");
        }
        return normalized;
    }
}
