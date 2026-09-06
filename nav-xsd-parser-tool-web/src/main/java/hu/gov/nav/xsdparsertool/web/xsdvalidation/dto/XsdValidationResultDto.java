package hu.gov.nav.xsdparsertool.web.xsdvalidation.dto;

import java.util.List;

/**
 * A webes rétegek közötti adatátadás strukturált modellje.
 *
 * <p>A {@code XsdValidationResultDto} rekord a web modul XSD-validációs területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public record XsdValidationResultDto(
        XsdValidationRequestDto request,
        List<XsdValidationErrorDto> errors
) {
}
