package hu.gov.nav.xsdparsertool.web.processing.entity;

/**
 * A kapcsolódó folyamat lehetséges állapotait vagy működési módjait rögzítő típus.
 *
 * <p>A {@code ProcessingJobStatus} felsorolás a web modul feldolgozási job területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public enum ProcessingJobStatus {
    PENDING,
    RUNNING,
    FINISHED,
    FAILED,
    CANCEL_REQUESTED,
    CANCELLED;

    /**
     * A {@code isActive} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    public boolean isActive() {
        return this == PENDING || this == RUNNING || this == CANCEL_REQUESTED;
    }

    /**
     * A {@code isTerminal} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    public boolean isTerminal() {
        return this == FINISHED || this == FAILED || this == CANCELLED;
    }
}
