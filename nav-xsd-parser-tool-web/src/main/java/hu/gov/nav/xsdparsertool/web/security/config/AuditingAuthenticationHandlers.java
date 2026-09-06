package hu.gov.nav.xsdparsertool.web.security.config;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

import hu.gov.nav.xsdparsertool.web.audit.AuditLogService;
import hu.gov.nav.xsdparsertool.web.security.service.LoginAttemptService;
import hu.gov.nav.xsdparsertool.web.secret.service.MasterKeyService;
import hu.gov.nav.xsdparsertool.web.secret.service.RuntimeSecretBindingService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A web modul biztonsági és jogosultságkezelési területének közös alkalmazási típusa.
 *
 * <p>A {@code AuditingAuthenticationHandlers} osztály a web modul biztonsági és jogosultságkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Component
public class AuditingAuthenticationHandlers implements AuthenticationSuccessHandler, AuthenticationFailureHandler, LogoutHandler {

    private final AuditLogService auditLogService;
    private final LoginAttemptService loginAttemptService;
    private final MasterKeyService masterKeyService;
    private final RuntimeSecretBindingService runtimeSecretBindingService;
    private final VerifiedLoginCredentialHolder credentialHolder;

    /**
     * Létrehozza a {@code AuditingAuthenticationHandlers} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param auditLogService a művelet bemeneti {@code auditLogService} értéke
     * @param loginAttemptService a művelet bemeneti {@code loginAttemptService} értéke
     * @param masterKeyService a művelet bemeneti {@code masterKeyService} értéke
     * @param runtimeSecretBindingService a művelet bemeneti {@code runtimeSecretBindingService} értéke
     * @param credentialHolder a művelet bemeneti {@code credentialHolder} értéke
     */
    public AuditingAuthenticationHandlers(AuditLogService auditLogService, LoginAttemptService loginAttemptService,
                                         MasterKeyService masterKeyService,
                                         RuntimeSecretBindingService runtimeSecretBindingService,
                                         VerifiedLoginCredentialHolder credentialHolder) {
        this.auditLogService = auditLogService;
        this.loginAttemptService = loginAttemptService;
        this.masterKeyService = masterKeyService;
        this.runtimeSecretBindingService = runtimeSecretBindingService;
        this.credentialHolder = credentialHolder;
    }

    /**
     * A {@code onAuthenticationSuccess} művelet kezeli a kapcsolódó eseményt vagy feldolgozási ágat.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @param response a feldolgozandó vagy továbbadandó válaszobjektum
     * @param authentication a művelet bemeneti {@code authentication} értéke
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     * @throws ServletException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        char[] loginPassword = credentialHolder.consume();
        try {
            boolean migrated = masterKeyService.migrateLegacyStandaloneKey(authentication.getName(), loginPassword);
            if (migrated) {
                runtimeSecretBindingService.refresh();
                auditLogService.log("MASTER_KEY_MIGRATED", authentication.getName(), "SUCCESS",
                        "A standalone master.key gepi szintu formatumra migralva.");
            }
        } catch (RuntimeException ex) {
            auditLogService.log("MASTER_KEY_MIGRATION_SKIPPED", authentication.getName(), "WARNING",
                    "A legacy standalone master.key migracioja nem sikerult; a felhasznaloi belepes ettol fuggetlenul sikeres.");
        } finally {
            java.util.Arrays.fill(loginPassword, '\0');
        }
        loginAttemptService.success(authentication.getName());
        auditLogService.log("LOGIN_SUCCESS", authentication.getName(), "SUCCESS", "Sikeres bejelentkezes.");
        response.sendRedirect(request.getContextPath() + "/xml-files.html");
    }

    /**
     * A {@code onAuthenticationFailure} művelet kezeli a kapcsolódó eseményt vagy feldolgozási ágat.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @param response a feldolgozandó vagy továbbadandó válaszobjektum
     * @param exception a művelet bemeneti {@code exception} értéke
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     * @throws ServletException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String username = request.getParameter("username");
        loginAttemptService.failure(username);
        auditLogService.log("LOGIN_FAILED", username, "ERROR", "Sikertelen bejelentkezes.");
        response.sendRedirect(request.getContextPath() + "/login.html?error=true");
    }

    /**
     * A {@code logout} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @param response a feldolgozandó vagy továbbadandó válaszobjektum
     * @param authentication a művelet bemeneti {@code authentication} értéke
     */
    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication != null) {
            auditLogService.log("LOGOUT", authentication.getName(), "SUCCESS", "Kijelentkezes.");
        }
    }
}
