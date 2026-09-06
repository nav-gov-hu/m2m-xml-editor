package hu.nav.m2m.submitter.controller;

import hu.nav.m2m.submitter.service.nav.NavRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST vezérlő a NAV regisztrációs nonce beváltásához és a felhasználói regisztráció aktiválásához.
 */
@RestController
@RequestMapping("/api/nav-registration")
@Tag(name = "NAV regisztráció / aktiválás", description = "Nonce beváltás és felhasználó aktiválás lépésenkénti tesztelése")
public class NavRegistrationController {
    private final NavRegistrationService navRegistrationService;

    /**
     * Létrehozza a(z) {@code NavRegistrationController} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
     *
     * @param navRegistrationService a művelethez átadott {@code navRegistrationService} érték
     */
    public NavRegistrationController(NavRegistrationService navRegistrationService) {
        this.navRegistrationService = navRegistrationService;
    }

    /**
     * A(z) {@code redeemNonce} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @return a művelet eredménye
     */
    @PostMapping("/redeem-nonce")
    @Operation(summary = "Nonce beváltása signatureKeySecondPart értékre")
    public Map<String, Object> redeemNonce() {
        return navRegistrationService.redeemNonce();
    }

    /**
     * A(z) {@code activateUserRegistration} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @return a művelet eredménye
     */
    @PostMapping("/activation")
    @Operation(summary = "Felhasználó regisztráció aktiválása")
    public Map<String, Object> activateUserRegistration() {
        return navRegistrationService.activateUserRegistration();
    }
}
