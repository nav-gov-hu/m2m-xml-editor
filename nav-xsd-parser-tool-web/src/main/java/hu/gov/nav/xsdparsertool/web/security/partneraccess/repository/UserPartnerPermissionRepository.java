package hu.gov.nav.xsdparsertool.web.security.partneraccess.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import hu.gov.nav.xsdparsertool.web.security.partneraccess.entity.UserPartnerPermissionEntity;
/**
 * A perzisztens adatok elérését biztosító repository szerződés.
 *
 * <p>A {@code UserPartnerPermissionRepository} interfész a web modul biztonsági és jogosultságkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public interface UserPartnerPermissionRepository extends JpaRepository<UserPartnerPermissionEntity,Long>{
 /**
  * A {@code findByUser_IdOrderByPartner_NameAsc} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
  * @param userId a célobjektum vagy erőforrás azonosítója
  * @return a művelet eredményeként előállított elemek listája
  */
 List<UserPartnerPermissionEntity> findByUser_IdOrderByPartner_NameAsc(Long userId);
 /**
  * A {@code existsByUser_IdAndPartner_Id} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
  *
  * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
  * @param userId a célobjektum vagy erőforrás azonosítója
  * @param partnerId a célobjektum vagy erőforrás azonosítója
  * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
  */
 boolean existsByUser_IdAndPartner_Id(Long userId,Long partnerId);
 /**
  * A {@code deleteByUser_IdAndPartner_Id} művelet lezárja, felszabadítja vagy eltávolítja a kijelölt erőforrást a vonatkozó szabályok szerint.
  *
  * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
  * @param userId a célobjektum vagy erőforrás azonosítója
  * @param partnerId a célobjektum vagy erőforrás azonosítója
  */
 void deleteByUser_IdAndPartner_Id(Long userId,Long partnerId);
}
