package hu.gov.nav.xsdparsertool.web.partner.repository;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import hu.gov.nav.xsdparsertool.web.partner.entity.PartnerEntity;
/**
 * A perzisztens adatok elérését biztosító repository szerződés.
 *
 * <p>A {@code PartnerRepository} interfész a web modul partnerkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public interface PartnerRepository extends JpaRepository<PartnerEntity,Long>{
 /**
  * A {@code existsByTaxNumberIgnoreCase} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param taxNumber a művelet bemeneti {@code taxNumber} értéke
  * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
  */
 boolean existsByTaxNumberIgnoreCase(String taxNumber);
 /**
  * A {@code findFirstByTaxNumberIgnoreCase} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param taxNumber a művelet bemeneti {@code taxNumber} értéke
  * @return a feloldott érték, vagy üres {@link java.util.Optional}, ha nincs alkalmazható találat
  */
 Optional<PartnerEntity> findFirstByTaxNumberIgnoreCase(String taxNumber);
 /**
  * A {@code suggest} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param q a művelet bemeneti {@code q} értéke
  * @param pageable a lapozási vagy mennyiségi korlátot meghatározó érték
  * @return a művelet eredményeként előállított elemek listája
  */
 @Query("select p from PartnerEntity p where p.active=true and (lower(p.name) like lower(concat('%',:q,'%')) or lower(p.taxNumber) like lower(concat('%',:q,'%'))) order by p.taxNumber,p.name")
 List<PartnerEntity> suggest(@Param("q") String q, Pageable pageable);
 /**
  * A {@code findAllByOrderByNameAsc} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a művelet eredményeként előállított elemek listája
  */
 List<PartnerEntity> findAllByOrderByNameAsc();
}
