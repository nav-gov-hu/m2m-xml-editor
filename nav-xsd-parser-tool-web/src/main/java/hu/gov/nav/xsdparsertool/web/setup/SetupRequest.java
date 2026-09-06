package hu.gov.nav.xsdparsertool.web.setup;
/**
 * A webes rétegek közötti adatátadás strukturált modellje.
 *
 * <p>A {@code SetupRequest} rekord a web modul kezdeti beállítási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public record SetupRequest(
        String dataDirectory,
        String securityMode,
        String databaseType,
        String databaseHost,
        String databasePort,
        String databaseName,
        String databaseSchema,
        String databaseUsername,
        String databasePassword,
        String adminUsername,
        String adminDisplayName,
        String adminEmail,
        String adminPassword,
        String adminPasswordConfirmation,
        String githubToken,
        String m2mApiKey,
        String m2mClientId,
        String m2mClientSecret,
        String databaseTestToken) {
    /** Visszafelé kompatibilis konstruktor a korábbi H2-only setup hívásokhoz. */
    public SetupRequest(String dataDirectory, String securityMode, String adminUsername, String adminDisplayName,
                        String adminEmail, String adminPassword, String adminPasswordConfirmation,
                        String githubToken, String m2mApiKey, String m2mClientId, String m2mClientSecret) {
        this(dataDirectory, securityMode, "H2", "localhost", "", "nav_xsd_parser_tool", "PUBLIC", "sa", "",
                adminUsername, adminDisplayName, adminEmail, adminPassword, adminPasswordConfirmation,
                githubToken, m2mApiKey, m2mClientId, m2mClientSecret, null);
    }
}
