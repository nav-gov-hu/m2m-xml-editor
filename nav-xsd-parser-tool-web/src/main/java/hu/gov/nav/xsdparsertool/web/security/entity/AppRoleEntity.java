package hu.gov.nav.xsdparsertool.web.security.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A perzisztens állapot adatbázis-reprezentációját leíró entitás.
 *
 * <p>A {@code AppRoleEntity} osztály a web modul biztonsági és jogosultságkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Entity
@Table(name = "app_role")
public class AppRoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_code", nullable = false, unique = true, length = 100)
    private String roleCode;

    @Column(name = "role_name", nullable = false, length = 255)
    private String roleName;

    /**
     * A {@code getId} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public Long getId() {
        return id;
    }

    /**
     * A {@code getRoleCode} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getRoleCode() {
        return roleCode;
    }

    /**
     * A {@code setRoleCode} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @param roleCode a művelet bemeneti {@code roleCode} értéke
     */
    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    /**
     * A {@code getRoleName} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getRoleName() {
        return roleName;
    }

    /**
     * A {@code setRoleName} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @param roleName a feloldáshoz vagy azonosításhoz használt név
     */
    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
}
