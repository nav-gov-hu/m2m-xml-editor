package hu.gov.nav.xsdparsertool.web.xmlfile.dto;

import java.time.LocalDateTime;
import java.util.List;

import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileRevisionEntity;

/**
 * A webes rétegek közötti adatátadás strukturált modellje.
 *
 * <p>A {@code XmlRevisionDto} rekord a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public record XmlRevisionDto(
        Long id,
        Long xmlFileId,
        Integer revisionNo,
        String saveType,
        String targetFilePath,
        String backupFilePath,
        String diffSummary,
        Integer changeCount,
        Boolean xsdValidationRequested,
        String xsdValidationStatus,
        String userNote,
        LocalDateTime createdAt,
        String createdBy,
        List<XmlDiffEntryDto> diffEntries
) {
    /**
     * A {@code from} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param entity a művelet bemeneti {@code entity} értéke
     * @param diffEntries a feldolgozandó elemek kollekciója
     * @return a művelet feldolgozási eredménye
     */
    public static XmlRevisionDto from(XmlFileRevisionEntity entity, List<XmlDiffEntryDto> diffEntries) {
        return new XmlRevisionDto(
                entity.getId(),
                entity.getXmlFile() == null ? null : entity.getXmlFile().getId(),
                entity.getRevisionNo(),
                entity.getSaveType(),
                entity.getTargetFilePath(),
                entity.getBackupFilePath(),
                entity.getDiffSummary(),
                entity.getChangeCount(),
                entity.getXsdValidationRequested(),
                entity.getXsdValidationStatus(),
                entity.getUserNote(),
                entity.getCreatedAt(),
                entity.getCreatedBy(),
                diffEntries
        );
    }
}
