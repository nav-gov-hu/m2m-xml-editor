package hu.gov.nav.xsdparsertool.web.xsdvalidation.dto;

import hu.gov.nav.xsdparsertool.web.xsdvalidation.entity.XsdValidationErrorEntity;

/**
 * A webes rétegek közötti adatátadás strukturált modellje.
 *
 * <p>A {@code XsdValidationErrorDto} rekord a web modul XSD-validációs területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public record XsdValidationErrorDto(
        Long id,
        String severity,
        String code,
        String message,
        Integer lineNumber,
        Integer columnNumber,
        String path
) {
    /**
     * A {@code from} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param entity a művelet bemeneti {@code entity} értéke
     * @return a művelet feldolgozási eredménye
     */
    public static XsdValidationErrorDto from(XsdValidationErrorEntity entity) {
        return new XsdValidationErrorDto(
                entity.getId(),
                entity.getSeverity(),
                entity.getErrorCode(),
                entity.getErrorMessage(),
                entity.getLineNumber(),
                entity.getColumnNumber(),
                entity.getPath()
        );
    }
}
