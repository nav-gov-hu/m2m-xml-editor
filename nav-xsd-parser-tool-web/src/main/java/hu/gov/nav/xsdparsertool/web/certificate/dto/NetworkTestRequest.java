package hu.gov.nav.xsdparsertool.web.certificate.dto;
/**
 * A webes rétegek közötti adatátadás strukturált modellje.
 *
 * <p>A {@code NetworkTestRequest} rekord a web modul alkalmazási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public record NetworkTestRequest(String url){}
