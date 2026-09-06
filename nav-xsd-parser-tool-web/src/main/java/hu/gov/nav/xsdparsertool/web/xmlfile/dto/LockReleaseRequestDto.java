package hu.gov.nav.xsdparsertool.web.xmlfile.dto;

import java.time.LocalDateTime;

import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileLockReleaseRequestEntity;

/**
 * A webes rétegek közötti adatátadás strukturált modellje.
 *
 * <p>A {@code LockReleaseRequestDto} rekord a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public record LockReleaseRequestDto(
        Long id,
        Long xmlFileId,
        String fileName,
        String requesterUsername,
        String ownerUsername,
        String status,
        String message,
        String responseMessage,
        LocalDateTime requestedAt,
        LocalDateTime respondedAt,
        String closedBy,
        LocalDateTime forceClosedAt
) {
    /**
     * A {@code from} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param entity a művelet bemeneti {@code entity} értéke
     * @return a művelet feldolgozási eredménye
     */
    public static LockReleaseRequestDto from(XmlFileLockReleaseRequestEntity entity) {
        return new LockReleaseRequestDto(
                entity.getId(),
                entity.getXmlFile() == null ? null : entity.getXmlFile().getId(),
                entity.getXmlFile() == null ? null : entity.getXmlFile().getFileName(),
                entity.getRequesterUsername(),
                entity.getOwnerUsername(),
                entity.getStatus(),
                entity.getMessage(),
                entity.getResponseMessage(),
                entity.getRequestedAt(),
                entity.getRespondedAt(),
                entity.getClosedBy(),
                entity.getForceClosedAt()
        );
    }
}
