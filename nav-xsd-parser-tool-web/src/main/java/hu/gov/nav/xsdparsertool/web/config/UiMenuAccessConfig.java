package hu.gov.nav.xsdparsertool.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * A web modul kapcsolódó infrastruktúrájának Spring-konfigurációját biztosító típus.
 *
 * <p>A {@code UiMenuAccessConfig} osztály a web modul alkalmazási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Configuration
public class UiMenuAccessConfig implements WebMvcConfigurer {
    private final UiMenuAccessInterceptor uiMenuAccessInterceptor;

    /**
     * Létrehozza a {@code UiMenuAccessConfig} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param uiMenuAccessInterceptor a művelet bemeneti {@code uiMenuAccessInterceptor} értéke
     */
    public UiMenuAccessConfig(UiMenuAccessInterceptor uiMenuAccessInterceptor) {
        this.uiMenuAccessInterceptor = uiMenuAccessInterceptor;
    }

    /**
     * A {@code addInterceptors} művelet létrehozza vagy tartósítja a kért állapotváltozást.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param registry a művelet bemeneti {@code registry} értéke
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(uiMenuAccessInterceptor)
                .addPathPatterns(
                        "/validate.html",
                        "/xpath-validator.html",
                        "/form.html",
                        "/admin.html"
                );
    }
}
