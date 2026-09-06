package hu.gov.nav.xsdparsertool.web.xpath.filter;

import hu.gov.nav.xsdparsertool.web.xpath.util.IdGenerator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
/**
 * A bejövő HTTP kérésekhez stabil session-/korrelációs azonosítót biztosító servlet filter.
 * Az osztály a filter csomagból érhető el, és a projekt többi rétege innen hívja a publikus API-ját.
 * Spring regisztráció: @Component.
 * Interfész/implementáció kapcsolat: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *
 * Spring registration: @Component.
 * Interface/implementation relationship: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *

 */


@Component
public class SessionIdFilter extends OncePerRequestFilter {
    /**
     * A {@code doFilterInternal} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XPath-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @param response a feldolgozandó vagy továbbadandó válaszobjektum
     * @param filterChain a művelet bemeneti {@code filterChain} értéke
     * @throws ServletException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
/**
 * A HTTP kéréshez stabil session-azonosítót biztosít, majd a szűrőláncot a kiegészített kontextussal folytatja.
 * @param request a {@code request} paraméter átadott értéke
 * @param response a {@code response} paraméter átadott értéke
 * @param filterChain a {@code filterChain} paraméter átadott értéke
 * @throws ServletException Hiba esetén dobott kivétel.
 * @throws IOException Hiba esetén dobott kivétel.
 */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String sessionId = IdGenerator.newSessionId(18);
        MDC.put("sessionId", sessionId);
        response.setHeader("X-Session-Id", sessionId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("sessionId");
        }
    }
}
