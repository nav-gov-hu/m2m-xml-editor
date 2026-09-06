package hu.gov.nav.xsdparsertool.web.partner.dto;

/**
 * A webes rétegek közötti adatátadás strukturált modellje.
 *
 * <p>A {@code PartnerSaveRequest} rekord a web modul partnerkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public record PartnerSaveRequest(
        String name,
        String taxNumber,
        String communityTaxCountry,
        String communityTaxNumber,
        String registrationNumber,
        String email,
        String phone,
        String fax,
        String permanentPostalCode,
        String permanentCity,
        String permanentPublicPlace,
        String permanentPublicPlaceType,
        String permanentHouseNumber,
        String permanentBuilding,
        String permanentStaircase,
        String permanentFloor,
        String permanentDoor,
        String mailingPostalCode,
        String mailingCity,
        String mailingPublicPlace,
        String mailingPublicPlaceType,
        String mailingHouseNumber,
        String mailingBuilding,
        String mailingStaircase,
        String mailingFloor,
        String mailingDoor,
        String contactName,
        String contactPhone,
        String contactEmail,
        String bankName,
        String bankAccountNumber,
        Boolean active
) {}
