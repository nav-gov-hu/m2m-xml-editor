package hu.gov.nav.xsdparsertool.web.setup;

/**
 * A web modul kezdeti beállítási területének közös alkalmazási típusa.
 *
 * <p>A {@code SetupResult} rekord a web modul kezdeti beállítási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public record SetupResult(
        boolean completed,
        boolean restartRequired,
        boolean restartScheduled,
        String phase,
        String message) {

    /**
     * A {@code withRestartScheduled} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a kezdeti beállítási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param scheduled a művelet bemeneti {@code scheduled} értéke
     * @param updatedMessage a művelet bemeneti {@code updatedMessage} értéke
     * @return a művelet eredményeként előállított egyedi elemek halmaza
     */
    public SetupResult withRestartScheduled(boolean scheduled, String updatedMessage) {
        return new SetupResult(completed, restartRequired, scheduled, phase, updatedMessage);
    }
}
