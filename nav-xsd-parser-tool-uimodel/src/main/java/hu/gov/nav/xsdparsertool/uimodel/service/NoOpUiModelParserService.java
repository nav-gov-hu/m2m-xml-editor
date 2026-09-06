package hu.gov.nav.xsdparsertool.uimodel.service;

import hu.gov.nav.xsdparsertool.core.model.definition.DocumentDefinition;

import java.nio.file.Path;
/**
 * Olyan {@link UiModelParserService} implementáció, amely szándékosan nem módosítja a dokumentumdefiníciót.
 *
 * <p>Fallback vagy kikapcsolt UIModel-feldolgozás esetén használható, amikor a feldolgozási pipeline-nak
 * meg kell tartania a szolgáltatási szerződést, de az XSD-ből felépített definícióra nem kíván UIModel
 * metaadatokat alkalmazni.</p>
 */
public class NoOpUiModelParserService implements UiModelParserService {
    /**
     * Változtatás nélkül hagyja a megadott dokumentumdefiníciót.
     *
     * @param definition a változatlanul hagyott dokumentumdefiníció
     * @param uiModelFile a figyelmen kívül hagyott UIModel állomány elérési útja
     */
    @Override
    public void applyUiModel(DocumentDefinition definition, Path uiModelFile) {
        // Placeholder for future UI model merge logic.
    }
}
