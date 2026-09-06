package hu.gov.nav.xsdparsertool.web.security.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hu.gov.nav.xsdparsertool.web.security.entity.AppUserEntity;
import hu.gov.nav.xsdparsertool.web.security.repository.AppUserRepository;

/**
 * A kapcsolódó webes üzleti vagy alkalmazási folyamatokat összefogó szolgáltatás.
 *
 * <p>A {@code DatabaseUserDetailsService} osztály a web modul biztonsági és jogosultságkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    /**
     * Létrehozza a {@code DatabaseUserDetailsService} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param appUserRepository a művelet felhasználói kontextusa vagy felhasználóneve
     */
    public DatabaseUserDetailsService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    /**
     * A {@code loadUserByUsername} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @param username a művelet felhasználói kontextusa vagy felhasználóneve
     * @return a feloldott vagy lekért érték
     * @throws UsernameNotFoundException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUserEntity appUser = appUserRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Ismeretlen felhasznalo: " + username));


        List<SimpleGrantedAuthority> authorities = appUser.getRoles().stream()
                .map(role -> role.getRoleCode().startsWith("ROLE_")
                        ? role.getRoleCode()
                        : "ROLE_" + role.getRoleCode())
                .map(SimpleGrantedAuthority::new)
                .toList();

        return User.withUsername(appUser.getUsername())
                .password(appUser.getPasswordHash())
                .disabled(!appUser.isEnabled())
                .accountExpired(false)
                .accountLocked(appUser.getLockedUntil() != null && appUser.getLockedUntil().isAfter(LocalDateTime.now()))
                .credentialsExpired(false)
                .authorities(authorities)
                .build();
    }
}
