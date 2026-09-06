package hu.gov.nav.xsdparsertool.web.security.partneraccess.dto;
import java.util.List;
/**
 * A webes rétegek közötti adatátadás strukturált modellje.
 *
 * <p>A {@code PartnerAccessSaveRequest} rekord a web modul biztonsági és jogosultságkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public record PartnerAccessSaveRequest(List<Long> partnerIds,List<TaxPermissionRuleDto> rules){}
