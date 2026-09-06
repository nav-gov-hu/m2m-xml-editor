package hu.gov.nav.xsdparsertool.web.processing.dto;

/**
 * A webes rétegek közötti adatátadás strukturált modellje.
 *
 * <p>A {@code StartDemoJobRequest} rekord a web modul feldolgozási job területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public record StartDemoJobRequest(Integer durationSeconds, Long xmlFileId) {
    /**
     * A {@code normalizedDurationSeconds} művelet feldolgozza és normalizálja a bemeneti adatot a további feldolgozás számára.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a művelet feldolgozási eredménye
     */
    public int normalizedDurationSeconds() {
        if (durationSeconds == null) {
            return 12;
        }
        return Math.max(3, Math.min(120, durationSeconds));
    }
}
