package hu.gov.nav.xsdparsertool.web.xmlfile.dto;

/**
 * A web modul XML-állománykezelési területének közös alkalmazási típusa.
 *
 * <p>A {@code XmlHeaderInfo} rekord a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public record XmlHeaderInfo(
        String rootElement,
        String namespaceUri,
        String schemaLocation,
        String noNamespaceSchemaLocation,
        String formType,
        String formVersion,
        String errorMessage
) {
    /**
     * A {@code detected} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    public boolean detected() {
        return rootElement != null && !rootElement.isBlank();
    }
}
