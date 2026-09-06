package hu.gov.nav.xsdparsertool.web.setup;

import java.io.IOException;
import java.util.Set;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * A HTTP-kérés feldolgozási láncának speciális szűrőkomponense.
 *
 * <p>A {@code SetupRequiredFilter} osztály a web modul kezdeti beállítási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public class SetupRequiredFilter extends OncePerRequestFilter {
    private final SetupStateService state;
    private static final Set<String> PUBLIC = Set.of("/setup.html", "/js/pages/setup.js", "/styles/setup.css",
            "/js/core/theme-mode.js", "/styles.css", "/favicon.ico");
    /**
     * Létrehozza a {@code SetupRequiredFilter} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param state a feldolgozandó elemek kollekciója
     */
    public SetupRequiredFilter(SetupStateService state) { this.state = state; }
    /**
     * A {@code shouldNotFilter} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a kezdeti beállítási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String path=request.getRequestURI();
        return state.isCompleted() || PUBLIC.contains(path) || path.startsWith("/styles/") || path.startsWith("/images/") || path.startsWith("/api/setup/");
    }
    /**
     * A {@code doFilterInternal} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a kezdeti beállítási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @param response a feldolgozandó vagy továbbadandó válaszobjektum
     * @param chain a művelet bemeneti {@code chain} értéke
     * @throws ServletException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String accept=request.getHeader("Accept");
        if (request.getRequestURI().startsWith("/api/")) {
            response.setStatus(503); response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"setupRequired\":true,\"message\":\"Az első indítási beállítás még nem fejeződött be.\"}");
        } else if (accept == null || accept.contains("text/html") || request.getRequestURI().endsWith(".html") || "/".equals(request.getRequestURI())) {
            response.sendRedirect("/setup.html");
        } else chain.doFilter(request,response);
    }
}
