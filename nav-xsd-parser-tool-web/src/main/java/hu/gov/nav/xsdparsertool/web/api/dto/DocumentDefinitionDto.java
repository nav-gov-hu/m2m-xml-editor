package hu.gov.nav.xsdparsertool.web.api.dto;
import io.swagger.v3.oas.annotations.media.Schema;
/**
 * Az XSD/UIModel feldolgozásból előállított dokumentumdefiníció webes, sorosítható reprezentációja.
 * Az osztály a dto csomagból érhető el, és a projekt többi rétege innen hívja a publikus API-ját.
 * Spring regisztráció: Nincs közvetlen Spring bean regisztráció.
 * Interfész/implementáció kapcsolat: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *
 * Spring registration: Nincs közvetlen Spring bean regisztráció.
 * Interface/implementation relationship: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *

 */


public class DocumentDefinitionDto {
    @Schema(description = "HU: id mező. EN: id field.")
    private String id;
    @Schema(description = "HU: name mező. EN: name field.")
    private String name;
    @Schema(description = "HU: title mező. EN: title field.")
    private String title;
    @Schema(description = "HU: rootElementName mező. EN: rootElementName field.")
    private String rootElementName;
    @Schema(description = "HU: targetNamespace mező. EN: targetNamespace field.")
    private String targetNamespace;
    @Schema(description = "HU: blockCount mező. EN: blockCount field.")
    private int blockCount;
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
 * Visszaadja a {@code name} mező aktuális értékét.
 * @return a {@code name} mező értéke
 */
    public String getName() { return name; }
/**
 * Beállítja a {@code name} mező értékét.
 * @param name a beállítandó új érték
 */
    public void setName(String name) { this.name = name; }
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
 * Visszaadja a {@code rootElementName} mező aktuális értékét.
 * @return a {@code rootElementName} mező értéke
 */
    public String getRootElementName() { return rootElementName; }
/**
 * Beállítja a {@code rootElementName} mező értékét.
 * @param rootElementName a beállítandó új érték
 */
    public void setRootElementName(String rootElementName) { this.rootElementName = rootElementName; }
/**
 * Visszaadja a {@code targetNamespace} mező aktuális értékét.
 * @return a {@code targetNamespace} mező értéke
 */
    public String getTargetNamespace() { return targetNamespace; }
/**
 * Beállítja a {@code targetNamespace} mező értékét.
 * @param targetNamespace a beállítandó új érték
 */
    public void setTargetNamespace(String targetNamespace) { this.targetNamespace = targetNamespace; }
/**
 * Visszaadja a {@code blockCount} mező aktuális értékét.
 * @return a {@code blockCount} mező értéke
 */
    public int getBlockCount() { return blockCount; }
/**
 * Beállítja a {@code blockCount} mező értékét.
 * @param blockCount a beállítandó új érték
 */
    public void setBlockCount(int blockCount) { this.blockCount = blockCount; }
}
