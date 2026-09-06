package hu.gov.nav.xsdparsertool.web.security.partneraccess.service;

import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import hu.gov.nav.xsdparsertool.web.security.SecurityMode;
import hu.gov.nav.xsdparsertool.web.security.SecurityModeProperties;
import hu.gov.nav.xsdparsertool.web.security.entity.AppUserEntity;
import hu.gov.nav.xsdparsertool.web.security.repository.AppUserRepository;
import hu.gov.nav.xsdparsertool.web.security.partneraccess.dto.TaxPermissionRuleDto;
import hu.gov.nav.xsdparsertool.web.security.partneraccess.entity.UserTaxPermissionRuleEntity;
import hu.gov.nav.xsdparsertool.web.security.partneraccess.repository.UserPartnerPermissionRepository;
import hu.gov.nav.xsdparsertool.web.security.partneraccess.repository.UserTaxPermissionRuleRepository;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileEntity;

/**
 * A kapcsolódó webes üzleti vagy alkalmazási folyamatokat összefogó szolgáltatás.
 *
 * <p>A {@code XmlAccessPolicyService} osztály a web modul biztonsági és jogosultságkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Service
public class XmlAccessPolicyService {
 private final SecurityModeProperties mode;
 private final AppUserRepository users;
 private final UserPartnerPermissionRepository partners;
 private final UserTaxPermissionRuleRepository rules;
 /**
  * Létrehozza a {@code XmlAccessPolicyService} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
  *
  * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
  * @param mode a művelet bemeneti {@code mode} értéke
  * @param users a művelet felhasználói kontextusa vagy felhasználóneve
  * @param partners a művelet bemeneti {@code partners} értéke
  * @param rules a művelet bemeneti {@code rules} értéke
  */
 public XmlAccessPolicyService(SecurityModeProperties mode,AppUserRepository users,UserPartnerPermissionRepository partners,UserTaxPermissionRuleRepository rules){this.mode=mode;this.users=users;this.partners=partners;this.rules=rules;}

 /**
  * A {@code filterCurrentUser} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
  *
  * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
  * @param source a feldolgozandó elemek kollekciója
  * @return a művelet eredményeként előállított elemek listája
  */
 @Transactional(readOnly=true)
 public List<XmlFileEntity> filterCurrentUser(List<XmlFileEntity> source){
  AccessContext ctx=currentContext();
  if(ctx.unrestricted()) return source;
  return source.stream().filter(x->isAllowed(x,ctx.partnerIds(),ctx.rules())).toList();
 }

 /**
  * A {@code requireCurrentUserAccess} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
  *
  * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
  * @param xml a feldolgozandó XML-hez tartozó adat vagy tartalom
  */
 @Transactional(readOnly=true)
 public void requireCurrentUserAccess(XmlFileEntity xml){
  AccessContext ctx=currentContext();
  if(!ctx.unrestricted()&&!isAllowed(xml,ctx.partnerIds(),ctx.rules())) throw new AccessDeniedException("Az XML állományhoz nincs partnerjogosultság.");
 }

 /**
  * A {@code requireCurrentUserAccess} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
  *
  * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
  * @param xmlId a célobjektum vagy erőforrás azonosítója
  * @param loader a művelet bemeneti {@code loader} értéke
  */
 @Transactional(readOnly=true)
 public void requireCurrentUserAccess(Long xmlId,java.util.function.Function<Long,XmlFileEntity> loader){requireCurrentUserAccess(loader.apply(xmlId));}

 /**
  * A {@code isAllowed} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
  *
  * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param xml a feldolgozandó XML-hez tartozó adat vagy tartalom
  * @param directPartnerIds a feldolgozandó elemek kollekciója
  * @param configuredRules a művelethez szükséges konfigurációs adatok
  * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
  */
 public boolean isAllowed(XmlFileEntity xml,Set<Long> directPartnerIds,List<? extends RuleView> configuredRules){
  if(xml==null||xml.getPartner()==null) return false;
  String tax=normalizeTax(xml.getPartner().getTaxNumber());
  boolean direct=directPartnerIds.contains(xml.getPartner().getId());
  boolean allowedByRule=configuredRules.stream().filter(r->"ALLOW".equals(r.ruleType())).anyMatch(r->matches(r,tax));
  boolean denied=configuredRules.stream().filter(r->"DENY".equals(r.ruleType())).anyMatch(r->matches(r,tax));
  return (direct||allowedByRule)&&!denied;
 }

 /**
  * A {@code isAllowedForDraft} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
  *
  * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param xml a feldolgozandó XML-hez tartozó adat vagy tartalom
  * @param directPartnerIds a feldolgozandó elemek kollekciója
  * @param draftRules a feldolgozandó elemek kollekciója
  * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
  */
 public boolean isAllowedForDraft(XmlFileEntity xml,Set<Long> directPartnerIds,List<TaxPermissionRuleDto> draftRules){
  List<RuleView> views=draftRules==null?List.of():draftRules.stream().map(r->new RuleView(normalizeType(r.ruleType()),normalizePart(r.taxNumber(),8,"adószám"),normalizePart(r.vatCode(),1,"áfakód"),normalizePart(r.countyCode(),2,"megyekód"))).toList();
  return isAllowed(xml,directPartnerIds,views);
 }

 /**
  * A {@code validateRule} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
  *
  * <p>Az ellenőrzési eredményt a webes megjelenítés és a további üzleti döntések számára konzisztens formában állítja elő.</p>
  * @param rule a művelet bemeneti {@code rule} értéke
  * @return a művelet feldolgozási eredménye
  */
 public RuleView validateRule(TaxPermissionRuleDto rule){
  if(rule==null) throw new IllegalArgumentException("Üres szabály nem menthető.");
  RuleView view=new RuleView(normalizeType(rule.ruleType()),normalizePart(rule.taxNumber(),8,"adószám"),normalizePart(rule.vatCode(),1,"áfakód"),normalizePart(rule.countyCode(),2,"megyekód"));
  if(view.taxNumber()==null&&view.vatCode()==null&&view.countyCode()==null) throw new IllegalArgumentException("A szabályban legalább egy feltételt meg kell adni.");
  return view;
 }

 /**
  * A {@code currentContext} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
  *
  * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a művelet feldolgozási eredménye
  */
 private AccessContext currentContext(){
  if(mode.getSecurityMode()!=SecurityMode.MULTI_USER) return AccessContext.unrestrictedContext();
  Authentication auth=SecurityContextHolder.getContext().getAuthentication();
  if(auth==null||!auth.isAuthenticated()) return new AccessContext(false,Set.of(),List.of());
  boolean admin=auth.getAuthorities().stream().anyMatch(a->"ROLE_ADMIN".equals(a.getAuthority()));
  if(admin) return AccessContext.unrestrictedContext();
  Optional<AppUserEntity> user=users.findByUsernameIgnoreCase(auth.getName());
  if(user.isEmpty()) return new AccessContext(false,Set.of(),List.of());
  Long userId=user.get().getId();
  Set<Long> partnerIds=new HashSet<>();
  partners.findByUser_IdOrderByPartner_NameAsc(userId).forEach(p->partnerIds.add(p.getPartner().getId()));
  List<RuleView> values=rules.findByUser_IdOrderBySortOrderAscIdAsc(userId).stream().map(this::toView).toList();
  return new AccessContext(false,partnerIds,values);
 }
 /**
  * A {@code toView} művelet előállítja a hívó réteg által használt reprezentációt.
  *
  * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param r a művelet bemeneti {@code r} értéke
  * @return a művelet feldolgozási eredménye
  */
 private RuleView toView(UserTaxPermissionRuleEntity r){return new RuleView(r.getRuleType(),r.getTaxNumber(),r.getVatCode(),r.getCountyCode());}
 /**
  * A {@code matches} művelet feloldja a megfelelő erőforrást, állapotot vagy értéket a rendelkezésre álló jelöltek közül.
  *
  * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param rule a művelet bemeneti {@code rule} értéke
  * @param tax a művelet bemeneti {@code tax} értéke
  * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
  */
 private boolean matches(RuleView rule,String tax){
  if(tax==null) return false;
  String[] p=tax.split("-"); if(p.length!=3) return false;
  return partMatches(rule.taxNumber(),p[0])&&partMatches(rule.vatCode(),p[1])&&partMatches(rule.countyCode(),p[2]);
 }
 /**
  * A {@code partMatches} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
  *
  * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param expected a művelet bemeneti {@code expected} értéke
  * @param actual a művelet bemeneti {@code actual} értéke
  * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
  */
 private boolean partMatches(String expected,String actual){return expected==null||("*".equals(expected)?actual!=null&&!actual.isBlank():expected.equals(actual));}
 /**
  * A {@code normalizeTax} művelet feldolgozza és normalizálja a bemeneti adatot a további feldolgozás számára.
  *
  * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param value a művelet bemeneti {@code value} értéke
  * @return a művelet feldolgozási eredménye
  */
 private String normalizeTax(String value){if(value==null)return null;String v=value.replaceAll("\\s+","");return v.matches("\\d{8}-\\d-\\d{2}")?v:null;}
 /**
  * A {@code normalizeType} művelet feldolgozza és normalizálja a bemeneti adatot a további feldolgozás számára.
  *
  * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param value a művelet bemeneti {@code value} értéke
  * @return a művelet feldolgozási eredménye
  */
 private String normalizeType(String value){String v=value==null?"ALLOW":value.trim().toUpperCase(Locale.ROOT);if(!Set.of("ALLOW","DENY").contains(v))throw new IllegalArgumentException("A szabály típusa ALLOW vagy DENY lehet.");return v;}
 /**
  * A {@code normalizePart} művelet feldolgozza és normalizálja a bemeneti adatot a további feldolgozás számára.
  *
  * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param value a művelet bemeneti {@code value} értéke
  * @param length a művelet bemeneti {@code length} értéke
  * @param label a művelet bemeneti {@code label} értéke
  * @return a művelet feldolgozási eredménye
  */
 private String normalizePart(String value,int length,String label){if(value==null||value.isBlank())return null;String v=value.trim();if("*".equals(v))return v;if(!v.matches("\\d{"+length+"}"))throw new IllegalArgumentException("A(z) "+label+" értéke "+length+" számjegy vagy * lehet.");return v;}
 /**
  * A web modul biztonsági és jogosultságkezelési területének közös alkalmazási típusa.
  *
  * <p>A {@code RuleView} rekord a web modul biztonsági és jogosultságkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
  */
 public record RuleView(String ruleType,String taxNumber,String vatCode,String countyCode){}
 /**
  * A web modul biztonsági és jogosultságkezelési területének közös alkalmazási típusa.
  *
  * <p>A {@code AccessContext} rekord a web modul biztonsági és jogosultságkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
  */
 private record AccessContext(boolean unrestricted,Set<Long> partnerIds,List<RuleView> rules){ /**
  * A {@code unrestrictedContext} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
  *
  * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a művelet feldolgozási eredménye
  */
 static AccessContext unrestrictedContext(){return new AccessContext(true,Set.of(),List.of());}}
}
