package hu.gov.nav.xsdparsertool.web.xmlfile.dto;

import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import java.time.LocalDateTime;
import java.nio.file.Files;
import java.nio.file.Path;

import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileLockEntity;
import hu.gov.nav.xsdparsertool.web.xsdvalidation.entity.XsdValidationRequestEntity;

/**
 * A webes rétegek közötti adatátadás strukturált modellje.
 *
 * <p>A {@code XmlFileDto} rekord a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public record XmlFileDto(
        Long id,
        String fileName,
        String originalFileName,
        String filePath,
        Long fileSizeBytes,
        String fileSizeDisplay,
        String formType,
        String formVersion,
        String rootElement,
        String namespaceUri,
        String schemaLocation,
        String noNamespaceSchemaLocation,
        String xsdPath,
        String uiModelPath,
        String xpathRulesPath,
        Boolean xsdExists,
        Boolean uiModelExists,
        Boolean xpathRulesExists,
        Long partnerId,
        String partnerTaxNumber,
        String partnerName,
        String partnerImportStatus,
        String partnerImportMessage,
        String resolutionStatus,
        String resolutionMessage,
        String userNote,
        String sourceType,
        String status,
        Boolean largeFileMode,
        Boolean archived,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy,
        LocalDateTime archivedAt,
        String archivedBy,
        Boolean locked,
        String displayStatus,
        String lockedBy,
        LocalDateTime lockedAt,
        LocalDateTime lockExpiresAt,
        String lockClientIp,
        String lockUserAgent,
        String lockBrowserSessionId,
        String activeSessionId,
        Long revisionCount,
        String latestXsdRequestId,
        String latestXsdStatus,
        String latestXsdResultStatus,
        Integer latestXsdErrorCount,
        Integer latestXsdWarningCount,
        LocalDateTime latestXsdFinishedAt
) {
    /**
     * A {@code from} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param entity a művelet bemeneti {@code entity} értéke
     * @return a művelet feldolgozási eredménye
     */
    public static XmlFileDto from(XmlFileEntity entity) {
        return from(entity, null);
    }

    /**
     * A {@code from} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param entity a művelet bemeneti {@code entity} értéke
     * @param activeLock a művelet bemeneti {@code activeLock} értéke
     * @return a művelet feldolgozási eredménye
     */
    public static XmlFileDto from(XmlFileEntity entity, XmlFileLockEntity activeLock) {
        return from(entity, activeLock, null, null);
    }

    /**
     * A {@code from} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param entity a művelet bemeneti {@code entity} értéke
     * @param activeLock a művelet bemeneti {@code activeLock} értéke
     * @param activeSessionId a célobjektum vagy erőforrás azonosítója
     * @return a művelet feldolgozási eredménye
     */
    public static XmlFileDto from(XmlFileEntity entity, XmlFileLockEntity activeLock, String activeSessionId) {
        return from(entity, activeLock, activeSessionId, null, null);
    }

    /**
     * A {@code from} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param entity a művelet bemeneti {@code entity} értéke
     * @param activeLock a művelet bemeneti {@code activeLock} értéke
     * @param activeSessionId a célobjektum vagy erőforrás azonosítója
     * @param revisionCount a művelet bemeneti {@code revisionCount} értéke
     * @return a művelet feldolgozási eredménye
     */
    public static XmlFileDto from(XmlFileEntity entity, XmlFileLockEntity activeLock, String activeSessionId, Long revisionCount) {
        return from(entity, activeLock, activeSessionId, revisionCount, null);
    }

    /**
     * A {@code from} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param entity a művelet bemeneti {@code entity} értéke
     * @param activeLock a művelet bemeneti {@code activeLock} értéke
     * @param activeSessionId a célobjektum vagy erőforrás azonosítója
     * @param revisionCount a művelet bemeneti {@code revisionCount} értéke
     * @param latestXsdRequest a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet feldolgozási eredménye
     */
    public static XmlFileDto from(XmlFileEntity entity, XmlFileLockEntity activeLock, String activeSessionId, Long revisionCount, XsdValidationRequestEntity latestXsdRequest) {
        boolean hasActiveLock = activeLock != null
                && "ACTIVE".equalsIgnoreCase(activeLock.getStatus())
                && activeLock.getLockExpiresAt() != null
                && activeLock.getLockExpiresAt().isAfter(LocalDateTime.now());
        String baseStatus = entity.getStatus();
        String displayStatus = hasActiveLock ? String.valueOf(baseStatus == null || baseStatus.isBlank() ? "-" : baseStatus) + " 🔒" : baseStatus;
        return new XmlFileDto(
                entity.getId(),
                entity.getFileName(),
                entity.getOriginalFileName(),
                entity.getFilePath(),
                entity.getFileSizeBytes(),
                formatSize(entity.getFileSizeBytes()),
                entity.getFormType(),
                entity.getFormVersion(),
                entity.getRootElement(),
                entity.getNamespaceUri(),
                entity.getSchemaLocation(),
                entity.getNoNamespaceSchemaLocation(),
                entity.getXsdPath(),
                entity.getUiModelPath(),
                entity.getXpathRulesPath(),
                isRegularFile(entity.getXsdPath()),
                isRegularFile(entity.getUiModelPath()),
                isRegularFile(entity.getXpathRulesPath()),
                entity.getPartner() == null ? null : entity.getPartner().getId(),
                entity.getPartner() == null ? null : entity.getPartner().getTaxNumber(),
                entity.getPartner() == null ? null : entity.getPartner().getName(),
                entity.getPartnerImportStatus(),
                entity.getPartnerImportMessage(),
                entity.getResolutionStatus(),
                entity.getResolutionMessage(),
                entity.getUserNote(),
                entity.getSourceType(),
                entity.getStatus(),
                entity.getLargeFileMode(),
                entity.getArchived(),
                entity.getCreatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedAt(),
                entity.getUpdatedBy(),
                entity.getArchivedAt(),
                entity.getArchivedBy(),
                hasActiveLock,
                displayStatus,
                hasActiveLock ? activeLock.getLockedBy() : null,
                hasActiveLock ? activeLock.getLockedAt() : null,
                hasActiveLock ? activeLock.getLockExpiresAt() : null,
                hasActiveLock ? activeLock.getLockClientIp() : null,
                hasActiveLock ? activeLock.getLockUserAgent() : null,
                hasActiveLock ? activeLock.getLockBrowserSessionId() : null,
                hasActiveLock ? activeSessionId : null,
                revisionCount == null ? 0L : revisionCount,
                latestXsdRequest == null ? null : latestXsdRequest.getRequestId(),
                latestXsdRequest == null ? null : latestXsdRequest.getStatus(),
                latestXsdRequest == null ? null : latestXsdRequest.getResultStatus(),
                latestXsdRequest == null ? null : latestXsdRequest.getErrorCount(),
                latestXsdRequest == null ? null : latestXsdRequest.getWarningCount(),
                latestXsdRequest == null ? null : latestXsdRequest.getFinishedAt());
    }

    /**
     * A {@code isRegularFile} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param value a művelet bemeneti {@code value} értéke
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    private static boolean isRegularFile(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            return ExceptionSafeOperations.isRegularFile(Path.of(value));
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /**
     * A {@code formatSize} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param bytes a művelet bemeneti {@code bytes} értéke
     * @return a művelet feldolgozási eredménye
     */
    private static String formatSize(Long bytes) {
        if (bytes == null) {
            return "-";
        }
        double value = bytes;
        String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = 0;
        while (value >= 1024 && unitIndex < units.length - 1) {
            value = value / 1024;
            unitIndex++;
        }
        if (unitIndex == 0) {
            return bytes + " B";
        }
        return String.format(java.util.Locale.ROOT, "%.2f %s", value, units[unitIndex]);
    }
}
