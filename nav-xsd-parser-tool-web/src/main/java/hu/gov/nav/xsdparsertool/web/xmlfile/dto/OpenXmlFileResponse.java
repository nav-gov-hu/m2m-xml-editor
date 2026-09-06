package hu.gov.nav.xsdparsertool.web.xmlfile.dto;

import java.time.LocalDateTime;

/**
 * A webes rétegek közötti adatátadás strukturált modellje.
 *
 * <p>A {@code OpenXmlFileResponse} rekord a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public record OpenXmlFileResponse(
        XmlFileDto file,
        String sessionId,
        boolean readOnly,
        boolean locked,
        String lockOwner,
        LocalDateTime lockExpiresAt,
        String message,
        boolean schemaVersionFallback,
        String xmlFormVersion,
        String resolvedXsdVersion
) {
    /**
     * Visszafelé kompatibilis konstruktor a verzió-kompatibilitási metaadatok nélküli hívásokhoz.
     */
    public OpenXmlFileResponse(XmlFileDto file, String sessionId, boolean readOnly, boolean locked,
                               String lockOwner, LocalDateTime lockExpiresAt, String message) {
        this(file, sessionId, readOnly, locked, lockOwner, lockExpiresAt, message, false, null, null);
    }
}
