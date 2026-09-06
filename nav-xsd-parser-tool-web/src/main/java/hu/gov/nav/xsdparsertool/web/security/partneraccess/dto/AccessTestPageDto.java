package hu.gov.nav.xsdparsertool.web.security.partneraccess.dto;
import java.util.List;
/**
 * A webes rétegek közötti adatátadás strukturált modellje.
 *
 * <p>A {@code AccessTestPageDto} rekord a web modul biztonsági és jogosultságkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public record AccessTestPageDto(long totalElements,int page,int size,int totalPages,List<AccessTestRowDto> content){}
