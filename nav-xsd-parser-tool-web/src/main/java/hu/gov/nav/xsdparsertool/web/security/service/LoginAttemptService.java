package hu.gov.nav.xsdparsertool.web.security.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hu.gov.nav.xsdparsertool.web.security.PasswordPolicyProperties;
import hu.gov.nav.xsdparsertool.web.security.repository.AppUserRepository;

/**
 * A kapcsolódó webes üzleti vagy alkalmazási folyamatokat összefogó szolgáltatás.
 *
 * <p>A {@code LoginAttemptService} osztály a web modul biztonsági és jogosultságkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Service
public class LoginAttemptService {
    private final AppUserRepository users;
    private final PasswordPolicyProperties properties;
    /**
     * Létrehozza a {@code LoginAttemptService} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param users a művelet felhasználói kontextusa vagy felhasználóneve
     * @param properties a művelethez szükséges konfigurációs adatok
     */
    public LoginAttemptService(AppUserRepository users, PasswordPolicyProperties properties) { this.users = users; this.properties = properties; }

    /**
     * A {@code success} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param username a művelet felhasználói kontextusa vagy felhasználóneve
     */
    @Transactional
    public void success(String username) {
        users.findByUsernameIgnoreCase(username).ifPresent(user -> {
            user.setFailedLoginAttempts(0); user.setLockedUntil(null); users.save(user);
        });
    }
    /**
     * A {@code failure} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param username a művelet felhasználói kontextusa vagy felhasználóneve
     */
    @Transactional
    public void failure(String username) {
        users.findByUsernameIgnoreCase(username).ifPresent(user -> {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= properties.getMaximumFailedAttempts()) {
                user.setLockedUntil(LocalDateTime.now().plus(properties.getLockDuration()));
                user.setFailedLoginAttempts(0);
            }
            users.save(user);
        });
    }
}
