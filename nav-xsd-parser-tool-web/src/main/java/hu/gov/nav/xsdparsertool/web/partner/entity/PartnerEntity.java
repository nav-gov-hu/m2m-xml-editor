package hu.gov.nav.xsdparsertool.web.partner.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;

/**
 * A perzisztens állapot adatbázis-reprezentációját leíró entitás.
 *
 * <p>A {@code PartnerEntity} osztály a web modul partnerkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Entity
@Table(name = "partner", uniqueConstraints = @UniqueConstraint(name = "uk_partner_tax_number", columnNames = "tax_number"))
public class PartnerEntity {
 @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
 @Column(name="name",nullable=false,length=500) private String name;
 @Column(name="tax_number",nullable=false,length=50) private String taxNumber;
 @Column(name="community_tax_country",length=2) private String communityTaxCountry;
 @Column(name="community_tax_number",length=50) private String communityTaxNumber;
 @Column(name="registration_number",length=100) private String registrationNumber;
 @Column(name="email",length=255) private String email;
 @Column(name="phone",length=100) private String phone;
 @Column(name="fax",length=100) private String fax;
 @Column(name="permanent_postal_code",length=20) private String permanentPostalCode;
 @Column(name="permanent_city",length=255) private String permanentCity;
 @Column(name="permanent_public_place",length=255) private String permanentPublicPlace;
 @Column(name="permanent_public_place_type",length=100) private String permanentPublicPlaceType;
 @Column(name="permanent_house_number",length=50) private String permanentHouseNumber;
 @Column(name="permanent_building",length=50) private String permanentBuilding;
 @Column(name="permanent_staircase",length=50) private String permanentStaircase;
 @Column(name="permanent_floor",length=50) private String permanentFloor;
 @Column(name="permanent_door",length=50) private String permanentDoor;
 @Column(name="mailing_postal_code",length=20) private String mailingPostalCode;
 @Column(name="mailing_city",length=255) private String mailingCity;
 @Column(name="mailing_public_place",length=255) private String mailingPublicPlace;
 @Column(name="mailing_public_place_type",length=100) private String mailingPublicPlaceType;
 @Column(name="mailing_house_number",length=50) private String mailingHouseNumber;
 @Column(name="mailing_building",length=50) private String mailingBuilding;
 @Column(name="mailing_staircase",length=50) private String mailingStaircase;
 @Column(name="mailing_floor",length=50) private String mailingFloor;
 @Column(name="mailing_door",length=50) private String mailingDoor;
 @Column(name="contact_name",length=255) private String contactName;
 @Column(name="contact_phone",length=100) private String contactPhone;
 @Column(name="contact_email",length=255) private String contactEmail;
 @Column(name="bank_name",length=255) private String bankName;
 @Column(name="bank_account_number",length=100) private String bankAccountNumber;
 @Column(name="active",nullable=false) private Boolean active=Boolean.TRUE;
 @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
 @Column(name="updated_at") private LocalDateTime updatedAt;
 /**
  * A {@code prePersist} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  */
 @PrePersist void prePersist(){ if(createdAt==null) createdAt=LocalDateTime.now(); }
 /**
  * A {@code preUpdate} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  */
 @PreUpdate void preUpdate(){ updatedAt=LocalDateTime.now(); }
 /**
  * A {@code getId} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public Long getId(){return id;}  /**
  * A {@code setId} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param x a művelet bemeneti {@code x} értéke
  */
 public void setId(Long x){this.id=x;}
 /**
  * A {@code getName} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getName(){return name;}  /**
  * A {@code setName} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param x a művelet bemeneti {@code x} értéke
  */
 public void setName(String x){this.name=x;}
 /**
  * A {@code getTaxNumber} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getTaxNumber(){return taxNumber;}  /**
  * A {@code setTaxNumber} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param x a művelet bemeneti {@code x} értéke
  */
 public void setTaxNumber(String x){this.taxNumber=x;}
 /**
  * A {@code getCommunityTaxCountry} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getCommunityTaxCountry(){return communityTaxCountry;}  /**
  * A {@code setCommunityTaxCountry} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param x a művelet bemeneti {@code x} értéke
  */
 public void setCommunityTaxCountry(String x){this.communityTaxCountry=x;}
 /**
  * A {@code getCommunityTaxNumber} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getCommunityTaxNumber(){return communityTaxNumber;}  /**
  * A {@code setCommunityTaxNumber} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param x a művelet bemeneti {@code x} értéke
  */
 public void setCommunityTaxNumber(String x){this.communityTaxNumber=x;}
 /**
  * A {@code getRegistrationNumber} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getRegistrationNumber(){return registrationNumber;}  /**
  * A {@code setRegistrationNumber} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param x a művelet bemeneti {@code x} értéke
  */
 public void setRegistrationNumber(String x){this.registrationNumber=x;}
 /**
  * A {@code getEmail} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getEmail(){return email;}  /**
  * A {@code setEmail} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param x a művelet bemeneti {@code x} értéke
  */
 public void setEmail(String x){this.email=x;}
 /**
  * A {@code getPhone} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getPhone(){return phone;}  /**
  * A {@code setPhone} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param x a művelet bemeneti {@code x} értéke
  */
 public void setPhone(String x){this.phone=x;}
 /**
  * A {@code getFax} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getFax(){return fax;}  /**
  * A {@code setFax} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param x a művelet bemeneti {@code x} értéke
  */
 public void setFax(String x){this.fax=x;}
 /**
  * A {@code getPermanentPostalCode} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getPermanentPostalCode(){return permanentPostalCode;}  /**
  * A {@code setPermanentPostalCode} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param x a művelet bemeneti {@code x} értéke
  */
 public void setPermanentPostalCode(String x){this.permanentPostalCode=x;}
 /**
  * A {@code getPermanentCity} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getPermanentCity(){return permanentCity;}  /**
  * A {@code setPermanentCity} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param x a művelet bemeneti {@code x} értéke
  */
 public void setPermanentCity(String x){this.permanentCity=x;}
 /**
  * A {@code getPermanentPublicPlace} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getPermanentPublicPlace(){return permanentPublicPlace;}  /**
  * A {@code setPermanentPublicPlace} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param x a művelet bemeneti {@code x} értéke
  */
 public void setPermanentPublicPlace(String x){this.permanentPublicPlace=x;}
 /**
  * A {@code getPermanentPublicPlaceType} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getPermanentPublicPlaceType(){return permanentPublicPlaceType;}  /**
  * A {@code setPermanentPublicPlaceType} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param x a művelet bemeneti {@code x} értéke
  */
 public void setPermanentPublicPlaceType(String x){this.permanentPublicPlaceType=x;}
 /**
  * A {@code getPermanentHouseNumber} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getPermanentHouseNumber(){return permanentHouseNumber;}  /**
  * A {@code setPermanentHouseNumber} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param x a művelet bemeneti {@code x} értéke
  */
 public void setPermanentHouseNumber(String x){this.permanentHouseNumber=x;}
 /**
  * A {@code getPermanentBuilding} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getPermanentBuilding(){return permanentBuilding;}  /**
  * A {@code setPermanentBuilding} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param x a művelet bemeneti {@code x} értéke
  */
 public void setPermanentBuilding(String x){this.permanentBuilding=x;}
 /**
  * A {@code getPermanentStaircase} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getPermanentStaircase(){return permanentStaircase;}  /**
  * A {@code setPermanentStaircase} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param x a művelet bemeneti {@code x} értéke
  */
 public void setPermanentStaircase(String x){this.permanentStaircase=x;}
 /**
  * A {@code getPermanentFloor} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getPermanentFloor(){return permanentFloor;}  /**
  * A {@code setPermanentFloor} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param x a művelet bemeneti {@code x} értéke
  */
 public void setPermanentFloor(String x){this.permanentFloor=x;}
 /**
  * A {@code getPermanentDoor} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getPermanentDoor(){return permanentDoor;}  /**
  * A {@code setPermanentDoor} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param x a művelet bemeneti {@code x} értéke
  */
 public void setPermanentDoor(String x){this.permanentDoor=x;}
 /**
  * A {@code getMailingPostalCode} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getMailingPostalCode(){return mailingPostalCode;}  /**
  * A {@code setMailingPostalCode} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param x a művelet bemeneti {@code x} értéke
  */
 public void setMailingPostalCode(String x){this.mailingPostalCode=x;}
 /**
  * A {@code getMailingCity} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getMailingCity(){return mailingCity;}  /**
  * A {@code setMailingCity} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param x a művelet bemeneti {@code x} értéke
  */
 public void setMailingCity(String x){this.mailingCity=x;}
 /**
  * A {@code getMailingPublicPlace} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getMailingPublicPlace(){return mailingPublicPlace;}  /**
  * A {@code setMailingPublicPlace} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param x a művelet bemeneti {@code x} értéke
  */
 public void setMailingPublicPlace(String x){this.mailingPublicPlace=x;}
 /**
  * A {@code getMailingPublicPlaceType} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getMailingPublicPlaceType(){return mailingPublicPlaceType;}  /**
  * A {@code setMailingPublicPlaceType} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param x a művelet bemeneti {@code x} értéke
  */
 public void setMailingPublicPlaceType(String x){this.mailingPublicPlaceType=x;}
 /**
  * A {@code getMailingHouseNumber} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getMailingHouseNumber(){return mailingHouseNumber;}  /**
  * A {@code setMailingHouseNumber} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param x a művelet bemeneti {@code x} értéke
  */
 public void setMailingHouseNumber(String x){this.mailingHouseNumber=x;}
 /**
  * A {@code getMailingBuilding} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getMailingBuilding(){return mailingBuilding;}  /**
  * A {@code setMailingBuilding} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param x a művelet bemeneti {@code x} értéke
  */
 public void setMailingBuilding(String x){this.mailingBuilding=x;}
 /**
  * A {@code getMailingStaircase} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getMailingStaircase(){return mailingStaircase;}  /**
  * A {@code setMailingStaircase} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param x a művelet bemeneti {@code x} értéke
  */
 public void setMailingStaircase(String x){this.mailingStaircase=x;}
 /**
  * A {@code getMailingFloor} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getMailingFloor(){return mailingFloor;}  /**
  * A {@code setMailingFloor} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param x a művelet bemeneti {@code x} értéke
  */
 public void setMailingFloor(String x){this.mailingFloor=x;}
 /**
  * A {@code getMailingDoor} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getMailingDoor(){return mailingDoor;}  /**
  * A {@code setMailingDoor} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param x a művelet bemeneti {@code x} értéke
  */
 public void setMailingDoor(String x){this.mailingDoor=x;}
 /**
  * A {@code getContactName} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getContactName(){return contactName;}  /**
  * A {@code setContactName} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param x a művelet bemeneti {@code x} értéke
  */
 public void setContactName(String x){this.contactName=x;}
 /**
  * A {@code getContactPhone} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getContactPhone(){return contactPhone;}  /**
  * A {@code setContactPhone} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param x a művelet bemeneti {@code x} értéke
  */
 public void setContactPhone(String x){this.contactPhone=x;}
 /**
  * A {@code getContactEmail} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getContactEmail(){return contactEmail;}  /**
  * A {@code setContactEmail} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param x a művelet bemeneti {@code x} értéke
  */
 public void setContactEmail(String x){this.contactEmail=x;}
 /**
  * A {@code getBankName} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getBankName(){return bankName;}  /**
  * A {@code setBankName} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param x a művelet bemeneti {@code x} értéke
  */
 public void setBankName(String x){this.bankName=x;}
 /**
  * A {@code getBankAccountNumber} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getBankAccountNumber(){return bankAccountNumber;}  /**
  * A {@code setBankAccountNumber} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param x a művelet bemeneti {@code x} értéke
  */
 public void setBankAccountNumber(String x){this.bankAccountNumber=x;}
 /**
  * A {@code getActive} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
  */
 public Boolean getActive(){return active;}  /**
  * A {@code setActive} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param x a művelet bemeneti {@code x} értéke
  */
 public void setActive(Boolean x){this.active=x;}
 /**
  * A {@code getCreatedAt} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public LocalDateTime getCreatedAt(){return createdAt;}  /**
  * A {@code setCreatedAt} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param x a művelet bemeneti {@code x} értéke
  */
 public void setCreatedAt(LocalDateTime x){this.createdAt=x;}
 /**
  * A {@code getUpdatedAt} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public LocalDateTime getUpdatedAt(){return updatedAt;}  /**
  * A {@code setUpdatedAt} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param x a művelet bemeneti {@code x} értéke
  */
 public void setUpdatedAt(LocalDateTime x){this.updatedAt=x;}
}
