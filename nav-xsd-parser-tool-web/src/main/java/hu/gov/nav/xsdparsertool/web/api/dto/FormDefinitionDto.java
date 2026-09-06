package hu.gov.nav.xsdparsertool.web.api.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;
/**
 * A frontend űrlaprendereléséhez szükséges teljes, szekciókra és mezőkre bontott definíciót hordozó DTO.
 * Az osztály a dto csomagból érhető el, és a projekt többi rétege innen hívja a publikus API-ját.
 * Spring regisztráció: Nincs közvetlen Spring bean regisztráció.
 * Interfész/implementáció kapcsolat: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *
 * Spring registration: Nincs közvetlen Spring bean regisztráció.
 * Interface/implementation relationship: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *

 */


public class FormDefinitionDto {
    @Schema(description = "HU: id mező. EN: id field.")
    private String id;
    @Schema(description = "HU: title mező. EN: title field.")
    private String title;
    @Schema(description = "HU: tabs mező. EN: tabs field.")
    private List<FormTabDto> tabs = new ArrayList<>();
    @Schema(description = "XSD annotation alapú strukturális címkék XML útvonal szerint")
    private Map<String, String> structuralLabelsByPath = new LinkedHashMap<>();
/**
 * Visszaadja a {@code id} mező aktuális értékét.
 * @return a {@code id} mező értéke
 */
    public String getId() { return id; }
/**
 * Beállítja a {@code id} mező értékét.
 * @param id a beállítandó új érték
 */
    public void setId(String id) { this.id = id; }
/**
 * Visszaadja a {@code title} mező aktuális értékét.
 * @return a {@code title} mező értéke
 */
    public String getTitle() { return title; }
/**
 * Beállítja a {@code title} mező értékét.
 * @param title a beállítandó új érték
 */
    public void setTitle(String title) { this.title = title; }
/**
 * Visszaadja a {@code tabs} mező aktuális értékét.
 * @return a {@code tabs} mező értéke
 */
    public List<FormTabDto> getTabs() { return tabs; }
/**
 * Beállítja a {@code tabs} mező értékét.
 * @param tabs a beállítandó új érték
 */
    public void setTabs(List<FormTabDto> tabs) { this.tabs = tabs; }
    /**
     * A {@code getStructuralLabelsByPath} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @return a feldolgozás során felépített kulcs-érték leképezés
     */
    public Map<String, String> getStructuralLabelsByPath() { return structuralLabelsByPath; }
    /**
     * A {@code setStructuralLabelsByPath} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param structuralLabelsByPath a feldolgozásban részt vevő fájl vagy elérési út
     */
    public void setStructuralLabelsByPath(Map<String, String> structuralLabelsByPath) {
        this.structuralLabelsByPath = structuralLabelsByPath == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(structuralLabelsByPath);
    }
}
