package hu.gov.nav.xsdparsertool.web.api.dto;
import io.swagger.v3.oas.annotations.media.Schema;
/**
 * Az XML felderítési művelet eredményét, a feloldott sémaadatokat és a dokumentumdefiníciót összefogó válasz DTO.
 * Az osztály a dto csomagból érhető el, és a projekt többi rétege innen hívja a publikus API-ját.
 * Spring regisztráció: Nincs közvetlen Spring bean regisztráció.
 * Interfész/implementáció kapcsolat: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *
 * Spring registration: Nincs közvetlen Spring bean regisztráció.
 * Interface/implementation relationship: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *

 */


public class InspectResponse {
    @Schema(description = "HU: xml mező. EN: xml field.")
    private XmlProbeDto xml;
    @Schema(description = "HU: schemaBundle mező. EN: schemaBundle field.")
    private SchemaBundleDto schemaBundle;
    @Schema(description = "HU: documentDefinition mező. EN: documentDefinition field.")
    private DocumentDefinitionDto documentDefinition;
    @Schema(description = "HU: xpathRuleFile mező. EN: xpathRuleFile field.")
    private String xpathRuleFile;
/**
 * Visszaadja a {@code xml} mező aktuális értékét.
 * @return a {@code xml} mező értéke
 */
    public XmlProbeDto getXml() { return xml; }
/**
 * Beállítja a {@code xml} mező értékét.
 * @param xml a beállítandó új érték
 */
    public void setXml(XmlProbeDto xml) { this.xml = xml; }
/**
 * Visszaadja a {@code schemaBundle} mező aktuális értékét.
 * @return a {@code schemaBundle} mező értéke
 */
    public SchemaBundleDto getSchemaBundle() { return schemaBundle; }
/**
 * Beállítja a {@code schemaBundle} mező értékét.
 * @param schemaBundle a beállítandó új érték
 */
    public void setSchemaBundle(SchemaBundleDto schemaBundle) { this.schemaBundle = schemaBundle; }
/**
 * Visszaadja a {@code documentDefinition} mező aktuális értékét.
 * @return a {@code documentDefinition} mező értéke
 */
    public DocumentDefinitionDto getDocumentDefinition() { return documentDefinition; }
/**
 * Beállítja a {@code documentDefinition} mező értékét.
 * @param documentDefinition a beállítandó új érték
 */
    public void setDocumentDefinition(DocumentDefinitionDto documentDefinition) { this.documentDefinition = documentDefinition; }

    /**
     * A {@code getXpathRuleFile} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getXpathRuleFile() { return xpathRuleFile; }
    /**
     * A {@code setXpathRuleFile} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param xpathRuleFile a feldolgozásban részt vevő fájl vagy elérési út
     */
    public void setXpathRuleFile(String xpathRuleFile) { this.xpathRuleFile = xpathRuleFile; }

}
