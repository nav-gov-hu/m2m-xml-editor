package hu.gov.nav.xsdparsertool.core.model.bundle;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
/**
 * Egy nyomtatvány feldolgozásához feloldott séma-erőforrások összefoglaló modellje.
 *
 * <p>A Schema Registry ezt az objektumot adja át a feldolgozási rétegnek. A csomag
 * tartalmazza a dokumentumtípus és verzió metaadatait, a fő XSD-t, a kapcsolódó
 * XSD-állományokat, valamint az opcionális UIModel- és page-schema erőforrást.</p>
 */
public class SchemaBundle {
    private String documentType;
    private String documentVersion;
    private String rootElementName;
    private String targetNamespace;
    private String matchReason;
    private Path primaryXsd;
    private List<Path> xsdFiles = new ArrayList<>();
    private Path uiModelFile;
    private Path pageSchemaFile;
/**
 * Visszaadja a következő modellértéket: a feloldott nyomtatvány dokumentumtípusa.
 *
 * @return a feloldott nyomtatvány dokumentumtípusa
 */
public String getDocumentType() { return documentType; }
/**
 * Beállítja a következő modellértéket: a feloldott nyomtatvány dokumentumtípusa.
 *
 * @param documentType a feloldott nyomtatvány dokumentumtípusa
 */
public void setDocumentType(String documentType) { this.documentType = documentType; }
/**
 * Visszaadja a következő modellértéket: a feloldott nyomtatvány főverziója.
 *
 * @return a feloldott nyomtatvány főverziója
 */
public String getDocumentVersion() { return documentVersion; }
/**
 * Beállítja a következő modellértéket: a feloldott nyomtatvány főverziója.
 *
 * @param documentVersion a feloldott nyomtatvány főverziója
 */
public void setDocumentVersion(String documentVersion) { this.documentVersion = documentVersion; }
/**
 * Visszaadja a következő modellértéket: az XML gyökérelemének lokális neve.
 *
 * @return az XML gyökérelemének lokális neve
 */
public String getRootElementName() { return rootElementName; }
/**
 * Beállítja a következő modellértéket: az XML gyökérelemének lokális neve.
 *
 * @param rootElementName az XML gyökérelemének lokális neve
 */
public void setRootElementName(String rootElementName) { this.rootElementName = rootElementName; }
/**
 * Visszaadja a következő modellértéket: az XSD cél namespace-e.
 *
 * @return az XSD cél namespace-e
 */
public String getTargetNamespace() { return targetNamespace; }
/**
 * Beállítja a következő modellértéket: az XSD cél namespace-e.
 *
 * @param targetNamespace az XSD cél namespace-e
 */
public void setTargetNamespace(String targetNamespace) { this.targetNamespace = targetNamespace; }
/**
 * Visszaadja a következő modellértéket: a Schema Registry által rögzített feloldási indok.
 *
 * @return a Schema Registry által rögzített feloldási indok
 */
public String getMatchReason() { return matchReason; }
/**
 * Beállítja a következő modellértéket: a Schema Registry által rögzített feloldási indok.
 *
 * @param matchReason a Schema Registry által rögzített feloldási indok
 */
public void setMatchReason(String matchReason) { this.matchReason = matchReason; }
/**
 * Visszaadja a következő modellértéket: a feldolgozás elsődleges XSD-állománya.
 *
 * @return a feldolgozás elsődleges XSD-állománya
 */
public Path getPrimaryXsd() { return primaryXsd; }
/**
 * Beállítja a következő modellértéket: a feldolgozás elsődleges XSD-állománya.
 *
 * @param primaryXsd a feldolgozás elsődleges XSD-állománya
 */
public void setPrimaryXsd(Path primaryXsd) { this.primaryXsd = primaryXsd; }
/**
 * Visszaadja a következő modellértéket: a sémacsomaghoz tartozó XSD-állományok listája.
 *
 * @return a sémacsomaghoz tartozó XSD-állományok listája
 */
public List<Path> getXsdFiles() { return xsdFiles; }
/**
 * Beállítja a következő modellértéket: a sémacsomaghoz tartozó XSD-állományok listája.
 *
 * @param xsdFiles a sémacsomaghoz tartozó XSD-állományok listája
 */
public void setXsdFiles(List<Path> xsdFiles) { this.xsdFiles = xsdFiles; }
/**
 * Visszaadja a következő modellértéket: a feloldott UIModel állomány útvonala.
 *
 * @return a feloldott UIModel állomány útvonala
 */
public Path getUiModelFile() { return uiModelFile; }
/**
 * Beállítja a következő modellértéket: a feloldott UIModel állomány útvonala.
 *
 * @param uiModelFile a feloldott UIModel állomány útvonala
 */
public void setUiModelFile(Path uiModelFile) { this.uiModelFile = uiModelFile; }
/**
 * Visszaadja a következő modellértéket: a feloldott page-schema állomány útvonala.
 *
 * @return a feloldott page-schema állomány útvonala
 */
public Path getPageSchemaFile() { return pageSchemaFile; }
/**
 * Beállítja a következő modellértéket: a feloldott page-schema állomány útvonala.
 *
 * @param pageSchemaFile a feloldott page-schema állomány útvonala
 */
public void setPageSchemaFile(Path pageSchemaFile) { this.pageSchemaFile = pageSchemaFile; }

}
