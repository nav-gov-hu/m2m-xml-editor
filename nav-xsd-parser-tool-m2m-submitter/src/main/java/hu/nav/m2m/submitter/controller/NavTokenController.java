package hu.nav.m2m.submitter.controller;

import hu.nav.m2m.submitter.dto.TokenTestResponse;
import hu.nav.m2m.submitter.service.nav.NavTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST vezérlő a NAV OAuth/token kapcsolat diagnosztikai ellenőrzéséhez.
 */
@RestController
@RequestMapping("/api/nav-token")
@Tag(name = "NAV token teszt", description = "Common API token kérés külön tesztelése beküldés nélkül")
public class NavTokenController {
    private final NavTokenService navTokenService;

    /**
     * Létrehozza a(z) {@code NavTokenController} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
     *
     * @param navTokenService a művelethez átadott {@code navTokenService} érték
     */
    public NavTokenController(NavTokenService navTokenService) {
        this.navTokenService = navTokenService;
    }

    /**
     * A(z) {@code testToken} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @return a művelet eredménye
     */
    @PostMapping("/test")
    @Operation(summary = "NAV Common API token kérés tesztelése")
    public TokenTestResponse testToken() {
        return navTokenService.testToken();
    }
}
