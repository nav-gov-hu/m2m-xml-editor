package hu.gov.nav.xsdparsertool.web.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;

/**
 * A webes rétegek közötti adatátadás strukturált modellje.
 *
 * <p>A {@code SchemaBundleDto} osztály a web modul REST API területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public class SchemaBundleDto {
    @Schema(description = "Dokumentumtípus") private String documentType;
    @Schema(description = "Dokumentumverzió") private String documentVersion;
    @Schema(description = "Gyökérelem neve") private String rootElementName;
    @Schema(description = "Célnévtér") private String targetNamespace;
    @Schema(description = "Sémafeloldás indoka") private String matchReason;
    @Schema(description = "Elsődleges XSD") private String primaryXsd;
    @Schema(description = "UIModel fájl") private String uiModelFile;
    @Schema(description = "Page schema fájl") private String pageSchemaFile;
    @Schema(description = "Űrlap megjelenítési neve a UIModelból") private String formName;
    @Schema(description = "Űrlap leírása a UIModelból") private String formInfo;
    @Schema(description = "Űrlap megjelenítési verziója a UIModelból") private String formVersion;
    @Schema(description = "Űrlap típusa a UIModelból") private String formType;
    @Schema(description = "Feloldott XSD fájlok") private List<String> xsdFiles = new ArrayList<>();

    /**
     * A {@code getDocumentType} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getDocumentType() { return documentType; }
    /**
     * A {@code setDocumentType} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param documentType a művelet bemeneti {@code documentType} értéke
     */
    public void setDocumentType(String documentType) { this.documentType = documentType; }
    /**
     * A {@code getDocumentVersion} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getDocumentVersion() { return documentVersion; }
    /**
     * A {@code setDocumentVersion} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param documentVersion a művelet bemeneti {@code documentVersion} értéke
     */
    public void setDocumentVersion(String documentVersion) { this.documentVersion = documentVersion; }
    /**
     * A {@code getRootElementName} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getRootElementName() { return rootElementName; }
    /**
     * A {@code setRootElementName} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param rootElementName a feloldáshoz vagy azonosításhoz használt név
     */
    public void setRootElementName(String rootElementName) { this.rootElementName = rootElementName; }
    /**
     * A {@code getTargetNamespace} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getTargetNamespace() { return targetNamespace; }
    /**
     * A {@code setTargetNamespace} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param targetNamespace a feloldáshoz vagy azonosításhoz használt név
     */
    public void setTargetNamespace(String targetNamespace) { this.targetNamespace = targetNamespace; }
    /**
     * A {@code getMatchReason} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getMatchReason() { return matchReason; }
    /**
     * A {@code setMatchReason} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param matchReason a művelet bemeneti {@code matchReason} értéke
     */
    public void setMatchReason(String matchReason) { this.matchReason = matchReason; }
    /**
     * A {@code getPrimaryXsd} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getPrimaryXsd() { return primaryXsd; }
    /**
     * A {@code setPrimaryXsd} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param primaryXsd a művelet bemeneti {@code primaryXsd} értéke
     */
    public void setPrimaryXsd(String primaryXsd) { this.primaryXsd = primaryXsd; }
    /**
     * A {@code getUiModelFile} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getUiModelFile() { return uiModelFile; }
    /**
     * A {@code setUiModelFile} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param uiModelFile a feldolgozásban részt vevő fájl vagy elérési út
     */
    public void setUiModelFile(String uiModelFile) { this.uiModelFile = uiModelFile; }
    /**
     * A {@code getPageSchemaFile} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getPageSchemaFile() { return pageSchemaFile; }
    /**
     * A {@code setPageSchemaFile} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param pageSchemaFile a feldolgozásban részt vevő fájl vagy elérési út
     */
    public void setPageSchemaFile(String pageSchemaFile) { this.pageSchemaFile = pageSchemaFile; }
    /**
     * A {@code getFormName} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getFormName() { return formName; }
    /**
     * A {@code setFormName} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param formName a feloldáshoz vagy azonosításhoz használt név
     */
    public void setFormName(String formName) { this.formName = formName; }
    /**
     * A {@code getFormInfo} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getFormInfo() { return formInfo; }
    /**
     * A {@code setFormInfo} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param formInfo a művelet bemeneti {@code formInfo} értéke
     */
    public void setFormInfo(String formInfo) { this.formInfo = formInfo; }
    /**
     * A {@code getFormVersion} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getFormVersion() { return formVersion; }
    /**
     * A {@code setFormVersion} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param formVersion a művelet bemeneti {@code formVersion} értéke
     */
    public void setFormVersion(String formVersion) { this.formVersion = formVersion; }
    /**
     * A {@code getFormType} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getFormType() { return formType; }
    /**
     * A {@code setFormType} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param formType a művelet bemeneti {@code formType} értéke
     */
    public void setFormType(String formType) { this.formType = formType; }
    /**
     * A {@code getXsdFiles} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @return a művelet eredményeként előállított elemek listája
     */
    public List<String> getXsdFiles() { return xsdFiles; }
    /**
     * A {@code setXsdFiles} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param xsdFiles a feldolgozásban részt vevő fájl vagy elérési út
     */
    public void setXsdFiles(List<String> xsdFiles) { this.xsdFiles = xsdFiles; }
}
