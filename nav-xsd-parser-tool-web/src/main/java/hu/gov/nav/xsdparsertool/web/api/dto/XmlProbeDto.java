package hu.gov.nav.xsdparsertool.web.api.dto;
import io.swagger.v3.oas.annotations.media.Schema;
/**
 * Az XML gyökéreleméből, namespace-éből és schemaLocation adataiból felismert metaadatok REST reprezentációja.
 * Az osztály a dto csomagból érhető el, és a projekt többi rétege innen hívja a publikus API-ját.
 * Spring regisztráció: Nincs közvetlen Spring bean regisztráció.
 * Interfész/implementáció kapcsolat: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *
 * Spring registration: Nincs közvetlen Spring bean regisztráció.
 * Interface/implementation relationship: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *

 */


public class XmlProbeDto {
    @Schema(description = "HU: fileName mező. EN: fileName field.")
    private String fileName;
    @Schema(description = "HU: rootElementName mező. EN: rootElementName field.")
    private String rootElementName;
    @Schema(description = "HU: namespace mező. EN: namespace field.")
    private String namespace;
    @Schema(description = "HU: schemaLocation mező. EN: schemaLocation field.")
    private String schemaLocation;
    @Schema(description = "HU: noNamespaceSchemaLocation mező. EN: noNamespaceSchemaLocation field.")
    private String noNamespaceSchemaLocation;
/**
 * Visszaadja a {@code fileName} mező aktuális értékét.
 * @return a {@code fileName} mező értéke
 */
    public String getFileName() { return fileName; }
/**
 * Beállítja a {@code fileName} mező értékét.
 * @param fileName a beállítandó új érték
 */
    public void setFileName(String fileName) { this.fileName = fileName; }
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
 * Visszaadja a {@code namespace} mező aktuális értékét.
 * @return a {@code namespace} mező értéke
 */
    public String getNamespace() { return namespace; }
/**
 * Beállítja a {@code namespace} mező értékét.
 * @param namespace a beállítandó új érték
 */
    public void setNamespace(String namespace) { this.namespace = namespace; }
/**
 * Visszaadja a {@code schemaLocation} mező aktuális értékét.
 * @return a {@code schemaLocation} mező értéke
 */
    public String getSchemaLocation() { return schemaLocation; }
/**
 * Beállítja a {@code schemaLocation} mező értékét.
 * @param schemaLocation a beállítandó új érték
 */
    public void setSchemaLocation(String schemaLocation) { this.schemaLocation = schemaLocation; }
/**
 * Visszaadja a {@code noNamespaceSchemaLocation} mező aktuális értékét.
 * @return a {@code noNamespaceSchemaLocation} mező értéke
 */
    public String getNoNamespaceSchemaLocation() { return noNamespaceSchemaLocation; }
/**
 * Beállítja a {@code noNamespaceSchemaLocation} mező értékét.
 * @param noNamespaceSchemaLocation a beállítandó új érték
 */
    public void setNoNamespaceSchemaLocation(String noNamespaceSchemaLocation) { this.noNamespaceSchemaLocation = noNamespaceSchemaLocation; }
}
