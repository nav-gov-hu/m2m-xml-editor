package hu.gov.nav.xsdparsertool.web.githubupdater.service;

import java.io.IOException;

/**
 * Előzetesen megtisztított diagnosztikai üzenetből egységes GitHub transport {@link IOException} példányt hoz létre.
 */
final class GitHubTransportExceptionFactory {
    /**
     * Létrehozza a(z) {@code GitHubTransportExceptionFactory} példányt a működéshez szükséges kezdeti állapottal és függőségekkel.
     */
    private GitHubTransportExceptionFactory() {
    }

    /**
     * A biztonságosan előállított hibaüzenetből a GitHub transport réteg egységes {@link java.io.IOException} példányát hozza létre.
     *
     * @param safeMessage a már megtisztított, naplózható hibaüzenet
     * @return a művelet eredménye
     */
    static IOException create(String safeMessage) {
        return new IOException(safeMessage);
    }
}
