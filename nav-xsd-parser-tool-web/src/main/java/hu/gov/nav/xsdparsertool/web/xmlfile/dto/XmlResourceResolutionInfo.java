package hu.gov.nav.xsdparsertool.web.xmlfile.dto;

/**
 * A web modul XML-állománykezelési területének közös alkalmazási típusa.
 *
 * <p>A {@code XmlResourceResolutionInfo} rekord a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public record XmlResourceResolutionInfo(
        String documentType,
        String documentVersion,
        String xsdPath,
        String uiModelPath,
        String xpathRulesPath,
        String status,
        String message,
        String resolvedSchemaVersion,
        boolean schemaVersionFallback
) {
    /**
     * Visszafelé kompatibilis konstruktor a korábbi erőforrás-feloldási eredményhez.
     */
    public XmlResourceResolutionInfo(String documentType, String documentVersion, String xsdPath, String uiModelPath,
                                     String xpathRulesPath, String status, String message) {
        this(documentType, documentVersion, xsdPath, uiModelPath, xpathRulesPath, status, message, null, false);
    }
    /**
     * A {@code xsdResolved} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    public boolean xsdResolved() {
        return xsdPath != null && !xsdPath.isBlank();
    }
}
