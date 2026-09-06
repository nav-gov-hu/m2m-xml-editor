package hu.gov.nav.xsdparsertool.web.security.partneraccess.service;

import hu.gov.nav.xsdparsertool.web.partner.entity.PartnerEntity;
import hu.gov.nav.xsdparsertool.web.security.SecurityModeProperties;
import hu.gov.nav.xsdparsertool.web.security.entity.AppUserEntity;
import hu.gov.nav.xsdparsertool.web.security.partneraccess.dto.TaxPermissionRuleDto;
import hu.gov.nav.xsdparsertool.web.security.partneraccess.entity.UserPartnerPermissionEntity;
import hu.gov.nav.xsdparsertool.web.security.partneraccess.entity.UserTaxPermissionRuleEntity;
import hu.gov.nav.xsdparsertool.web.security.partneraccess.repository.UserPartnerPermissionRepository;
import hu.gov.nav.xsdparsertool.web.security.partneraccess.repository.UserTaxPermissionRuleRepository;
import hu.gov.nav.xsdparsertool.web.security.repository.AppUserRepository;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class XmlAccessPolicyServiceSecurityTest {

    @Mock AppUserRepository users;
    @Mock UserPartnerPermissionRepository partners;
    @Mock UserTaxPermissionRuleRepository rules;

    private XmlAccessPolicyService service;

    @BeforeEach
    void setUp() {
        SecurityModeProperties mode = new SecurityModeProperties("MULTI_USER", "local-user");
        mode.validate();
        service = new XmlAccessPolicyService(mode, users, partners, rules);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void unknownAuthenticatedPrincipalMustFailClosed() {
        authenticate("unknown-principal", "ROLE_OPERATOR");
        when(users.findByUsernameIgnoreCase("unknown-principal")).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class,
                () -> service.requireCurrentUserAccess(xmlForPartner(42L, "12345678-1-42")));
        verifyNoInteractions(partners, rules);
    }

    @Test
    void adminPrincipalRemainsUnrestricted() {
        authenticate("admin", "ROLE_ADMIN");

        assertDoesNotThrow(() -> service.requireCurrentUserAccess(xmlForPartner(42L, "12345678-1-42")));
        verifyNoInteractions(users, partners, rules);
    }

    @Test
    void unauthenticatedContextMustFailClosed() {
        SecurityContextHolder.clearContext();

        assertThrows(AccessDeniedException.class,
                () -> service.requireCurrentUserAccess(xmlForPartner(42L, "12345678-1-42")));
        verifyNoInteractions(users, partners, rules);
    }

    @Test
    void directPartnerPermissionAllowsAccessButMatchingDenyRuleOverridesIt() {
        XmlFileEntity xml = xmlForPartner(42L, "12345678-1-42");
        XmlAccessPolicyService.RuleView deny = new XmlAccessPolicyService.RuleView("DENY", "12345678", null, null);

        assertTrue(service.isAllowed(xml, Set.of(42L), List.of()));
        assertFalse(service.isAllowed(xml, Set.of(42L), List.of(deny)));
    }

    @Test
    void allowRuleSupportsWildcardsAndRejectsMalformedOrMissingPartnerTaxNumber() {
        XmlAccessPolicyService.RuleView allowCounty = new XmlAccessPolicyService.RuleView("ALLOW", "12345678", "*", "42");

        assertTrue(service.isAllowed(xmlForPartner(7L, "12345678-1-42"), Set.of(), List.of(allowCounty)));
        assertFalse(service.isAllowed(xmlForPartner(7L, "12345678-1-43"), Set.of(), List.of(allowCounty)));
        assertFalse(service.isAllowed(xmlForPartner(7L, "invalid"), Set.of(), List.of()));
        assertFalse(service.isAllowed(new XmlFileEntity(), Set.of(7L), List.of()));
        assertFalse(service.isAllowed(null, Set.of(7L), List.of()));
    }

    @Test
    void draftRulesAreNormalizedAndDenyStillOverridesAllow() {
        XmlFileEntity xml = xmlForPartner(7L, "12345678-1-42");
        TaxPermissionRuleDto allow = new TaxPermissionRuleDto(null, " allow ", "12345678", "*", "42", 0);
        TaxPermissionRuleDto deny = new TaxPermissionRuleDto(null, "deny", "12345678", "1", "42", 1);

        assertTrue(service.isAllowedForDraft(xml, Set.of(), List.of(allow)));
        assertFalse(service.isAllowedForDraft(xml, Set.of(), List.of(allow, deny)));
        assertFalse(service.isAllowedForDraft(xml, Set.of(), null));
    }

    @Test
    void validateRuleNormalizesDefaultsAndRejectsInvalidDefinitions() {
        XmlAccessPolicyService.RuleView normalized = service.validateRule(
                new TaxPermissionRuleDto(null, null, "12345678", " * ", "42", 0));

        assertEquals("ALLOW", normalized.ruleType());
        assertEquals("12345678", normalized.taxNumber());
        assertEquals("*", normalized.vatCode());
        assertEquals("42", normalized.countyCode());

        assertThrows(IllegalArgumentException.class, () -> service.validateRule(null));
        assertThrows(IllegalArgumentException.class,
                () -> service.validateRule(new TaxPermissionRuleDto(null, "ALLOW", null, null, null, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> service.validateRule(new TaxPermissionRuleDto(null, "OTHER", "12345678", null, null, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> service.validateRule(new TaxPermissionRuleDto(null, "ALLOW", "123", null, null, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> service.validateRule(new TaxPermissionRuleDto(null, "ALLOW", null, "12", null, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> service.validateRule(new TaxPermissionRuleDto(null, "ALLOW", null, null, "4", 0)));
    }

    @Test
    void currentUserContextLoadsDirectPartnersAndTaxRulesForFiltering() {
        authenticate("operator", "ROLE_OPERATOR");
        AppUserEntity user = mock(AppUserEntity.class);
        when(user.getId()).thenReturn(9L);
        when(users.findByUsernameIgnoreCase("operator")).thenReturn(Optional.of(user));

        UserPartnerPermissionEntity direct = mock(UserPartnerPermissionEntity.class);
        PartnerEntity directPartner = new PartnerEntity();
        directPartner.setId(42L);
        when(direct.getPartner()).thenReturn(directPartner);
        when(partners.findByUser_IdOrderByPartner_NameAsc(9L)).thenReturn(List.of(direct));

        UserTaxPermissionRuleEntity allow = rule("ALLOW", "87654321", null, "01");
        UserTaxPermissionRuleEntity deny = rule("DENY", "12345678", null, "42");
        when(rules.findByUser_IdOrderBySortOrderAscIdAsc(9L)).thenReturn(List.of(allow, deny));

        XmlFileEntity directButDenied = xmlForPartner(42L, "12345678-1-42");
        XmlFileEntity ruleAllowed = xmlForPartner(77L, "87654321-2-01");
        XmlFileEntity unrelated = xmlForPartner(88L, "11111111-1-01");

        List<XmlFileEntity> visible = service.filterCurrentUser(List.of(directButDenied, ruleAllowed, unrelated));

        assertEquals(List.of(ruleAllowed), visible);
        verify(partners).findByUser_IdOrderByPartner_NameAsc(9L);
        verify(rules).findByUser_IdOrderBySortOrderAscIdAsc(9L);
    }

    @Test
    void idBasedAccessCheckUsesLoaderAndSamePolicy() {
        authenticate("unknown-principal", "ROLE_OPERATOR");
        when(users.findByUsernameIgnoreCase("unknown-principal")).thenReturn(Optional.empty());
        XmlFileEntity xml = xmlForPartner(42L, "12345678-1-42");

        assertThrows(AccessDeniedException.class,
                () -> service.requireCurrentUserAccess(123L, id -> {
                    assertEquals(123L, id);
                    return xml;
                }));
    }

    @Test
    void nonMultiUserModeIsUnrestrictedWithoutSecurityContextOrRepositories() {
        SecurityModeProperties localMode = new SecurityModeProperties("STANDALONE", "local-user");
        localMode.validate();
        XmlAccessPolicyService localService = new XmlAccessPolicyService(localMode, users, partners, rules);

        List<XmlFileEntity> source = List.of(xmlForPartner(42L, "12345678-1-42"));
        assertSame(source, localService.filterCurrentUser(source));
        assertDoesNotThrow(() -> localService.requireCurrentUserAccess(source.get(0)));
        verifyNoInteractions(users, partners, rules);
    }

    private void authenticate(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        username,
                        "n/a",
                        List.of(new SimpleGrantedAuthority(role))));
    }

    private static UserTaxPermissionRuleEntity rule(String type, String taxNumber, String vatCode, String countyCode) {
        UserTaxPermissionRuleEntity rule = new UserTaxPermissionRuleEntity();
        rule.setRuleType(type);
        rule.setTaxNumber(taxNumber);
        rule.setVatCode(vatCode);
        rule.setCountyCode(countyCode);
        return rule;
    }

    private static XmlFileEntity xmlForPartner(Long partnerId, String taxNumber) {
        PartnerEntity partner = new PartnerEntity();
        partner.setId(partnerId);
        partner.setTaxNumber(taxNumber);
        XmlFileEntity xml = new XmlFileEntity();
        xml.setPartner(partner);
        return xml;
    }
}
