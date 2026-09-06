package hu.gov.nav.xsdparsertool.web.security.partneraccess.entity;

import java.time.LocalDateTime;
import hu.gov.nav.xsdparsertool.web.security.entity.AppUserEntity;
import jakarta.persistence.*;

/**
 * A perzisztens állapot adatbázis-reprezentációját leíró entitás.
 *
 * <p>A {@code UserTaxPermissionRuleEntity} osztály a web modul biztonsági és jogosultságkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Entity
@Table(name="user_tax_permission_rule")
public class UserTaxPermissionRuleEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id", nullable=false) private AppUserEntity user;
 @Column(name="rule_type", nullable=false, length=10) private String ruleType="ALLOW";
 @Column(name="tax_number", length=8) private String taxNumber;
 @Column(name="vat_code", length=1) private String vatCode;
 @Column(name="county_code", length=2) private String countyCode;
 @Column(name="sort_order", nullable=false) private Integer sortOrder=0;
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
  * A {@code getRuleType} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getRuleType(){return ruleType;}  /**
  * A {@code setRuleType} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param v a művelet bemeneti {@code v} értéke
  */
 public void setRuleType(String v){ruleType=v;}
 /**
  * A {@code getTaxNumber} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getTaxNumber(){return taxNumber;}  /**
  * A {@code setTaxNumber} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param v a művelet bemeneti {@code v} értéke
  */
 public void setTaxNumber(String v){taxNumber=v;}
 /**
  * A {@code getVatCode} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getVatCode(){return vatCode;}  /**
  * A {@code setVatCode} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param v a művelet bemeneti {@code v} értéke
  */
 public void setVatCode(String v){vatCode=v;}
 /**
  * A {@code getCountyCode} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getCountyCode(){return countyCode;}  /**
  * A {@code setCountyCode} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param v a művelet bemeneti {@code v} értéke
  */
 public void setCountyCode(String v){countyCode=v;}
 /**
  * A {@code getSortOrder} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public Integer getSortOrder(){return sortOrder;}  /**
  * A {@code setSortOrder} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param v a művelet bemeneti {@code v} értéke
  */
 public void setSortOrder(Integer v){sortOrder=v;}
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
