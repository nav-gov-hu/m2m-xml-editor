package hu.gov.nav.xsdparsertool.web.security.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A perzisztens állapot adatbázis-reprezentációját leíró entitás.
 *
 * <p>A {@code PasswordHistoryEntity} osztály a web modul biztonsági és jogosultságkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Entity
@Table(name = "app_user_password_history")
public class PasswordHistoryEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false)
    private AppUserEntity user;
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    /**
     * A {@code getId} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public Long getId() { return id; }
    /**
     * A {@code getUser} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @return a feloldott vagy lekért érték
     */
    public AppUserEntity getUser() { return user; }
    /**
     * A {@code setUser} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @param user a művelet felhasználói kontextusa vagy felhasználóneve
     */
    public void setUser(AppUserEntity user) { this.user = user; }
    /**
     * A {@code getPasswordHash} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getPasswordHash() { return passwordHash; }
    /**
     * A {@code setPasswordHash} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param passwordHash a művelet bemeneti {@code passwordHash} értéke
     */
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    /**
     * A {@code getCreatedAt} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public LocalDateTime getCreatedAt() { return createdAt; }
    /**
     * A {@code setCreatedAt} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param createdAt a művelet bemeneti {@code createdAt} értéke
     */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
