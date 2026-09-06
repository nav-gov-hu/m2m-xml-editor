package hu.gov.nav.xsdparsertool.web.xmlfile.dto;

import java.time.LocalDateTime;

/**
 * A webes rétegek közötti adatátadás strukturált modellje.
 *
 * <p>A {@code XmlSessionStateDto} rekord a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public record XmlSessionStateDto(
        boolean active,
        Long xmlFileId,
        String sessionId,
        boolean lockActive,
        String closedBy,
        LocalDateTime closedAt,
        String closeReason
) {
}
