package hu.gov.nav.xsdparsertool.web.security.usermanagement.dto;

/**
 * A webes rétegek közötti adatátadás strukturált modellje.
 *
 * <p>A {@code PasswordPolicyDto} rekord a web modul biztonsági és jogosultságkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public record PasswordPolicyDto(int minimumLength, int maximumLength, int historySize,
        int maximumFailedAttempts, long lockDurationMinutes, boolean periodicPasswordChangeRequired) { }
