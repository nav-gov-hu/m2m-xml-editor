package hu.gov.nav.xsdparsertool.web.security.config;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A HTTP-kérés feldolgozási láncának speciális szűrőkomponense.
 *
 * <p>A {@code StandaloneAuthenticationFilter} osztály a web modul biztonsági és jogosultságkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public class StandaloneAuthenticationFilter extends OncePerRequestFilter {

    private final String username;

    /**
     * Létrehozza a {@code StandaloneAuthenticationFilter} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param username a művelet felhasználói kontextusa vagy felhasználóneve
     */
    public StandaloneAuthenticationFilter(String username) {
        this.username = username;
    }

    /**
     * A {@code doFilterInternal} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @param response a feldolgozandó vagy továbbadandó válaszobjektum
     * @param filterChain a művelet bemeneti {@code filterChain} értéke
     * @throws ServletException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    username,
                    "N/A",
                    List.of(
                            new SimpleGrantedAuthority("ROLE_ADMIN"),
                            new SimpleGrantedAuthority("ROLE_OPERATOR"),
                            new SimpleGrantedAuthority("ROLE_VIEWER"),
                            new SimpleGrantedAuthority("ROLE_FILE_DELETE"),
                            new SimpleGrantedAuthority("ROLE_M2M_SUBMITTER"),
                            new SimpleGrantedAuthority("ROLE_XML_INDEX_CONFIG_MANAGE")));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }
}
