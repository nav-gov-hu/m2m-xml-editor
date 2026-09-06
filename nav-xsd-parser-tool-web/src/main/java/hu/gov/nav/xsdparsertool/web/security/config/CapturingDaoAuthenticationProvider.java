package hu.gov.nav.xsdparsertool.web.security.config;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * A már sikeresen ellenőrzött login credentialt továbbítja a legacy master-key
 * migrációhoz anélkül, hogy a success handlernek a HTTP request paramétereit
 * újra ki kellene olvasnia.
 */
public class CapturingDaoAuthenticationProvider extends DaoAuthenticationProvider {

    private final VerifiedLoginCredentialHolder credentialHolder;

    /**
     * Létrehozza a {@code CapturingDaoAuthenticationProvider} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param credentialHolder a művelet bemeneti {@code credentialHolder} értéke
     */
    public CapturingDaoAuthenticationProvider(VerifiedLoginCredentialHolder credentialHolder) {
        this.credentialHolder = credentialHolder;
    }

    /**
     * A {@code additionalAuthenticationChecks} művelet létrehozza vagy tartósítja a kért állapotváltozást.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @param userDetails a művelet felhasználói kontextusa vagy felhasználóneve
     * @param authentication a művelet bemeneti {@code authentication} értéke
     * @throws AuthenticationException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails,
                                                  UsernamePasswordAuthenticationToken authentication)
            throws AuthenticationException {
        credentialHolder.clear();
        super.additionalAuthenticationChecks(userDetails, authentication);
        credentialHolder.capture(authentication.getCredentials());
    }
}
