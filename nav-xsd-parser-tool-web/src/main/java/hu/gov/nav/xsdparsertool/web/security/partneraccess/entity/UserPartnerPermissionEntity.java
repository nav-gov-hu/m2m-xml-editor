package hu.gov.nav.xsdparsertool.web.security.partneraccess.entity;

import java.time.LocalDateTime;
import hu.gov.nav.xsdparsertool.web.partner.entity.PartnerEntity;
import hu.gov.nav.xsdparsertool.web.security.entity.AppUserEntity;
import jakarta.persistence.*;

/**
 * A perzisztens állapot adatbázis-reprezentációját leíró entitás.
 *
 * <p>A {@code UserPartnerPermissionEntity} osztály a web modul biztonsági és jogosultságkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Entity
@Table(name="user_partner_permission", uniqueConstraints=@UniqueConstraint(name="uk_user_partner_permission", columnNames={"user_id","partner_id"}))
public class UserPartnerPermissionEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id", nullable=false) private AppUserEntity user;
 @ManyToOne(fetch=FetchType.EAGER, optional=false) @JoinColumn(name="partner_id", nullable=false) private PartnerEntity partner;
 @Column(name="created_at", nullable=false) private LocalDateTime createdAt=LocalDateTime.now();
 @Column(name="created_by", length=255) private String createdBy;
 /**
  * A {@code getId} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public Long getId(){return id;}  /**
  * A {@code getUser} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
  * @return a feloldott vagy lekért érték
  */
 public AppUserEntity getUser(){return user;}  /**
  * A {@code setUser} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
  * @param v a művelet bemeneti {@code v} értéke
  */
 public void setUser(AppUserEntity v){user=v;}
 /**
  * A {@code getPartner} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public PartnerEntity getPartner(){return partner;}  /**
  * A {@code setPartner} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param v a művelet bemeneti {@code v} értéke
  */
 public void setPartner(PartnerEntity v){partner=v;}
 /**
  * A {@code getCreatedAt} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public LocalDateTime getCreatedAt(){return createdAt;}  /**
  * A {@code setCreatedAt} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param v a művelet bemeneti {@code v} értéke
  */
 public void setCreatedAt(LocalDateTime v){createdAt=v;}
 /**
  * A {@code getCreatedBy} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getCreatedBy(){return createdBy;}  /**
  * A {@code setCreatedBy} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param v a művelet bemeneti {@code v} értéke
  */
 public void setCreatedBy(String v){createdBy=v;}
}
