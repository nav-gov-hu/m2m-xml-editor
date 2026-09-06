package hu.gov.nav.xsdparsertool.print.service;

import hu.gov.nav.xsdparsertool.print.model.PrintOptions;

import java.nio.file.Path;

/**
 * XML dokumentumok nyomtatható HTML- és PDF-reprezentációjának előállítására szolgáló szolgáltatás.
 *
 * <p>A nyomtatási folyamat az XML állomány tartalmát a hozzá tartozó XSD-sémák és UIModel
 * metaadatok alapján űrlapdefinícióvá és űrlapadattá alakítja, majd ezekből nyomtatásra optimalizált
 * kimenetet készít. A konkrét implementáció feladata a séma- és UIModel-feloldás, valamint a
 * nyomtatási beállítások érvényesítése.</p>
 */
public interface XmlPrintService {
    /**
     * Nyomtatásra optimalizált HTML-dokumentumot állít elő a megadott XML állományból.
     *
     * @param xmlFile a feldolgozandó XML állomány elérési útja
     * @param schemaRootDir a nyomtatványhoz tartozó XSD-sémák feloldásának gyökérkönyvtára
     * @param generalXsdDir az általános, közösen használt XSD-sémák könyvtára
     * @param uiModelDir a UIModel állományok alapértelmezett keresési könyvtára
     * @param options a nyomtatási beállítások; {@code null} esetén az implementáció alapértelmezett beállításokat használ
     * @return a teljes, önálló nyomtatható HTML-dokumentum
     */
    String generateHtml(Path xmlFile,
                        Path schemaRootDir,
                        Path generalXsdDir,
                        Path uiModelDir,
                        PrintOptions options);

    /**
     * PDF dokumentumot állít elő a megadott XML állomány nyomtatható HTML-reprezentációjából.
     *
     * @param xmlFile a feldolgozandó XML állomány elérési útja
     * @param schemaRootDir a nyomtatványhoz tartozó XSD-sémák feloldásának gyökérkönyvtára
     * @param generalXsdDir az általános, közösen használt XSD-sémák könyvtára
     * @param uiModelDir a UIModel állományok alapértelmezett keresési könyvtára
     * @param options a nyomtatási beállítások; {@code null} esetén az implementáció alapértelmezett beállításokat használ
     * @return a létrehozott PDF dokumentum bájtjai
     * @throws IllegalStateException ha a HTML-ből történő PDF-előállítás nem hajtható végre
     */
    byte[] generatePdf(Path xmlFile,
                       Path schemaRootDir,
                       Path generalXsdDir,
                       Path uiModelDir,
                       PrintOptions options);
}
