package hu.gov.nav.xsdparsertool.web.security.api;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import hu.gov.nav.xsdparsertool.web.security.SecurityModeProperties;

/**
 * A webes végpontokat kiszolgáló vezérlő, amely a HTTP-kéréseket a megfelelő alkalmazási szolgáltatásokhoz irányítja.
 *
 * <p>A {@code SecurityController} osztály a web modul biztonsági és jogosultságkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@RestController
@RequestMapping("/api/security")
public class SecurityController {

    private final SecurityModeProperties securityModeProperties;

    /**
     * Létrehozza a {@code SecurityController} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param securityModeProperties a művelethez szükséges konfigurációs adatok
     */
    public SecurityController(SecurityModeProperties securityModeProperties) {
        this.securityModeProperties = securityModeProperties;
    }

    /**
     * A {@code mode} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feldolgozás során felépített kulcs-érték leképezés
     */
    @GetMapping("/mode")
    public Map<String, Object> mode() {
        return Map.of(
                "mode", securityModeProperties.getSecurityMode().name(),
                "standaloneUsername", securityModeProperties.getStandaloneUsername());
    }

    /**
     * A {@code currentUser} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @param authentication a művelet bemeneti {@code authentication} értéke
     * @return a feldolgozás során felépített kulcs-érték leképezés
     */
    @GetMapping("/current-user")
    public Map<String, Object> currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Map.of(
                    "authenticated", false,
                    "mode", securityModeProperties.getSecurityMode().name());
        }
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        boolean admin = roles.contains("ROLE_ADMIN") || roles.contains("ADMIN");
        boolean operator = roles.contains("ROLE_OPERATOR") || roles.contains("OPERATOR");
        boolean fileDelete = roles.contains("ROLE_FILE_DELETE") || roles.contains("FILE_DELETE");
        boolean xmlIndexManage = admin || roles.contains("ROLE_XML_INDEX_CONFIG_MANAGE") || roles.contains("XML_INDEX_CONFIG_MANAGE");
        return Map.of(
                "authenticated", true,
                "mode", securityModeProperties.getSecurityMode().name(),
                "username", authentication.getName(),
                "roles", roles,
                "permissions", Map.of(
                        "canAdmin", admin,
                        "canUploadXml", admin || operator,
                        "canEditXml", admin || operator,
                        "canViewXml", true,
                        "canPhysicallyArchiveXml", fileDelete,
                        "canManageXmlIndexConfig", xmlIndexManage));
    }
}
