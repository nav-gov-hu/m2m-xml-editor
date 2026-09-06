package hu.gov.nav.xsdparsertool.web.security.apikey;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Egyetlen, kulso properties-ben megadott API kulcsot fogad el.
 *     Sikeres azonositas eseten teljes alkalmazasjogot ad a technikai kliensnek.
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);

    private final ApiKeySecurityProperties properties;

    /**
     * Létrehozza a {@code ApiKeyAuthenticationFilter} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param properties a művelethez szükséges konfigurációs adatok
     */
    public ApiKeyAuthenticationFilter(ApiKeySecurityProperties properties) {
        this.properties = properties;
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
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!properties.isConfiguredEnabled()) {
            log.debug("API key authentication skipped: configuredEnabled=false, method={}, uri={}, headerName={}",
                    safeForLog(request.getMethod()), safeForLog(request.getRequestURI()), safeForLog(properties.getHeaderName()));
            filterChain.doFilter(request, response);
            return;
        }
        if (!properties.hasApiKey()) {
            log.error("API key authentication unavailable: configuredEnabled=true but decrypted API key is missing, method={}, uri={}, headerName={}",
                    safeForLog(request.getMethod()), safeForLog(request.getRequestURI()), safeForLog(properties.getHeaderName()));
            filterChain.doFilter(request, response);
            return;
        }

        String providedApiKey = request.getHeader(properties.getHeaderName());
        if (!StringUtils.hasText(providedApiKey)) {
            log.warn("API key header missing: method={}, uri={}, remoteAddr={}, expectedHeader={}",
                    safeForLog(request.getMethod()), safeForLog(request.getRequestURI()), safeForLog(request.getRemoteAddr()), safeForLog(properties.getHeaderName()));
            filterChain.doFilter(request, response);
            return;
        }

        if (!apiKeysEqual(providedApiKey.trim(), properties.getApiKey())) {
            log.warn("Invalid API key: method={}, uri={}, remoteAddr={}, headerName={}, providedLength={}, expectedLength={}",
                    safeForLog(request.getMethod()), safeForLog(request.getRequestURI()), safeForLog(request.getRemoteAddr()), safeForLog(properties.getHeaderName()),
                    providedApiKey.trim().length(), properties.getApiKey().length());
            writeUnauthorized(response);
            return;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                properties.getPrincipalName(),
                "N/A",
                List.of(
                        new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("ROLE_OPERATOR"),
                        new SimpleGrantedAuthority("ROLE_VIEWER"),
                        new SimpleGrantedAuthority("ROLE_FILE_DELETE"),
                        new SimpleGrantedAuthority("ROLE_M2M_SUBMITTER"),
                        new SimpleGrantedAuthority("API_KEY_FULL_ACCESS")
                ));
        authentication.setDetails(request.getRemoteAddr());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("API key authentication accepted for {} {} as {}", safeForLog(request.getMethod()), safeForLog(request.getRequestURI()), safeForLog(properties.getPrincipalName()));
        filterChain.doFilter(request, response);
    }

    /**
     * A {@code safeForLog} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param value a művelet bemeneti {@code value} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String safeForLog(String value) {
        if (value == null) {
            return null;
        }
        return value.replace('\r', ' ').replace('\n', ' ');
    }

    /**
     * A {@code apiKeysEqual} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param provided a művelet bemeneti {@code provided} értéke
     * @param expected a művelet bemeneti {@code expected} értéke
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    private boolean apiKeysEqual(String provided, String expected) {
        byte[] providedBytes = provided.getBytes(StandardCharsets.UTF_8);
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(providedBytes, expectedBytes);
    }

    /**
     * A {@code writeUnauthorized} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @param response a feldolgozandó vagy továbbadandó válaszobjektum
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"error\":\"UNAUTHORIZED\",\"message\":\"Invalid API key.\"}");
    }
}
