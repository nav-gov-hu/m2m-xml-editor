package hu.gov.nav.xsdparsertool.web.xmlfile.dto;

import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A webes rétegek közötti adatátadás strukturált modellje.
 *
 * <p>A {@code XmlResolverInfoDto} rekord a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public record XmlResolverInfoDto(
        Long id,
        String fileName,
        String filePath,
        String rootElement,
        String namespaceUri,
        String schemaLocation,
        String noNamespaceSchemaLocation,
        String formType,
        String formVersion,
        String xsdPath,
        String uiModelPath,
        String xpathRulesPath,
        Boolean xsdExists,
        Boolean uiModelExists,
        Boolean xpathRulesExists,
        String resolutionStatus,
        String resolutionMessage,
        String storedFormType,
        String storedFormVersion,
        String storedXsdPath,
        String storedUiModelPath,
        String storedXpathRulesPath
) {
    /**
     * A {@code isRegularFile} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param value a művelet bemeneti {@code value} értéke
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    public static boolean isRegularFile(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            return ExceptionSafeOperations.isRegularFile(Path.of(value));
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
