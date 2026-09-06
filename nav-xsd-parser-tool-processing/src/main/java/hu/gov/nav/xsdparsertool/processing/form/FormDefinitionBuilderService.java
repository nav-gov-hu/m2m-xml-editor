package hu.gov.nav.xsdparsertool.processing.form;

import hu.gov.nav.xsdparsertool.core.model.bundle.SchemaBundle;
import hu.gov.nav.xsdparsertool.core.model.definition.DocumentDefinition;
import hu.gov.nav.xsdparsertool.core.model.form.FormDefinition;


/**
 * A dokumentum metaadataiból megjeleníthető űrlapdefiníciót felépítő szolgáltatás.
 *
 * <p>A kimeneti modell tabokból, szekciókból, sorokból és mezőkből áll, és közvetlenül
 * felhasználható a webes vagy nyomtatási megjelenítéshez.</p>
 */
public interface FormDefinitionBuilderService {
/**
 * Felépíti a megjelenítéshez használható űrlapdefiníciót.
 * @param documentDefinition az XSD és kiegészítő metaadatok dokumentumdefiníciója
 * @param schemaBundle a feloldott séma-csomag
 * @return a megjelenítési űrlapdefiníció
 */
    FormDefinition build(DocumentDefinition documentDefinition, SchemaBundle schemaBundle);
}
