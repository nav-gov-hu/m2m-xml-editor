package hu.gov.nav.xsdparsertool.web.security.partneraccess.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import hu.gov.nav.xsdparsertool.web.security.partneraccess.entity.UserTaxPermissionRuleEntity;
/**
 * A perzisztens adatok elérését biztosító repository szerződés.
 *
 * <p>A {@code UserTaxPermissionRuleRepository} interfész a web modul biztonsági és jogosultságkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public interface UserTaxPermissionRuleRepository extends JpaRepository<UserTaxPermissionRuleEntity,Long>{
 /**
  * A {@code findByUser_IdOrderBySortOrderAscIdAsc} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
  * @param userId a célobjektum vagy erőforrás azonosítója
  * @return a művelet eredményeként előállított elemek listája
  */
 List<UserTaxPermissionRuleEntity> findByUser_IdOrderBySortOrderAscIdAsc(Long userId);
 /**
  * A {@code deleteByUser_Id} művelet lezárja, felszabadítja vagy eltávolítja a kijelölt erőforrást a vonatkozó szabályok szerint.
  *
  * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
  * @param userId a célobjektum vagy erőforrás azonosítója
  */
 void deleteByUser_Id(Long userId);
}
