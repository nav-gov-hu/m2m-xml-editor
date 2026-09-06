package hu.gov.nav.xsdparsertool.uimodel.service;

import hu.gov.nav.xsdparsertool.core.model.definition.DocumentDefinition;

import java.nio.file.Path;
/**
 * Az XSD-ből felépített dokumentumdefiníció UIModel-adatokkal történő kiegészítésének szolgáltatási szerződése.
 *
 * <p>A szolgáltatás a megadott UIModel állomány vizuális és mezőszintű metaadatait a már létező
 * {@link DocumentDefinition} példányra alkalmazza. A hívás módosíthatja a dokumentum címét,
 * a blokkok megnevezését, valamint a mezők címkéjét, típusát, maszkját és maximális hosszát,
 * amennyiben az adott implementáció ezeket támogatja.</p>
 */
public interface UiModelParserService {
    /**
     * Alkalmazza a megadott UIModel állományból feloldható metaadatokat a dokumentumdefinícióra.
     *
     * @param definition az XSD alapján már felépített, módosítandó dokumentumdefiníció
     * @param uiModelFile a feldolgozandó UIModel XML állomány elérési útja
     */
    void applyUiModel(DocumentDefinition definition, Path uiModelFile);
}
