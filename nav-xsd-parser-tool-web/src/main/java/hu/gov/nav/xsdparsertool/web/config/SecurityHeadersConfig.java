package hu.gov.nav.xsdparsertool.web.config;

import java.io.IOException;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Biztonsági HTTP válaszfejléceket beállító servlet filter.
 *
 * <p>Az alkalmazás webes válaszaihoz hozzáadja a HSTS fejlécet, amely
 * HTTPS használat esetén megakadályozza, hogy a böngésző később nem
 * titkosított HTTP kapcsolatra váltson vissza.</p>
 *
 * @since 1.0
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersConfig implements Filter {

    private static final String STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security";
    private static final String HSTS_VALUE = "max-age=31536000; includeSubDomains";

    /**
     * A {@code doFilter} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @param response a feldolgozandó vagy továbbadandó válaszobjektum
     * @param chain a művelet bemeneti {@code chain} értéke
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     * @throws ServletException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        if (response instanceof HttpServletResponse httpResponse) {
            httpResponse.setHeader(STRICT_TRANSPORT_SECURITY, HSTS_VALUE);
        }

        chain.doFilter(request, response);
    }
}