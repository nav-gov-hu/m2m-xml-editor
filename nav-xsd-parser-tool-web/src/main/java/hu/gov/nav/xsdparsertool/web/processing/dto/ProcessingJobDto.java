package hu.gov.nav.xsdparsertool.web.processing.dto;

import java.time.LocalDateTime;

import hu.gov.nav.xsdparsertool.web.processing.entity.ProcessingJobEntity;

/**
 * A webes rétegek közötti adatátadás strukturált modellje.
 *
 * <p>A {@code ProcessingJobDto} rekord a web modul feldolgozási job területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public record ProcessingJobDto(
        String jobId,
        Long xmlFileId,
        String jobType,
        String status,
        Integer progressPercent,
        String progressMessage,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        LocalDateTime requestedCancelAt,
        String errorMessage,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * A {@code from} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param entity a művelet bemeneti {@code entity} értéke
     * @return a művelet feldolgozási eredménye
     */
    public static ProcessingJobDto from(ProcessingJobEntity entity) {
        Long xmlFileId = entity.getXmlFile() == null ? null : entity.getXmlFile().getId();
        return new ProcessingJobDto(
                entity.getJobId(),
                xmlFileId,
                entity.getJobType(),
                entity.getStatus(),
                entity.getProgressPercent(),
                entity.getProgressMessage(),
                entity.getStartedAt(),
                entity.getFinishedAt(),
                entity.getRequestedCancelAt(),
                entity.getErrorMessage(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
