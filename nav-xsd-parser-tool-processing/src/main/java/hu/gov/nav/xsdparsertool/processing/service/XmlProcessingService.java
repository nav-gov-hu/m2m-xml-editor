package hu.gov.nav.xsdparsertool.processing.service;

import hu.gov.nav.xsdparsertool.core.model.processing.ExportResult;
import hu.gov.nav.xsdparsertool.core.model.processing.ProcessingResult;
import hu.gov.nav.xsdparsertool.core.model.processing.ValidationResult;

import java.nio.file.Path;

/**
 * Az XML-feldolgozási folyamat magas szintű szolgáltatási szerződése.
 *
 * <p>Feladata az XML-hez tartozó séma-csomag feloldása, a dokumentumdefiníció felépítése,
 * az XSD-validáció indítása és dokumentumtípus alapján minimális XML előállítása.</p>
 */
public interface XmlProcessingService {
/**
 * Feldolgozza az XML-t az alapértelmezett séma-gyökér használatával.
 * @param xmlFile a feldolgozandó XML állomány
 * @param schemaRootDir az XSD-k gyökérkönyvtára
 * @return a feloldott séma-csomagot és dokumentumdefiníciót tartalmazó eredmény
 */
    ProcessingResult inspect(Path xmlFile, Path schemaRootDir);
/**
 * Feldolgozza az XML-t külön általános XSD könyvtár figyelembevételével.
 * @param xmlFile a feldolgozandó XML állomány
 * @param schemaRootDir a dokumentumspecifikus XSD-k gyökérkönyvtára
 * @param generalXsdDir az általános XSD-k könyvtára, vagy {@code null}
 * @return a feldolgozás eredménye
 */
    ProcessingResult inspect(Path xmlFile, Path schemaRootDir, Path generalXsdDir);
/**
 * Feldolgozza az XML-t a teljes séma- és UIModel-környezettel.
 * @param xmlFile a feldolgozandó XML állomány
 * @param schemaRootDir a dokumentumspecifikus XSD-k gyökérkönyvtára
 * @param generalXsdDir az általános XSD-k könyvtára, vagy {@code null}
 * @param uiModelDir a UIModel könyvtár, vagy {@code null}
 * @return a feldolgozás eredménye
 */
    ProcessingResult inspect(Path xmlFile, Path schemaRootDir, Path generalXsdDir, Path uiModelDir);

/**
 * XSD szerint validálja az XML-t.
 * @param xmlFile a validálandó XML állomány
 * @param schemaRootDir az XSD-k gyökérkönyvtára
 * @return a validáció eredménye és az észlelt hibák
 */
    ValidationResult validate(Path xmlFile, Path schemaRootDir);
/**
 * XSD szerint validálja az XML-t külön general XSD könyvtárral.
 * @param xmlFile a validálandó XML állomány
 * @param schemaRootDir a dokumentumspecifikus XSD-k gyökérkönyvtára
 * @param generalXsdDir az általános XSD-k könyvtára, vagy {@code null}
 * @return a validáció eredménye
 */
    ValidationResult validate(Path xmlFile, Path schemaRootDir, Path generalXsdDir);
/**
 * XSD szerint validálja az XML-t a teljes erőforráskörnyezettel.
 * @param xmlFile a validálandó XML állomány
 * @param schemaRootDir a dokumentumspecifikus XSD-k gyökérkönyvtára
 * @param generalXsdDir az általános XSD-k könyvtára, vagy {@code null}
 * @param uiModelDir a UIModel könyvtár, vagy {@code null}
 * @return a validáció eredménye
 */
    ValidationResult validate(Path xmlFile, Path schemaRootDir, Path generalXsdDir, Path uiModelDir);

/**
 * Minimális XML állományt generál a megadott dokumentumtípushoz.
 * @param documentType a dokumentumtípus technikai azonosítója
 * @param schemaRootDir az XSD-k gyökérkönyvtára
 * @param outputFile a létrehozandó XML célfájlja
 * @return az export eredménye
 */
    ExportResult generateEmptyXml(String documentType, Path schemaRootDir, Path outputFile);
/**
 * Minimális XML állományt generál külön general XSD könyvtárral.
 * @param documentType a dokumentumtípus technikai azonosítója
 * @param schemaRootDir a dokumentumspecifikus XSD-k gyökérkönyvtára
 * @param generalXsdDir az általános XSD-k könyvtára, vagy {@code null}
 * @param outputFile a létrehozandó XML célfájlja
 * @return az export eredménye
 */
    ExportResult generateEmptyXml(String documentType, Path schemaRootDir, Path generalXsdDir, Path outputFile);
}
