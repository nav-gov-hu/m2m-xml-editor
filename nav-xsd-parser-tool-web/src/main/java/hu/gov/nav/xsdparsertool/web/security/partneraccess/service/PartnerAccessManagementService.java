package hu.gov.nav.xsdparsertool.web.security.partneraccess.service;

import hu.gov.nav.xsdparsertool.web.support.RepositoryAccess;

import java.time.LocalDateTime;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import hu.gov.nav.xsdparsertool.web.partner.repository.PartnerRepository;
import hu.gov.nav.xsdparsertool.web.security.SecurityMode;
import hu.gov.nav.xsdparsertool.web.security.SecurityModeProperties;
import hu.gov.nav.xsdparsertool.web.security.entity.AppUserEntity;
import hu.gov.nav.xsdparsertool.web.security.repository.AppUserRepository;
import hu.gov.nav.xsdparsertool.web.security.service.CurrentUserService;
import hu.gov.nav.xsdparsertool.web.security.partneraccess.dto.*;
import hu.gov.nav.xsdparsertool.web.security.partneraccess.entity.*;
import hu.gov.nav.xsdparsertool.web.security.partneraccess.repository.*;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileRepository;

/**
 * A kapcsolódó webes üzleti vagy alkalmazási folyamatokat összefogó szolgáltatás.
 *
 * <p>A {@code PartnerAccessManagementService} osztály a web modul biztonsági és jogosultságkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Service
public class PartnerAccessManagementService {
 private final SecurityModeProperties mode; private final AppUserRepository users; private final PartnerRepository partnerRepository;
 private final UserPartnerPermissionRepository partnerPermissions; private final UserTaxPermissionRuleRepository ruleRepository;
 private final XmlFileRepository xmlFiles; private final XmlAccessPolicyService policy; private final CurrentUserService currentUser;
 /**
  * Létrehozza a {@code PartnerAccessManagementService} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
  *
  * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
  * @param mode a művelet bemeneti {@code mode} értéke
  * @param users a művelet felhasználói kontextusa vagy felhasználóneve
  * @param partnerRepository a művelet bemeneti {@code partnerRepository} értéke
  * @param partnerPermissions a művelet bemeneti {@code partnerPermissions} értéke
  * @param ruleRepository a művelet bemeneti {@code ruleRepository} értéke
  * @param xmlFiles a feldolgozandó XML-hez tartozó adat vagy tartalom
  * @param policy a művelet bemeneti {@code policy} értéke
  * @param currentUser a művelet felhasználói kontextusa vagy felhasználóneve
  */
 public PartnerAccessManagementService(SecurityModeProperties mode,AppUserRepository users,PartnerRepository partnerRepository,UserPartnerPermissionRepository partnerPermissions,UserTaxPermissionRuleRepository ruleRepository,XmlFileRepository xmlFiles,XmlAccessPolicyService policy,CurrentUserService currentUser){this.mode=mode;this.users=users;this.partnerRepository=partnerRepository;this.partnerPermissions=partnerPermissions;this.ruleRepository=ruleRepository;this.xmlFiles=xmlFiles;this.policy=policy;this.currentUser=currentUser;}
 /**
  * A {@code requireMultiUser} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
  *
  * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
  */
 private void requireMultiUser(){if(mode.getSecurityMode()!=SecurityMode.MULTI_USER)throw new IllegalStateException("A partnerjogosultság csak multi-user módban érhető el.");}
 /**
  * A {@code user} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
  *
  * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
  * @param id a célobjektum vagy erőforrás azonosítója
  * @return a művelet feldolgozási eredménye
  */
 private AppUserEntity user(Long id){return RepositoryAccess.findById(users, id).orElseThrow(()->new IllegalArgumentException("A felhasználó nem található."));}
 /**
  * A {@code get} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param userId a célobjektum vagy erőforrás azonosítója
  * @return a feloldott vagy lekért érték
  */
 @Transactional(readOnly=true) public PartnerAccessConfigDto get(Long userId){requireMultiUser();AppUserEntity u=user(userId);return toDto(u);}
 /**
  * A {@code save} művelet létrehozza vagy tartósítja a kért állapotváltozást.
  *
  * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param userId a célobjektum vagy erőforrás azonosítója
  * @param request a művelet bemeneti kérésadatait tartalmazó objektum
  * @return a művelet feldolgozási eredménye
  */
 @Transactional public PartnerAccessConfigDto save(Long userId,PartnerAccessSaveRequest request){requireMultiUser();AppUserEntity u=user(userId);List<Long> ids=request==null||request.partnerIds()==null?List.of():request.partnerIds().stream().filter(Objects::nonNull).distinct().toList();
  partnerPermissions.findByUser_IdOrderByPartner_NameAsc(userId).forEach(partnerPermissions::delete);
  partnerPermissions.flush();
  for(Long id:ids){var p=RepositoryAccess.findById(partnerRepository, id).orElseThrow(()->new IllegalArgumentException("A partner nem található: "+id));UserPartnerPermissionEntity e=new UserPartnerPermissionEntity();e.setUser(u);e.setPartner(p);e.setCreatedAt(LocalDateTime.now());e.setCreatedBy(actor());partnerPermissions.save(e);}
  ruleRepository.deleteByUser_Id(userId);
  ruleRepository.flush();
  int order=0;for(TaxPermissionRuleDto dto:request==null||request.rules()==null?List.<TaxPermissionRuleDto>of():request.rules()){var v=policy.validateRule(dto);UserTaxPermissionRuleEntity e=new UserTaxPermissionRuleEntity();e.setUser(u);e.setRuleType(v.ruleType());e.setTaxNumber(v.taxNumber());e.setVatCode(v.vatCode());e.setCountyCode(v.countyCode());e.setSortOrder(order++);e.setCreatedAt(LocalDateTime.now());e.setCreatedBy(actor());ruleRepository.save(e);}return toDto(u);
 }
 /**
  * A {@code test} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
  *
  * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param userId a célobjektum vagy erőforrás azonosítója
  * @param request a művelet bemeneti kérésadatait tartalmazó objektum
  * @param page a lapozási vagy mennyiségi korlátot meghatározó érték
  * @param size a lapozási vagy mennyiségi korlátot meghatározó érték
  * @return a művelet feldolgozási eredménye
  */
 @Transactional(readOnly=true) public AccessTestPageDto test(Long userId,PartnerAccessSaveRequest request,int page,int size){requireMultiUser();user(userId);if(page<0||page>1_000_000)throw new IllegalArgumentException("Az oldalszám 0 és 1000000 közötti lehet.");if(size<1||size>100)throw new IllegalArgumentException("Az oldalméret 1 és 100 közötti lehet.");int safePage=page,safeSize=size;Set<Long> partnerIds=new HashSet<>(request==null||request.partnerIds()==null?List.of():request.partnerIds());List<TaxPermissionRuleDto> rules=request==null||request.rules()==null?List.of():request.rules();rules.forEach(policy::validateRule);
  List<XmlFileEntity> matches=xmlFiles.findByArchivedFalseOrderByCreatedAtDesc().stream().filter(x->policy.isAllowedForDraft(x,partnerIds,rules)).toList();long offset=(long)safePage*(long)safeSize;int from=offset>=matches.size()?matches.size():(int)offset;long endOffset=(long)from+(long)safeSize;int to=endOffset>=matches.size()?matches.size():(int)endOffset;List<AccessTestRowDto> rows=matches.subList(from,to).stream().map(x->new AccessTestRowDto(x.getId(),x.getFileName(),x.getPartner()==null?null:x.getPartner().getName(),x.getPartner()==null?null:x.getPartner().getTaxNumber(),x.getFormType(),x.getStatus(),x.getCreatedAt())).toList();int pages=matches.isEmpty()?0:(int)(((long)matches.size()+safeSize-1L)/safeSize);return new AccessTestPageDto(matches.size(),safePage,safeSize,pages,rows);
 }
 /**
  * A {@code searchPartners} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
  *
  * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param q a művelet bemeneti {@code q} értéke
  * @return a művelet eredményeként előállított elemek listája
  */
 @Transactional(readOnly=true) public List<hu.gov.nav.xsdparsertool.web.partner.dto.PartnerDto> searchPartners(String q){requireMultiUser();String s=q==null?"":q.trim();if(s.length()<2)return List.of();try{return partnerRepository.suggest(s,PageRequest.of(0,20)).stream().map(hu.gov.nav.xsdparsertool.web.partner.dto.PartnerDto::from).toList();}catch(RuntimeException ex){throw new IllegalStateException("A partnerkeresés sikertelen.",ex);}}
 /**
  * A {@code toDto} művelet előállítja a hívó réteg által használt reprezentációt.
  *
  * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param u a művelet bemeneti {@code u} értéke
  * @return a művelet feldolgozási eredménye
  */
 private PartnerAccessConfigDto toDto(AppUserEntity u){List<PartnerPermissionDto> ps=partnerPermissions.findByUser_IdOrderByPartner_NameAsc(u.getId()).stream().map(e->new PartnerPermissionDto(e.getId(),e.getPartner().getId(),e.getPartner().getName(),e.getPartner().getTaxNumber())).toList();List<TaxPermissionRuleDto> rs=ruleRepository.findByUser_IdOrderBySortOrderAscIdAsc(u.getId()).stream().map(e->new TaxPermissionRuleDto(e.getId(),e.getRuleType(),e.getTaxNumber(),e.getVatCode(),e.getCountyCode(),e.getSortOrder())).toList();return new PartnerAccessConfigDto(u.getId(),u.getUsername(),ps,rs);}
 /**
  * A {@code actor} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
  *
  * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a művelet feldolgozási eredménye
  */
 private String actor(){String a=currentUser.getCurrentUsername();return a==null?"system":a;}
}
