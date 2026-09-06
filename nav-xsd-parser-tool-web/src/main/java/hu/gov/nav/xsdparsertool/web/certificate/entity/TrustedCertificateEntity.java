package hu.gov.nav.xsdparsertool.web.certificate.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * A perzisztens állapot adatbázis-reprezentációját leíró entitás.
 *
 * <p>A {@code TrustedCertificateEntity} osztály a web modul alkalmazási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Entity
@Table(name="trusted_certificate")
public class TrustedCertificateEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="certificate_alias",length=255,nullable=false) private String alias;
 @Column(name="subject_dn",length=2000,nullable=false) private String subjectDn;
 @Column(name="issuer_dn",length=2000,nullable=false) private String issuerDn;
 @Column(name="serial_number",length=255,nullable=false) private String serialNumber;
 @Column(name="sha256_fingerprint",length=128,nullable=false,unique=true) private String sha256Fingerprint;
 @Column(name="valid_from",nullable=false) private Instant validFrom;
 @Column(name="valid_until",nullable=false) private Instant validUntil;
 @Column(name="source_host",length=512) private String sourceHost;
 @Column(name="source_port") private Integer sourcePort;
 @Column(name="status",length=64,nullable=false) private String status;
 @Lob @Column(name="certificate_der",nullable=false) private byte[] certificateDer;
 @Column(name="created_at",nullable=false) private Instant createdAt;
 @Column(name="created_by",length=255) private String createdBy;
 /**
  * A {@code getId} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public Long getId(){return id;}  /**
  * A {@code getAlias} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getAlias(){return alias;}  /**
  * A {@code setAlias} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param v a művelet bemeneti {@code v} értéke
  */
 public void setAlias(String v){alias=v;}  /**
  * A {@code getSubjectDn} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getSubjectDn(){return subjectDn;}  /**
  * A {@code setSubjectDn} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param v a művelet bemeneti {@code v} értéke
  */
 public void setSubjectDn(String v){subjectDn=v;}  /**
  * A {@code getIssuerDn} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getIssuerDn(){return issuerDn;}  /**
  * A {@code setIssuerDn} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param v a művelet bemeneti {@code v} értéke
  */
 public void setIssuerDn(String v){issuerDn=v;}  /**
  * A {@code getSerialNumber} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getSerialNumber(){return serialNumber;}  /**
  * A {@code setSerialNumber} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param v a művelet bemeneti {@code v} értéke
  */
 public void setSerialNumber(String v){serialNumber=v;}  /**
  * A {@code getSha256Fingerprint} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getSha256Fingerprint(){return sha256Fingerprint;}  /**
  * A {@code setSha256Fingerprint} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param v a művelet bemeneti {@code v} értéke
  */
 public void setSha256Fingerprint(String v){sha256Fingerprint=v;}  /**
  * A {@code getValidFrom} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public Instant getValidFrom(){return validFrom;}  /**
  * A {@code setValidFrom} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param v a művelet bemeneti {@code v} értéke
  */
 public void setValidFrom(Instant v){validFrom=v;}  /**
  * A {@code getValidUntil} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public Instant getValidUntil(){return validUntil;}  /**
  * A {@code setValidUntil} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param v a művelet bemeneti {@code v} értéke
  */
 public void setValidUntil(Instant v){validUntil=v;}  /**
  * A {@code getSourceHost} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getSourceHost(){return sourceHost;}  /**
  * A {@code setSourceHost} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param v a művelet bemeneti {@code v} értéke
  */
 public void setSourceHost(String v){sourceHost=v;}  /**
  * A {@code getSourcePort} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public Integer getSourcePort(){return sourcePort;}  /**
  * A {@code setSourcePort} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param v a művelet bemeneti {@code v} értéke
  */
 public void setSourcePort(Integer v){sourcePort=v;}  /**
  * A {@code getStatus} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getStatus(){return status;}  /**
  * A {@code setStatus} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param v a művelet bemeneti {@code v} értéke
  */
 public void setStatus(String v){status=v;}  /**
  * A {@code getCertificateDer} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public byte[] getCertificateDer(){return certificateDer == null ? null : certificateDer.clone();}  /**
  * A {@code setCertificateDer} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param v a művelet bemeneti {@code v} értéke
  */
 public void setCertificateDer(byte[] v){certificateDer=v == null ? null : v.clone();}  /**
  * A {@code getCreatedAt} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public Instant getCreatedAt(){return createdAt;}  /**
  * A {@code setCreatedAt} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param v a művelet bemeneti {@code v} értéke
  */
 public void setCreatedAt(Instant v){createdAt=v;}  /**
  * A {@code getCreatedBy} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a feloldott vagy lekért érték
  */
 public String getCreatedBy(){return createdBy;}  /**
  * A {@code setCreatedBy} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param v a művelet bemeneti {@code v} értéke
  */
 public void setCreatedBy(String v){createdBy=v;}
}
