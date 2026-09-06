package hu.gov.nav.xsdparsertool.schemaregistry.service;

import hu.gov.nav.xsdparsertool.core.model.bundle.SchemaBundle;
import hu.gov.nav.xsdparsertool.schemaregistry.model.XmlProbeResult;

import java.nio.file.Path;



/**
 * A dokumentumhoz tartozó séma-csomag feloldásának programozói belépési pontja.
 * XML-elővizsgálati adatok vagy ismert dokumentumtípus alapján adja vissza az elsődleges XSD-t és a kapcsolódó erőforrásokat tartalmazó {@link SchemaBundle} objektumot.
 */
public interface SchemaRegistryService {
    /**
     * Feloldja az XML elővizsgálati adataihoz legjobban illeszkedő séma-csomagot.
     * @param probeResult az XML elővizsgálatából származó gyökérelem-, namespace- és sémahelyadatok.
     * @param schemaRootDir a nyomtatványspecifikus XSD-k gyökérkönyvtára.
     * @return a kiválasztott {@link SchemaBundle}.
     */
    SchemaBundle resolveByXmlProbe(XmlProbeResult probeResult, Path schemaRootDir);
    /**
     * Feloldja az XML elővizsgálati adataihoz legjobban illeszkedő séma-csomagot.
     * @param probeResult az XML elővizsgálatából származó gyökérelem-, namespace- és sémahelyadatok.
     * @param schemaRootDir a nyomtatványspecifikus XSD-k gyökérkönyvtára.
     * @param generalXsdDir a közös/general XSD-k könyvtára; opcionális.
     * @return a kiválasztott {@link SchemaBundle}.
     */
    SchemaBundle resolveByXmlProbe(XmlProbeResult probeResult, Path schemaRootDir, Path generalXsdDir);
    /**
     * Feloldja az XML elővizsgálati adataihoz legjobban illeszkedő séma-csomagot.
     * @param probeResult az XML elővizsgálatából származó gyökérelem-, namespace- és sémahelyadatok.
     * @param schemaRootDir a nyomtatványspecifikus XSD-k gyökérkönyvtára.
     * @param generalXsdDir a közös/general XSD-k könyvtára; opcionális.
     * @param uiModelDir a UIModel és kísérőfájlok keresési gyökérkönyvtára; opcionális.
     * @return a kiválasztott {@link SchemaBundle}.
     */
    SchemaBundle resolveByXmlProbe(XmlProbeResult probeResult, Path schemaRootDir, Path generalXsdDir, Path uiModelDir);
    /**
     * Feloldja az ismert dokumentumtípushoz legjobban illeszkedő séma-csomagot.
     * @param documentType a feloldandó dokumentumtípus vagy gyökérelem-azonosító.
     * @param schemaRootDir a nyomtatványspecifikus XSD-k gyökérkönyvtára.
     * @return a kiválasztott {@link SchemaBundle}.
     * @throws IllegalArgumentException ha a dokumentumtípushoz nem található megfelelő XSD.
     */
    SchemaBundle resolveByDocumentType(String documentType, Path schemaRootDir);
    /**
     * Feloldja az ismert dokumentumtípushoz legjobban illeszkedő séma-csomagot.
     * @param documentType a feloldandó dokumentumtípus vagy gyökérelem-azonosító.
     * @param schemaRootDir a nyomtatványspecifikus XSD-k gyökérkönyvtára.
     * @param generalXsdDir a közös/general XSD-k könyvtára; opcionális.
     * @return a kiválasztott {@link SchemaBundle}.
     * @throws IllegalArgumentException ha a dokumentumtípushoz nem található megfelelő XSD.
     */
    SchemaBundle resolveByDocumentType(String documentType, Path schemaRootDir, Path generalXsdDir);
    /**
     * Feloldja az ismert dokumentumtípushoz legjobban illeszkedő séma-csomagot.
     * @param documentType a feloldandó dokumentumtípus vagy gyökérelem-azonosító.
     * @param schemaRootDir a nyomtatványspecifikus XSD-k gyökérkönyvtára.
     * @param generalXsdDir a közös/general XSD-k könyvtára; opcionális.
     * @param uiModelDir a UIModel és kísérőfájlok keresési gyökérkönyvtára; opcionális.
     * @return a kiválasztott {@link SchemaBundle}.
     * @throws IllegalArgumentException ha a dokumentumtípushoz nem található megfelelő XSD.
     */
    SchemaBundle resolveByDocumentType(String documentType, Path schemaRootDir, Path generalXsdDir, Path uiModelDir);
}
