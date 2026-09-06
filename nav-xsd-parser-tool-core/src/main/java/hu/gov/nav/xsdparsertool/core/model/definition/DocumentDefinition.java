package hu.gov.nav.xsdparsertool.core.model.definition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/**
 * A feldolgozott XSD strukturális dokumentumdefiníciója.
 *
 * <p>A modell a dokumentum gyökérelemét, namespace-ét, blokkjait és a teljes
 * XML/XSD-útvonalhoz kötött strukturális címkéket tartalmazza. A processing,
 * UIModel és nyomtatási folyamatok közös, webfüggetlen reprezentációként használják.</p>
 */
public class DocumentDefinition {
    private String id;
    private String name;
    private String title;
    private String rootElementName;
    private String targetNamespace;
    private List<BlockDefinition> blocks = new ArrayList<>();
    private Map<String, String> structuralLabelsByPath = new LinkedHashMap<>();
/**
 * Visszaadja a következő modellértéket: az objektum technikai azonosítója.
 *
 * @return az objektum technikai azonosítója
 */
public String getId() {
        return id;
    }
/**
 * Beállítja a következő modellértéket: az objektum technikai azonosítója.
 *
 * @param id az objektum technikai azonosítója
 */
public void setId(String id) {
        this.id = id;
    }
/**
 * Visszaadja a következő modellértéket: az elem technikai vagy megjelenítési neve.
 *
 * @return az elem technikai vagy megjelenítési neve
 */
public String getName() {
        return name;
    }
/**
 * Beállítja a következő modellértéket: az elem technikai vagy megjelenítési neve.
 *
 * @param name az elem technikai vagy megjelenítési neve
 */
public void setName(String name) {
        this.name = name;
    }
/**
 * Visszaadja a következő modellértéket: a felhasználói felületen megjelenő cím.
 *
 * @return a felhasználói felületen megjelenő cím
 */
public String getTitle() {
        return title;
    }
/**
 * Beállítja a következő modellértéket: a felhasználói felületen megjelenő cím.
 *
 * @param title a felhasználói felületen megjelenő cím
 */
public void setTitle(String title) {
        this.title = title;
    }
/**
 * Visszaadja a következő modellértéket: az XML gyökérelemének lokális neve.
 *
 * @return az XML gyökérelemének lokális neve
 */
public String getRootElementName() {
        return rootElementName;
    }
/**
 * Beállítja a következő modellértéket: az XML gyökérelemének lokális neve.
 *
 * @param rootElementName az XML gyökérelemének lokális neve
 */
public void setRootElementName(String rootElementName) {
        this.rootElementName = rootElementName;
    }
/**
 * Visszaadja a következő modellértéket: az XSD cél namespace-e.
 *
 * @return az XSD cél namespace-e
 */
public String getTargetNamespace() {
        return targetNamespace;
    }
/**
 * Beállítja a következő modellértéket: az XSD cél namespace-e.
 *
 * @param targetNamespace az XSD cél namespace-e
 */
public void setTargetNamespace(String targetNamespace) {
        this.targetNamespace = targetNamespace;
    }
/**
 * Visszaadja a következő modellértéket: a dokumentumhoz tartozó blokkdefiníciók rendezett listája.
 *
 * @return a dokumentumhoz tartozó blokkdefiníciók rendezett listája
 */
public List<BlockDefinition> getBlocks() {
        return blocks;
    }
/**
 * Beállítja a következő modellértéket: a dokumentumhoz tartozó blokkdefiníciók rendezett listája.
 *
 * @param blocks a dokumentumhoz tartozó blokkdefiníciók rendezett listája
 */
public void setBlocks(List<BlockDefinition> blocks) {
        this.blocks = blocks;
    }

    /**
     * Visszaadja a következő modellértéket: a teljes strukturális XML/XSD-útvonalhoz rendelt címkék térképe.
     *
     * @return a teljes strukturális XML/XSD-útvonalhoz rendelt címkék térképe
     */
    public Map<String, String> getStructuralLabelsByPath() {
        return structuralLabelsByPath;
    }

    /**
     * Beállítja a következő modellértéket: a teljes strukturális XML/XSD-útvonalhoz rendelt címkék térképe.
     *
     * @param Map<String a teljes strukturális XML/XSD-útvonalhoz rendelt címkék térképe
     */
    public void setStructuralLabelsByPath(Map<String, String> structuralLabelsByPath) {
        this.structuralLabelsByPath = structuralLabelsByPath == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(structuralLabelsByPath);
    }
}
