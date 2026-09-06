package hu.gov.nav.xsdparsertool.web.xmlfile.dto;

/**
 * A webes rétegek közötti adatátadás strukturált modellje.
 *
 * <p>A {@code ServerFileDto} rekord a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public record ServerFileDto(
        String fileName,
        String path,
        long fileSizeBytes,
        String fileSizeDisplay,
        String lastModified
) {
}
