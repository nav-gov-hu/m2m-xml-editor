package hu.gov.nav.xsdparsertool.web.systemconfig.dto;

import java.util.List;

/**
 * A webes rétegek közötti adatátadás strukturált modellje.
 *
 * <p>A {@code ConfigurationItemDto} rekord a web modul alkalmazási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public record ConfigurationItemDto(
        String key, String label, String description, String category,
        String storage, String type, String value, String defaultValue,
        String source, boolean sensitive, boolean restartRequired,
        boolean advanced, boolean required, boolean missing, boolean databasePersisted, List<String> options) {
}
