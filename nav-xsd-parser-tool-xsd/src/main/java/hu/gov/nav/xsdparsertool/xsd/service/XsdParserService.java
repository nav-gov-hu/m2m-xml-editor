package hu.gov.nav.xsdparsertool.xsd.service;

import hu.gov.nav.xsdparsertool.core.model.bundle.SchemaBundle;
import hu.gov.nav.xsdparsertool.core.model.definition.DocumentDefinition;


/**
 * Az XSD-séma és a hozzá tartozó metaadatok alkalmazásszintű dokumentumdefinícióvá alakításának szolgáltatási szerződése.
 *
 * <p>A hívó réteg egy {@link SchemaBundle} objektumot ad át, amelyből az implementáció a szerkesztő,
 * űrlapmegjelenítő és további feldolgozási lépések által használható {@link DocumentDefinition}
 * modellt állítja elő.</p>
 */
public interface XsdParserService {
    /**
     * Feldolgozza a séma-csomagot és létrehozza a dokumentum szerkezeti definícióját.
     *
     * @param bundle az elsődleges és kapcsolódó XSD-ket, valamint dokumentum-metaadatokat tartalmazó csomag
     * @return az XSD-kből felépített dokumentumdefiníció
     */
    DocumentDefinition parse(SchemaBundle bundle);
}
