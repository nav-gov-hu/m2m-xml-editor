package hu.gov.nav.xsdparsertool.web.security.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import hu.gov.nav.xsdparsertool.web.security.entity.AppUserEntity;

/**
 * A perzisztens adatok elérését biztosító repository szerződés.
 *
 * <p>A {@code AppUserRepository} interfész a web modul biztonsági és jogosultságkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public interface AppUserRepository extends JpaRepository<AppUserEntity, Long> {
    /**
     * A {@code findByUsernameIgnoreCase} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @param username a művelet felhasználói kontextusa vagy felhasználóneve
     * @return a feloldott érték, vagy üres {@link java.util.Optional}, ha nincs alkalmazható találat
     */
    Optional<AppUserEntity> findByUsernameIgnoreCase(String username);
    /**
     * A {@code existsByUsernameIgnoreCase} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @param username a művelet felhasználói kontextusa vagy felhasználóneve
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    boolean existsByUsernameIgnoreCase(String username);
}
