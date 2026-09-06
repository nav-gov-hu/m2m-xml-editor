package hu.gov.nav.xsdparsertool.web.setup;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * A kapcsolódó webes üzleti vagy alkalmazási folyamatokat összefogó szolgáltatás.
 *
 * <p>A {@code SetupStateService} osztály a web modul kezdeti beállítási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Service
public class SetupStateService {
    private volatile boolean completed;
    /**
     * Létrehozza a {@code SetupStateService} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param completed a művelet bemeneti {@code completed} értéke
     */
    public SetupStateService(@Value("${nav.xsdparsertool.setup.completed:false}") boolean completed) { this.completed = completed; }
    /**
     * A {@code isCompleted} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a kezdeti beállítási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    public boolean isCompleted() { return completed; }
    /**
     * A {@code markCompleted} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a kezdeti beállítási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     */
    public void markCompleted() { this.completed = true; }
}
