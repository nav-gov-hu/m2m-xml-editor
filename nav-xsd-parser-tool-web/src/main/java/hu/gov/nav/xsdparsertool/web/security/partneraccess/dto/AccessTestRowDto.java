package hu.gov.nav.xsdparsertool.web.security.partneraccess.dto;
import java.time.LocalDateTime;
/**
 * A webes rétegek közötti adatátadás strukturált modellje.
 *
 * <p>A {@code AccessTestRowDto} rekord a web modul biztonsági és jogosultságkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public record AccessTestRowDto(Long id,String fileName,String partnerName,String taxNumber,String formType,String status,LocalDateTime createdAt){}
