package hu.gov.nav.xsdparsertool.web.security.usermanagement.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A webes rétegek közötti adatátadás strukturált modellje.
 *
 * <p>A {@code UserDto} rekord a web modul biztonsági és jogosultságkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public record UserDto(Long id, String username, String displayName, String email, boolean enabled,
        boolean passwordChangeRequired, int failedLoginAttempts, LocalDateTime lockedUntil,
        List<String> roles, LocalDateTime createdAt, String createdBy, LocalDateTime updatedAt, String updatedBy) {
}
