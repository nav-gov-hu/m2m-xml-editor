package hu.gov.nav.xsdparsertool.web.api.dto;

import java.util.ArrayList;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
/**
 * Az XML-validáció eredményét és a strukturált validációs hibákat továbbító REST válasz DTO.
 * Az osztály a dto csomagból érhető el, és a projekt többi rétege innen hívja a publikus API-ját.
 * Spring regisztráció: Nincs közvetlen Spring bean regisztráció.
 * Interfész/implementáció kapcsolat: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *
 * Spring registration: Nincs közvetlen Spring bean regisztráció.
 * Interface/implementation relationship: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *

 */

public class ValidateResponse {
    @Schema(description = "HU: valid mező. EN: valid field.")
    private boolean valid;
    @Schema(description = "HU: xml mező. EN: xml field.")
    private XmlProbeDto xml;
    @Schema(description = "HU: schemaBundle mező. EN: schemaBundle field.")
    private SchemaBundleDto schemaBundle;
    @Schema(description = "HU: documentDefinition mező. EN: documentDefinition field.")
    private DocumentDefinitionDto documentDefinition;
    @Schema(description = "HU: formDefinition mező. EN: formDefinition field.")
    private FormDefinitionDto formDefinition;
    @Schema(description = "HU: formData mező. EN: formData field.")
    private FormDataDto formData;
    @Schema(description = "HU: xmlView mező. EN: xmlView field.")
    private XmlDocumentViewDto xmlView;
    @Schema(description = "HU: issues mező. EN: issues field.")
    private List<ValidationIssueDto> issues = new ArrayList<>();
    @Schema(description = "HU: xpathRuleFile mező. EN: xpathRuleFile field.")
    private String xpathRuleFile;
    @Schema(description = "Nagy XML feldolgozási mód aktív.")
    private boolean largeFileMode;
    @Schema(description = "A válasz csak részleges, főlap-alapú előnézetet tartalmaz.")
    private boolean partialPreview;
    @Schema(description = "A teljes eredeti XML validációja megtörtént-e.")
    private boolean fullDocumentValidationPerformed = true;
    @Schema(description = "Nagy XML megnyitási tájékoztató.")
    private String largeFileMessage;
    @Schema(description = "Kizárólag a nagy XML űrlap-runtime számára átadott kis előnézeti XML. Nem jelenik meg XML-forrásként.")
    private String formRuntimePreviewXml;
    @Schema(description = "A nagy XML-ben előforduló ismétlődő melléklap neve.")
    private String largeXmlRepeatingFormName;
    @Schema(description = "A nagy XML-ben előforduló ismétlődő melléklapok teljes darabszáma.")
    private long largeXmlRepeatingFormCount;

    /**
     * A {@code isValid} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    public boolean isValid() { return valid; }
    /**
     * A {@code setValid} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param valid a célobjektum vagy erőforrás azonosítója
     */
    public void setValid(boolean valid) { this.valid = valid; }
    /**
     * A {@code getXml} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>Az XML-adatot a REST API folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @return a feloldott vagy lekért érték
     */
    public XmlProbeDto getXml() { return xml; }
    /**
     * A {@code setXml} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>Az XML-adatot a REST API folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param xml a feldolgozandó XML-hez tartozó adat vagy tartalom
     */
    public void setXml(XmlProbeDto xml) { this.xml = xml; }
    /**
     * A {@code getSchemaBundle} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public SchemaBundleDto getSchemaBundle() { return schemaBundle; }
    /**
     * A {@code setSchemaBundle} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param schemaBundle a művelet bemeneti {@code schemaBundle} értéke
     */
    public void setSchemaBundle(SchemaBundleDto schemaBundle) { this.schemaBundle = schemaBundle; }
    /**
     * A {@code getDocumentDefinition} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public DocumentDefinitionDto getDocumentDefinition() { return documentDefinition; }
    /**
     * A {@code setDocumentDefinition} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param documentDefinition a művelet bemeneti {@code documentDefinition} értéke
     */
    public void setDocumentDefinition(DocumentDefinitionDto documentDefinition) { this.documentDefinition = documentDefinition; }
    /**
     * A {@code getFormDefinition} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public FormDefinitionDto getFormDefinition() { return formDefinition; }
    /**
     * A {@code setFormDefinition} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param formDefinition a művelet bemeneti {@code formDefinition} értéke
     */
    public void setFormDefinition(FormDefinitionDto formDefinition) { this.formDefinition = formDefinition; }
    /**
     * A {@code getFormData} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public FormDataDto getFormData() { return formData; }
    /**
     * A {@code setFormData} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param formData a művelet bemeneti {@code formData} értéke
     */
    public void setFormData(FormDataDto formData) { this.formData = formData; }
    /**
     * A {@code getXmlView} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>Az XML-adatot a REST API folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @return a feloldott vagy lekért érték
     */
    public XmlDocumentViewDto getXmlView() { return xmlView; }
    /**
     * A {@code setXmlView} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>Az XML-adatot a REST API folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param xmlView a feldolgozandó XML-hez tartozó adat vagy tartalom
     */
    public void setXmlView(XmlDocumentViewDto xmlView) { this.xmlView = xmlView; }
    /**
     * A {@code getIssues} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a művelet eredményeként előállított elemek listája
     */
    public List<ValidationIssueDto> getIssues() { return issues; }
    /**
     * A {@code setIssues} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param issues a feldolgozandó elemek kollekciója
     */
    public void setIssues(List<ValidationIssueDto> issues) { this.issues = issues; }
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
    /**
     * A {@code isLargeFileMode} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    public boolean isLargeFileMode() { return largeFileMode; }
    /**
     * A {@code setLargeFileMode} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param largeFileMode a feldolgozásban részt vevő fájl vagy elérési út
     */
    public void setLargeFileMode(boolean largeFileMode) { this.largeFileMode = largeFileMode; }
    /**
     * A {@code isPartialPreview} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    public boolean isPartialPreview() { return partialPreview; }
    /**
     * A {@code setPartialPreview} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param partialPreview a művelet bemeneti {@code partialPreview} értéke
     */
    public void setPartialPreview(boolean partialPreview) { this.partialPreview = partialPreview; }
    /**
     * A {@code isFullDocumentValidationPerformed} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>Az ellenőrzési eredményt a webes megjelenítés és a további üzleti döntések számára konzisztens formában állítja elő.</p>
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    public boolean isFullDocumentValidationPerformed() { return fullDocumentValidationPerformed; }
    /**
     * A {@code setFullDocumentValidationPerformed} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>Az ellenőrzési eredményt a webes megjelenítés és a további üzleti döntések számára konzisztens formában állítja elő.</p>
     * @param fullDocumentValidationPerformed a művelet bemeneti {@code fullDocumentValidationPerformed} értéke
     */
    public void setFullDocumentValidationPerformed(boolean fullDocumentValidationPerformed) { this.fullDocumentValidationPerformed = fullDocumentValidationPerformed; }
    /**
     * A {@code getLargeFileMessage} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getLargeFileMessage() { return largeFileMessage; }
    /**
     * A {@code setLargeFileMessage} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param largeFileMessage a feldolgozásban részt vevő fájl vagy elérési út
     */
    public void setLargeFileMessage(String largeFileMessage) { this.largeFileMessage = largeFileMessage; }
    /**
     * A {@code getFormRuntimePreviewXml} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>Az XML-adatot a REST API folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getFormRuntimePreviewXml() { return formRuntimePreviewXml; }
    /**
     * A {@code setFormRuntimePreviewXml} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>Az XML-adatot a REST API folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param formRuntimePreviewXml a feldolgozandó XML-hez tartozó adat vagy tartalom
     */
    public void setFormRuntimePreviewXml(String formRuntimePreviewXml) { this.formRuntimePreviewXml = formRuntimePreviewXml; }
    /**
     * A {@code getLargeXmlRepeatingFormName} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>Az XML-adatot a REST API folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getLargeXmlRepeatingFormName() { return largeXmlRepeatingFormName; }
    /**
     * A {@code setLargeXmlRepeatingFormName} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>Az XML-adatot a REST API folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param largeXmlRepeatingFormName a feldolgozandó XML-hez tartozó adat vagy tartalom
     */
    public void setLargeXmlRepeatingFormName(String largeXmlRepeatingFormName) { this.largeXmlRepeatingFormName = largeXmlRepeatingFormName; }
    /**
     * A {@code getLargeXmlRepeatingFormCount} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>Az XML-adatot a REST API folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @return a feloldott vagy lekért érték
     */
    public long getLargeXmlRepeatingFormCount() { return largeXmlRepeatingFormCount; }
    /**
     * A {@code setLargeXmlRepeatingFormCount} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>Az XML-adatot a REST API folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param largeXmlRepeatingFormCount a feldolgozandó XML-hez tartozó adat vagy tartalom
     */
    public void setLargeXmlRepeatingFormCount(long largeXmlRepeatingFormCount) { this.largeXmlRepeatingFormCount = largeXmlRepeatingFormCount; }
}
