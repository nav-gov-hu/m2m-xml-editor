package hu.gov.nav.xsdparsertool.web.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * A kapcsolódó webes üzleti vagy alkalmazási folyamatokat összefogó szolgáltatás.
 *
 * <p>A {@code UiMenuVisibilityService} osztály a web modul alkalmazási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Service
public class UiMenuVisibilityService {
    public static final String MENU_VALIDATE = "validate";
    public static final String MENU_XML_FILES = "xmlFiles";
    public static final String MENU_GITHUB_TEMPLATES = "githubTemplates";
    public static final String MENU_XPATH_VALIDATOR = "xpathValidator";
    public static final String MENU_FORM = "form";
    public static final String MENU_ADMIN = "admin";

    /**
     * A {@code headerMenuVisibility} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feldolgozás során felépített kulcs-érték leképezés
     */
    public Map<String, Boolean> headerMenuVisibility() {
        Map<String, Boolean> visibility = new LinkedHashMap<>();
        visibility.put(MENU_VALIDATE, isVisible(MENU_VALIDATE));
        visibility.put(MENU_XML_FILES, isVisible(MENU_XML_FILES));
        visibility.put(MENU_GITHUB_TEMPLATES, isVisible(MENU_GITHUB_TEMPLATES));
        visibility.put(MENU_XPATH_VALIDATOR, isVisible(MENU_XPATH_VALIDATOR));
        visibility.put(MENU_FORM, isVisible(MENU_FORM));
        visibility.put(MENU_ADMIN, isVisible(MENU_ADMIN));
        return visibility;
    }

    /**
     * A {@code isVisible} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param menuKey a művelet bemeneti {@code menuKey} értéke
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    public boolean isVisible(String menuKey) {
        if (MENU_ADMIN.equals(menuKey)) {
            return hasRole("ROLE_ADMIN") || hasRole("ADMIN");
        }
        return true;
    }

    /**
     * A {@code menuKeyForPath} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param requestPath a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet feldolgozási eredménye
     */
    public String menuKeyForPath(String requestPath) {
        if (requestPath == null) {
            return null;
        }
        String path = requestPath;
        int queryIndex = path.indexOf('?');
        if (queryIndex >= 0) {
            path = path.substring(0, queryIndex);
        }
        return switch (path) {
            case "/" -> MENU_XML_FILES;
            case "/validate.html" -> MENU_VALIDATE;
            case "/xml-files.html" -> MENU_XML_FILES;
            case "/github-templates.html" -> MENU_GITHUB_TEMPLATES;
            case "/xpath-validator.html" -> MENU_XPATH_VALIDATOR;
            case "/form.html" -> MENU_FORM;
            case "/admin.html" -> MENU_ADMIN;
            default -> null;
        };
    }

    /**
     * A {@code hasRole} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @param role a művelet bemeneti {@code role} értéke
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    private boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> role.equals(authority.getAuthority()));
    }
}
