package hu.gov.nav.xsdparsertool.web.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * A web modul alkalmazási területének közös alkalmazási típusa.
 *
 * <p>A {@code UiMenuAccessInterceptor} osztály a web modul alkalmazási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Component
public class UiMenuAccessInterceptor implements HandlerInterceptor {
    private final UiMenuVisibilityService uiMenuVisibilityService;

    /**
     * Létrehozza a {@code UiMenuAccessInterceptor} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param uiMenuVisibilityService a művelet bemeneti {@code uiMenuVisibilityService} értéke
     */
    public UiMenuAccessInterceptor(UiMenuVisibilityService uiMenuVisibilityService) {
        this.uiMenuVisibilityService = uiMenuVisibilityService;
    }

    /**
     * A {@code preHandle} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @param response a feldolgozandó vagy továbbadandó válaszobjektum
     * @param handler a művelet bemeneti {@code handler} értéke
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     * @throws Exception ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String menuKey = uiMenuVisibilityService.menuKeyForPath(request.getRequestURI());
        if (menuKey != null && !uiMenuVisibilityService.isVisible(menuKey)) {
            response.sendError(HttpStatus.NOT_FOUND.value(), "Nincs jogosultságod a kért felület megnyitásához.");
            return false;
        }
        return true;
    }
}
