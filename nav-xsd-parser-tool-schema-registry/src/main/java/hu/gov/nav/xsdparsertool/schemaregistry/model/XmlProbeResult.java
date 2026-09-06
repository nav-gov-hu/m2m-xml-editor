package hu.gov.nav.xsdparsertool.schemaregistry.model;



/**
 * Az XML dokumentum gyors elővizsgálatának eredményét hordozó adatmodell.
 * A séma-regiszter a gyökérelem, a namespace és az XML-ben megadott sémahelyek alapján használja a megfelelő XSD kiválasztásához.
 */
public class XmlProbeResult {
    private String rootElementName;
    private String namespace;
    private String schemaLocation;
    private String noNamespaceSchemaLocation;


    /**


     * Visszaadja az XML gyökérelemének lokális nevét.


     * @return az XML gyökérelemének neve.


     */


    public String getRootElementName() { return rootElementName; }

    /**

     * Beállítja az XML gyökérelemének lokális nevét.

     * @param rootElementName az XML gyökérelemének neve.

     */

    public void setRootElementName(String rootElementName) { this.rootElementName = rootElementName; }

    /**

     * Visszaadja az XML gyökérelemének namespace URI-ját.

     * @return a namespace URI, vagy {@code null}.

     */

    public String getNamespace() { return namespace; }

    /**

     * Beállítja az XML gyökérelemének namespace URI-ját.

     * @param namespace a namespace URI, vagy {@code null}.

     */

    public void setNamespace(String namespace) { this.namespace = namespace; }

    /**

     * Visszaadja az XML {@code xsi:schemaLocation} attribútumának teljes értékét.

     * @return a schemaLocation érték, vagy {@code null}.

     */

    public String getSchemaLocation() { return schemaLocation; }

    /**

     * Beállítja az XML {@code xsi:schemaLocation} attribútumának teljes értékét.

     * @param schemaLocation a schemaLocation érték, vagy {@code null}.

     */

    public void setSchemaLocation(String schemaLocation) { this.schemaLocation = schemaLocation; }

    /**

     * Visszaadja az XML {@code xsi:noNamespaceSchemaLocation} attribútumának értékét.

     * @return a noNamespaceSchemaLocation érték, vagy {@code null}.

     */

    public String getNoNamespaceSchemaLocation() { return noNamespaceSchemaLocation; }

    /**

     * Beállítja az XML {@code xsi:noNamespaceSchemaLocation} attribútumának értékét.

     * @param noNamespaceSchemaLocation a noNamespaceSchemaLocation érték, vagy {@code null}.

     */

    public void setNoNamespaceSchemaLocation(String noNamespaceSchemaLocation) { this.noNamespaceSchemaLocation = noNamespaceSchemaLocation; }
}
