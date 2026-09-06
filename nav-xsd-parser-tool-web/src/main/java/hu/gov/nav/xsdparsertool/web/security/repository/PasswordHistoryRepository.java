package hu.gov.nav.xsdparsertool.web.security.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import hu.gov.nav.xsdparsertool.web.security.entity.PasswordHistoryEntity;

/**
 * A perzisztens adatok elérését biztosító repository szerződés.
 *
 * <p>A {@code PasswordHistoryRepository} interfész a web modul biztonsági és jogosultságkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public interface PasswordHistoryRepository extends JpaRepository<PasswordHistoryEntity, Long> {
    /**
     * A {@code findByUserIdOrderByCreatedAtDesc} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @param userId a célobjektum vagy erőforrás azonosítója
     * @return a művelet eredményeként előállított elemek listája
     */
    List<PasswordHistoryEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
}
