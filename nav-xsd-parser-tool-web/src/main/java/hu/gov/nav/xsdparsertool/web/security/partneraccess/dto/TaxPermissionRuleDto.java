package hu.gov.nav.xsdparsertool.web.security.partneraccess.dto;
/**
 * A webes rétegek közötti adatátadás strukturált modellje.
 *
 * <p>A {@code TaxPermissionRuleDto} rekord a web modul biztonsági és jogosultságkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public record TaxPermissionRuleDto(Long id,String ruleType,String taxNumber,String vatCode,String countyCode,Integer sortOrder){}
