package hu.gov.nav.xsdparsertool.web.api.dto;

import java.util.ArrayList;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
/**
 * Egy űrlap felső szintű lapjának vagy részbizonylatának frontend felé továbbított metaadatait hordozó DTO.
 * Az osztály a dto csomagból érhető el, és a projekt többi rétege innen hívja a publikus API-ját.
 * Spring regisztráció: Nincs közvetlen Spring bean regisztráció.
 * Interfész/implementáció kapcsolat: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *
 * Spring registration: Nincs közvetlen Spring bean regisztráció.
 * Interface/implementation relationship: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *

 */


public class FormTabDto {
    @Schema(description = "HU: id mező. EN: id field.")
    private String id;
    @Schema(description = "HU: title mező. EN: title field.")
    private String title;
    @Schema(description = "HU: sections mező. EN: sections field.")
    private List<FormSectionDto> sections = new ArrayList<>();
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
 * Visszaadja a {@code sections} mező aktuális értékét.
 * @return a {@code sections} mező értéke
 */
    public List<FormSectionDto> getSections() { return sections; }
/**
 * Beállítja a {@code sections} mező értékét.
 * @param sections a beállítandó új érték
 */
    public void setSections(List<FormSectionDto> sections) { this.sections = sections; }
}
