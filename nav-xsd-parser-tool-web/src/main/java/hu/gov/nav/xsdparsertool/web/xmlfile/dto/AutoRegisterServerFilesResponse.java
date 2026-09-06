package hu.gov.nav.xsdparsertool.web.xmlfile.dto;

import java.util.List;

/**
 * A webes rétegek közötti adatátadás strukturált modellje.
 *
 * <p>A {@code AutoRegisterServerFilesResponse} rekord a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public record AutoRegisterServerFilesResponse(
        boolean enabled,
        String rootDir,
        int scannedCount,
        int registeredCount,
        int skippedCount,
        List<XmlFileDto> registeredFiles,
        List<String> warnings
) {
}
