package hu.gov.nav.xsdparsertool.web.xmlfile.dto;

/**
 * A webes rétegek közötti adatátadás strukturált modellje.
 *
 * <p>A {@code LargeXmlFragmentSaveRequest} rekord a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public record LargeXmlFragmentSaveRequest(
        String formName,
        Long occurrenceIndex,
        String xmlFragment,
        String sessionId,
        Long sourceFileSize,
        Long sourceLastModified,
        String userNote
) {}
