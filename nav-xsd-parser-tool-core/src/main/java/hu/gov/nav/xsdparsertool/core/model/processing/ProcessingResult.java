package hu.gov.nav.xsdparsertool.core.model.processing;

import hu.gov.nav.xsdparsertool.core.model.bundle.SchemaBundle;
import hu.gov.nav.xsdparsertool.core.model.definition.DocumentDefinition;
import hu.gov.nav.xsdparsertool.core.model.instance.DocumentInstance;
import hu.gov.nav.xsdparsertool.core.model.validation.ValidationIssue;

import java.util.ArrayList;
import java.util.List;
/**
 * A teljes XML-feldolgozási pipeline összesített eredményobjektuma.
 *
 * <p>Egy helyen adja vissza a feloldott sémacsomagot, a strukturális definíciót,
 * a konkrét dokumentumpéldányt és az összegyűjtött validációs problémákat.</p>
 */
public class ProcessingResult {
    private SchemaBundle schemaBundle;
    private DocumentDefinition documentDefinition;
    private DocumentInstance documentInstance;
    private List<ValidationIssue> validationIssues = new ArrayList<>();
/**
 * Visszaadja a következő modellértéket: a feldolgozáshoz feloldott sémacsomag.
 *
 * @return a feldolgozáshoz feloldott sémacsomag
 */
public SchemaBundle getSchemaBundle() {
        return schemaBundle;
    }
/**
 * Beállítja a következő modellértéket: a feldolgozáshoz feloldott sémacsomag.
 *
 * @param schemaBundle a feldolgozáshoz feloldott sémacsomag
 */
public void setSchemaBundle(SchemaBundle schemaBundle) {
        this.schemaBundle = schemaBundle;
    }
/**
 * Visszaadja a következő modellértéket: az XSD/UIModel alapján előállított dokumentumdefiníció.
 *
 * @return az XSD/UIModel alapján előállított dokumentumdefiníció
 */
public DocumentDefinition getDocumentDefinition() {
        return documentDefinition;
    }
/**
 * Beállítja a következő modellértéket: az XSD/UIModel alapján előállított dokumentumdefiníció.
 *
 * @param documentDefinition az XSD/UIModel alapján előállított dokumentumdefiníció
 */
public void setDocumentDefinition(DocumentDefinition documentDefinition) {
        this.documentDefinition = documentDefinition;
    }
/**
 * Visszaadja a következő modellértéket: a konkrét XML-ből felépített dokumentumpéldány.
 *
 * @return a konkrét XML-ből felépített dokumentumpéldány
 */
public DocumentInstance getDocumentInstance() {
        return documentInstance;
    }
/**
 * Beállítja a következő modellértéket: a konkrét XML-ből felépített dokumentumpéldány.
 *
 * @param documentInstance a konkrét XML-ből felépített dokumentumpéldány
 */
public void setDocumentInstance(DocumentInstance documentInstance) {
        this.documentInstance = documentInstance;
    }
/**
 * Visszaadja a következő modellértéket: a feldolgozás során összegyűjtött validációs problémák.
 *
 * @return a feldolgozás során összegyűjtött validációs problémák
 */
public List<ValidationIssue> getValidationIssues() {
        return validationIssues;
    }
/**
 * Beállítja a következő modellértéket: a feldolgozás során összegyűjtött validációs problémák.
 *
 * @param validationIssues a feldolgozás során összegyűjtött validációs problémák
 */
public void setValidationIssues(List<ValidationIssue> validationIssues) {
        this.validationIssues = validationIssues;
    }
}
