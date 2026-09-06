package hu.gov.nav.xsdparsertool.xsd.service;

import hu.gov.nav.xsdparsertool.core.model.bundle.SchemaBundle;
import hu.gov.nav.xsdparsertool.core.model.definition.DocumentDefinition;


/**
 * Minimalista {@link XsdParserService} implementáció, amely tényleges XSD-szerkezet feldolgozása nélkül
 * csak a séma-csomag alapmetaadataiból hoz létre {@link DocumentDefinition} objektumot.
 *
 * <p>Elsősorban olyan hívási helyekhez vagy tesztelési/bővítési helyzetekhez használható, ahol a teljes
 * XSD parser működésére nincs szükség. Nem állít elő blokk- és meződefiníciókat.</p>
 */
public class StubXsdParserService implements XsdParserService {
    /**
     * Létrehoz egy minimális dokumentumdefiníciót a séma-csomag dokumentumtípusából és elsődleges XSD-jéből.
     *
     * <p>Az azonosító, név és cím a dokumentumtípus lesz. Ha van elsődleges XSD, annak fájlnevéből a
     * {@code .xsd} kiterjesztés eltávolításával állítja be a gyökérelem nevét. A metódus nem parse-olja
     * a sémafájlt és nem épít blokkokat vagy mezőket.</p>
     *
     * @param bundle a minimális definíció alapadatait tartalmazó séma-csomag
     * @return a metaadatokkal feltöltött, de strukturális tartalom nélküli dokumentumdefiníció
     */
    @Override
    public DocumentDefinition parse(SchemaBundle bundle) {
        DocumentDefinition definition = new DocumentDefinition();
        definition.setId(bundle.getDocumentType());
        definition.setName(bundle.getDocumentType());
        definition.setTitle(bundle.getDocumentType());
        if (bundle.getPrimaryXsd() != null) {
            definition.setRootElementName(bundle.getPrimaryXsd().getFileName().toString().replace(".xsd", ""));
        }
        return definition;
    }
}
