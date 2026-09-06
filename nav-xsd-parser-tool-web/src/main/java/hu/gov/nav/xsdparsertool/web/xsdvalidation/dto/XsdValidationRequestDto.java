package hu.gov.nav.xsdparsertool.web.xsdvalidation.dto;

import java.time.LocalDateTime;

import hu.gov.nav.xsdparsertool.web.xsdvalidation.entity.XsdValidationRequestEntity;

/**
 * A webes rétegek közötti adatátadás strukturált modellje.
 *
 * <p>A {@code XsdValidationRequestDto} rekord a web modul XSD-validációs területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public record XsdValidationRequestDto(
        String requestId,
        String jobId,
        Long xmlFileId,
        String xmlFileName,
        String formType,
        String formVersion,
        String xsdPath,
        String status,
        String resultStatus,
        Integer errorCount,
        Integer warningCount,
        Integer infoCount,
        Boolean maxErrorsReached,
        String technicalErrorMessage,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        LocalDateTime createdAt
) {
    /**
     * A {@code from} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param entity a művelet bemeneti {@code entity} értéke
     * @return a művelet feldolgozási eredménye
     */
    public static XsdValidationRequestDto from(XsdValidationRequestEntity entity) {
        return new XsdValidationRequestDto(
                entity.getRequestId(),
                entity.getJobId(),
                entity.getXmlFile() == null ? null : entity.getXmlFile().getId(),
                entity.getXmlFile() == null ? null : entity.getXmlFile().getFileName(),
                entity.getXmlFile() == null ? null : entity.getXmlFile().getFormType(),
                entity.getXmlFile() == null ? null : entity.getXmlFile().getFormVersion(),
                entity.getXsdPath(),
                entity.getStatus(),
                entity.getResultStatus(),
                entity.getErrorCount(),
                entity.getWarningCount(),
                entity.getInfoCount(),
                entity.getMaxErrorsReached(),
                entity.getTechnicalErrorMessage(),
                entity.getStartedAt(),
                entity.getFinishedAt(),
                entity.getCreatedAt()
        );
    }
}
