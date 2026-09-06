package hu.gov.nav.xsdparsertool.web.systemconfig.dto;

import java.util.Map;
import java.util.Set;

/**
 * A webes rétegek közötti adatátadás strukturált modellje.
 *
 * <p>A {@code ConfigurationSaveRequest} rekord a web modul alkalmazási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public record ConfigurationSaveRequest(Map<String, String> values, Set<String> confirmedSensitiveKeys) {
}
