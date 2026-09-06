package hu.gov.nav.xsdparsertool.web.systemconfig.dto;

import java.util.List;

/**
 * A webes rétegek közötti adatátadás strukturált modellje.
 *
 * <p>A {@code ConfigurationSaveResponse} rekord a web modul alkalmazási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public record ConfigurationSaveResponse(int savedDatabaseValues, int savedBootstrapValues,
                                        boolean restartRequired, List<String> changedKeys,
                                        String bootstrapFile) {
}
