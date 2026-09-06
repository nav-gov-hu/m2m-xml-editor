package hu.gov.nav.xsdparsertool.web.partner.service;

import hu.gov.nav.xsdparsertool.web.support.RepositoryAccess;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import hu.gov.nav.xsdparsertool.web.partner.dto.PartnerDto;
import hu.gov.nav.xsdparsertool.web.partner.dto.PartnerSaveRequest;
import hu.gov.nav.xsdparsertool.web.partner.entity.PartnerEntity;
import hu.gov.nav.xsdparsertool.web.partner.repository.PartnerRepository;
import hu.gov.nav.xsdparsertool.web.security.partneraccess.service.XmlAccessPolicyService;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileRepository;

/**
 * A kapcsolódó webes üzleti vagy alkalmazási folyamatokat összefogó szolgáltatás.
 *
 * <p>A {@code PartnerService} osztály a web modul partnerkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Service
public class PartnerService {
 private final PartnerRepository repository;
 private final XmlFileRepository xmlFiles;
 private final XmlAccessPolicyService accessPolicy;
 /**
  * Létrehozza a {@code PartnerService} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
  *
  * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
  * @param repository a művelet bemeneti {@code repository} értéke
  * @param xmlFiles a feldolgozandó XML-hez tartozó adat vagy tartalom
  * @param accessPolicy a művelet bemeneti {@code accessPolicy} értéke
  */
 public PartnerService(PartnerRepository repository,XmlFileRepository xmlFiles,XmlAccessPolicyService accessPolicy){this.repository=repository;this.xmlFiles=xmlFiles;this.accessPolicy=accessPolicy;}
 /**
  * A {@code list} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a művelet eredményeként előállított elemek listája
  */
 @Transactional(readOnly=true) public List<PartnerDto> list(){Set<Long> visible=visiblePartnerIds();return repository.findAllByOrderByNameAsc().stream().filter(p->visible==null||visible.contains(p.getId())).map(PartnerDto::from).toList();}
 /**
  * A {@code suggest} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param q a művelet bemeneti {@code q} értéke
  * @return a művelet eredményeként előállított elemek listája
  */
 @Transactional(readOnly=true) public List<PartnerDto> suggest(String q){String s=q==null?"":q.trim();if(s.length()<2)return List.of();Set<Long> visible=visiblePartnerIds();try{return repository.suggest(s,PageRequest.of(0,50)).stream().filter(p->visible==null||visible.contains(p.getId())).limit(10).map(PartnerDto::from).toList();}catch(RuntimeException ex){throw new IllegalStateException("A partnerkeresés sikertelen.",ex);}}
 /**
  * A {@code visiblePartnerIds} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a művelet eredményeként előállított egyedi elemek halmaza
  */
 private Set<Long> visiblePartnerIds(){List<hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileEntity> all=xmlFiles.findByArchivedFalseOrderByCreatedAtDesc();List<hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileEntity> visible=accessPolicy.filterCurrentUser(all);if(visible==all)return null;return visible.stream().filter(x->x.getPartner()!=null).map(x->x.getPartner().getId()).collect(Collectors.toSet());}
 /**
  * A {@code save} művelet létrehozza vagy tartósítja a kért állapotváltozást.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param id a célobjektum vagy erőforrás azonosítója
  * @param input a művelet bemeneti {@code input} értéke
  * @return a művelet feldolgozási eredménye
  */
 @Transactional public PartnerDto save(Long id,PartnerSaveRequest input){String name=req(input.name(),"A partner megnevezése kötelező.");String tax=validateTaxNumber(req(input.taxNumber(),"Az adószám kötelező."));PartnerEntity p=id==null?new PartnerEntity():RepositoryAccess.findById(repository, id).orElseThrow();if(id==null&&repository.existsByTaxNumberIgnoreCase(tax))throw new IllegalArgumentException("Ezzel az adószámmal már létezik partner.");copy(input,p);p.setName(name);p.setTaxNumber(tax);return PartnerDto.from(repository.save(p));}
 /**
  * A {@code resolveOrCreateImportedPartner} művelet feloldja a megfelelő erőforrást, állapotot vagy értéket a rendelkezésre álló jelöltek közül.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param taxNumber a művelet bemeneti {@code taxNumber} értéke
  * @param partnerName a feloldáshoz vagy azonosításhoz használt név
  * @return a feloldott vagy lekért érték
  */
 @Transactional public PartnerEntity resolveOrCreateImportedPartner(String taxNumber,String partnerName){String tax=validateTaxNumber(req(taxNumber,"A partner kísérőfájlban az adószám kötelező."));return repository.findFirstByTaxNumberIgnoreCase(tax).orElseGet(()->{PartnerEntity p=new PartnerEntity();p.setTaxNumber(tax);p.setName(req(partnerName,"A partner kísérőfájlban a partner neve kötelező."));p.setActive(Boolean.TRUE);return repository.save(p);});}
 /**
  * A {@code deactivate} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param id a célobjektum vagy erőforrás azonosítója
  */
 @Transactional public void deactivate(Long id){PartnerEntity p=RepositoryAccess.findById(repository, id).orElseThrow();p.setActive(false);repository.save(p);}
 /**
  * A {@code require} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param id a célobjektum vagy erőforrás azonosítója
  * @return a művelet feldolgozási eredménye
  */
 @Transactional(readOnly=true) public PartnerEntity require(Long id){return id==null?null:RepositoryAccess.findById(repository, id).orElseThrow(()->new IllegalArgumentException("A kiválasztott partner nem található."));}
 /**
  * A {@code req} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param v a művelet bemeneti {@code v} értéke
  * @param m a művelet bemeneti {@code m} értéke
  * @return a művelet feldolgozási eredménye
  */
 private String req(String v,String m){if(v==null||v.isBlank())throw new IllegalArgumentException(m);return v.trim();}
 /**
  * A {@code validateTaxNumber} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
  *
  * <p>Az ellenőrzési eredményt a webes megjelenítés és a további üzleti döntések számára konzisztens formában állítja elő.</p>
  * @param value a művelet bemeneti {@code value} értéke
  * @return a művelet feldolgozási eredménye
  */
 private String validateTaxNumber(String value){String tax=value.replaceAll("\\s+","");if(!tax.matches("\\d{8}-\\d-\\d{2}"))throw new IllegalArgumentException("Az adószám formátuma 8-1-2 legyen, például 12345676-1-42.");String core=tax.substring(0,8);int[] weights={9,7,3,1,9,7,3};int sum=0;for(int i=0;i<weights.length;i++){int digit=Character.digit(core.charAt(i),10);if(digit<0||digit>9)throw new IllegalArgumentException("Az adószám csak számjegyeket tartalmazhat.");sum=Math.addExact(sum,Math.multiplyExact(digit,weights[i]));}int expected=Math.floorMod(10-Math.floorMod(sum,10),10);if(Character.digit(core.charAt(7),10)!=expected)throw new IllegalArgumentException("Az adószám első nyolc számjegyének CDV ellenőrzése sikertelen.");return tax;}
 /**
  * A {@code copy} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param a a művelet bemeneti {@code a} értéke
  * @param b a művelet bemeneti {@code b} értéke
  */
 private void copy(PartnerSaveRequest a,PartnerEntity b){b.setCommunityTaxCountry(a.communityTaxCountry());b.setCommunityTaxNumber(a.communityTaxNumber());b.setRegistrationNumber(a.registrationNumber());b.setEmail(a.email());b.setPhone(a.phone());b.setFax(a.fax());b.setPermanentPostalCode(a.permanentPostalCode());b.setPermanentCity(a.permanentCity());b.setPermanentPublicPlace(a.permanentPublicPlace());b.setPermanentPublicPlaceType(a.permanentPublicPlaceType());b.setPermanentHouseNumber(a.permanentHouseNumber());b.setPermanentBuilding(a.permanentBuilding());b.setPermanentStaircase(a.permanentStaircase());b.setPermanentFloor(a.permanentFloor());b.setPermanentDoor(a.permanentDoor());b.setMailingPostalCode(a.mailingPostalCode());b.setMailingCity(a.mailingCity());b.setMailingPublicPlace(a.mailingPublicPlace());b.setMailingPublicPlaceType(a.mailingPublicPlaceType());b.setMailingHouseNumber(a.mailingHouseNumber());b.setMailingBuilding(a.mailingBuilding());b.setMailingStaircase(a.mailingStaircase());b.setMailingFloor(a.mailingFloor());b.setMailingDoor(a.mailingDoor());b.setContactName(a.contactName());b.setContactPhone(a.contactPhone());b.setContactEmail(a.contactEmail());b.setBankName(a.bankName());b.setBankAccountNumber(a.bankAccountNumber());b.setActive(a.active());if(a.active()==null)b.setActive(Boolean.TRUE);}
}
