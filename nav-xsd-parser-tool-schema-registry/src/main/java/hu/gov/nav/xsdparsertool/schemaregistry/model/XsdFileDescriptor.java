package hu.gov.nav.xsdparsertool.schemaregistry.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;



/**
 * Egy indexelt XSD állomány metaadatait leíró könnyűsúlyú modell.
 * A séma-regiszter ebből az objektumból dolgozik a gyökérelem-, namespace- és kapcsolódó sémaadatok összehasonlításakor.
 */
public class XsdFileDescriptor {
    private Path path;
    private String targetNamespace;
    private List<String> rootElementNames = new ArrayList<>();
    private List<String> relatedSchemaLocations = new ArrayList<>();


    /**


     * Visszaadja a leírt XSD állomány elérési útját.


     * @return az XSD állomány elérési útja.


     */


    public Path getPath() { return path; }

    /**

     * Beállítja a leírt XSD állomány elérési útját.

     * @param path az XSD állomány elérési útja.

     */

    public void setPath(Path path) { this.path = path; }

    /**

     * Visszaadja az XSD {@code targetNamespace} értékét.

     * @return a target namespace, vagy {@code null}.

     */

    public String getTargetNamespace() { return targetNamespace; }

    /**

     * Beállítja az XSD target namespace értékét.

     * @param targetNamespace a target namespace, vagy {@code null}.

     */

    public void setTargetNamespace(String targetNamespace) { this.targetNamespace = targetNamespace; }

    /**

     * Visszaadja az XSD globális gyökérelemeinek neveit.

     * @return a gyökérelemnevek listája.

     */

    public List<String> getRootElementNames() { return rootElementNames; }

    /**

     * Beállítja az XSD globális gyökérelemeinek neveit.

     * @param rootElementNames a gyökérelemnevek listája.

     */

    public void setRootElementNames(List<String> rootElementNames) { this.rootElementNames = rootElementNames; }

    /**

     * Visszaadja az import/include/redefine elemekben hivatkozott sémahelyeket.

     * @return a kapcsolódó sémahelyek listája.

     */

    public List<String> getRelatedSchemaLocations() { return relatedSchemaLocations; }

    /**

     * Beállítja az import/include/redefine elemekből származó sémahelyeket.

     * @param relatedSchemaLocations a kapcsolódó sémahelyek listája.

     */

    public void setRelatedSchemaLocations(List<String> relatedSchemaLocations) { this.relatedSchemaLocations = relatedSchemaLocations; }

    /**
     * Az XSD-leírók azonosságát a fizikai sémafájl útvonala alapján határozza meg.
     *
     * @param o az összehasonlítandó objektum
     * @return {@code true}, ha mindkét leíró ugyanahhoz az XSD útvonalhoz tartozik
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof XsdFileDescriptor that)) return false;
        return Objects.equals(path, that.path);
    }

    /**
     * Az {@link #equals(Object)} útvonal-alapú azonosságával konzisztens hash-kódot képez.
     *
     * @return az XSD útvonalából képzett hash-kód
     */
    @Override
    public int hashCode() {
        return Objects.hash(path);
    }
}
