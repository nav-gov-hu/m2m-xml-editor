package hu.gov.nav.xsdparsertool.web.security.usermanagement.dto;

import java.util.Set;

/**
 * A webes rétegek közötti adatátadás strukturált modellje.
 *
 * <p>A {@code UserSaveRequest} rekord a web modul biztonsági és jogosultságkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public record UserSaveRequest(String username, String displayName, String email, Boolean enabled,
        Boolean passwordChangeRequired, String password, Set<String> roles) {
}
