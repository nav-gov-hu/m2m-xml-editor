package hu.gov.nav.xsdparsertool.web.security.service;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hu.gov.nav.xsdparsertool.web.security.PasswordPolicyProperties;
import hu.gov.nav.xsdparsertool.web.security.entity.AppUserEntity;
import hu.gov.nav.xsdparsertool.web.security.entity.PasswordHistoryEntity;
import hu.gov.nav.xsdparsertool.web.security.repository.PasswordHistoryRepository;

/**
 * A kapcsolódó webes üzleti vagy alkalmazási folyamatokat összefogó szolgáltatás.
 *
 * <p>A {@code PasswordPolicyService} osztály a web modul biztonsági és jogosultságkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Service
public class PasswordPolicyService {
    private final PasswordPolicyProperties properties;
    private final PasswordHistoryRepository history;
    private final PasswordEncoder encoder;

    /**
     * Létrehozza a {@code PasswordPolicyService} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param properties a művelethez szükséges konfigurációs adatok
     * @param history a művelet bemeneti {@code history} értéke
     * @param encoder a művelet bemeneti {@code encoder} értéke
     */
    public PasswordPolicyService(PasswordPolicyProperties properties, PasswordHistoryRepository history, PasswordEncoder encoder) {
        this.properties = properties; this.history = history; this.encoder = encoder;
    }

    /**
     * A {@code validateNewPassword} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>Az ellenőrzési eredményt a webes megjelenítés és a további üzleti döntések számára konzisztens formában állítja elő.</p>
     * @param user a művelet felhasználói kontextusa vagy felhasználóneve
     * @param password a művelet bemeneti {@code password} értéke
     */
    public void validateNewPassword(AppUserEntity user, String password) {
        if (password == null || password.length() < properties.getMinimumLength())
            throw new IllegalArgumentException("A jelszó legalább " + properties.getMinimumLength() + " karakter hosszú legyen.");
        if (password.length() > properties.getMaximumLength())
            throw new IllegalArgumentException("A jelszó legfeljebb " + properties.getMaximumLength() + " karakter hosszú lehet.");
        String normalized = normalize(password);
        if (properties.getForbiddenPasswords().stream().map(this::normalize).anyMatch(normalized::equals))
            throw new IllegalArgumentException("A megadott jelszó tiltott vagy túl gyakran használt jelszó.");
        if (user != null && user.getUsername() != null && normalized.contains(normalize(user.getUsername())))
            throw new IllegalArgumentException("A jelszó nem tartalmazhatja a felhasználónevet.");
        if (user != null && user.getPasswordHash() != null && encoder.matches(password, user.getPasswordHash()))
            throw new IllegalArgumentException("A jelenlegi jelszó nem használható újra.");
        if (user != null && user.getId() != null) {
            List<PasswordHistoryEntity> previous = history.findByUserIdOrderByCreatedAtDesc(user.getId());
            if (previous.stream().limit(properties.getHistorySize()).anyMatch(item -> encoder.matches(password, item.getPasswordHash())))
                throw new IllegalArgumentException("Az utolsó " + properties.getHistorySize() + " jelszó egyike nem használható újra.");
        }
    }

    /**
     * A {@code rememberCurrentPassword} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param user a művelet felhasználói kontextusa vagy felhasználóneve
     */
    @Transactional
    public void rememberCurrentPassword(AppUserEntity user) {
        if (user.getId() == null || user.getPasswordHash() == null) return;
        PasswordHistoryEntity item = new PasswordHistoryEntity();
        item.setUser(user); item.setPasswordHash(user.getPasswordHash()); item.setCreatedAt(LocalDateTime.now());
        history.save(item);
        List<PasswordHistoryEntity> all = history.findByUserIdOrderByCreatedAtDesc(user.getId());
        if (all.size() > properties.getHistorySize()) history.deleteAll(all.subList(properties.getHistorySize(), all.size()));
    }

    /**
     * A {@code normalize} művelet feldolgozza és normalizálja a bemeneti adatot a további feldolgozás számára.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param value a művelet bemeneti {@code value} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFKC).trim().toLowerCase(Locale.ROOT);
    }
}
