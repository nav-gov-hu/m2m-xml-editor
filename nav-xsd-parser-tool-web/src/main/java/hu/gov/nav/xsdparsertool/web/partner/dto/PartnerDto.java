package hu.gov.nav.xsdparsertool.web.partner.dto;
import java.time.LocalDateTime;
import hu.gov.nav.xsdparsertool.web.partner.entity.PartnerEntity;
/**
 * A webes rétegek közötti adatátadás strukturált modellje.
 *
 * <p>A {@code PartnerDto} rekord a web modul partnerkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public record PartnerDto(Long id,String name,String taxNumber,String communityTaxCountry,String communityTaxNumber,String registrationNumber,String email,String phone,String fax,String permanentPostalCode,String permanentCity,String permanentPublicPlace,String permanentPublicPlaceType,String permanentHouseNumber,String permanentBuilding,String permanentStaircase,String permanentFloor,String permanentDoor,String mailingPostalCode,String mailingCity,String mailingPublicPlace,String mailingPublicPlaceType,String mailingHouseNumber,String mailingBuilding,String mailingStaircase,String mailingFloor,String mailingDoor,String contactName,String contactPhone,String contactEmail,String bankName,String bankAccountNumber,Boolean active,LocalDateTime createdAt,LocalDateTime updatedAt){
 /**
  * A {@code from} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param p a művelet bemeneti {@code p} értéke
  * @return a művelet feldolgozási eredménye
  */
 public static PartnerDto from(PartnerEntity p){return new PartnerDto(p.getId(),p.getName(),p.getTaxNumber(),p.getCommunityTaxCountry(),p.getCommunityTaxNumber(),p.getRegistrationNumber(),p.getEmail(),p.getPhone(),p.getFax(),p.getPermanentPostalCode(),p.getPermanentCity(),p.getPermanentPublicPlace(),p.getPermanentPublicPlaceType(),p.getPermanentHouseNumber(),p.getPermanentBuilding(),p.getPermanentStaircase(),p.getPermanentFloor(),p.getPermanentDoor(),p.getMailingPostalCode(),p.getMailingCity(),p.getMailingPublicPlace(),p.getMailingPublicPlaceType(),p.getMailingHouseNumber(),p.getMailingBuilding(),p.getMailingStaircase(),p.getMailingFloor(),p.getMailingDoor(),p.getContactName(),p.getContactPhone(),p.getContactEmail(),p.getBankName(),p.getBankAccountNumber(),p.getActive(),p.getCreatedAt(),p.getUpdatedAt());}
}
