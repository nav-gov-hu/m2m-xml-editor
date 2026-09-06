package hu.gov.nav.xsdparsertool.web.partner.api;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import hu.gov.nav.xsdparsertool.web.partner.dto.PartnerDto;
import hu.gov.nav.xsdparsertool.web.partner.dto.PartnerSaveRequest;
import hu.gov.nav.xsdparsertool.web.partner.service.PartnerService;
/**
 * A webes végpontokat kiszolgáló vezérlő, amely a HTTP-kéréseket a megfelelő alkalmazási szolgáltatásokhoz irányítja.
 *
 * <p>A {@code PartnerController} osztály a web modul partnerkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@RestController @RequestMapping("/api/partners") public class PartnerController{
 private final PartnerService service;  /**
  * Létrehozza a {@code PartnerController} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
  *
  * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
  * @param service a művelet bemeneti {@code service} értéke
  */
 public PartnerController(PartnerService service){this.service=service;}
 /**
  * A {@code list} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a művelet eredményeként előállított elemek listája
  */
 @GetMapping @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.AUTHENTICATED_READ) public List<PartnerDto> list(){return service.list();}
 /**
  * A {@code suggest} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param q a művelet bemeneti {@code q} értéke
  * @return a művelet eredményeként előállított elemek listája
  */
 @GetMapping("/suggest") @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.AUTHENTICATED_READ) public List<PartnerDto> suggest(@RequestParam String q){return service.suggest(q);}
 /**
  * A {@code create} művelet létrehozza vagy tartósítja a kért állapotváltozást.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param p a művelet bemeneti {@code p} értéke
  * @return a művelet feldolgozási eredménye
  */
 @PostMapping @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.OPERATOR_WRITE) public PartnerDto create(@RequestBody PartnerSaveRequest p){return service.save(null,p);}
 /**
  * A {@code update} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param id a célobjektum vagy erőforrás azonosítója
  * @param p a művelet bemeneti {@code p} értéke
  * @return a művelet feldolgozási eredménye
  */
 @PutMapping("/{id}") @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.OPERATOR_WRITE) public PartnerDto update(@PathVariable Long id,@RequestBody PartnerSaveRequest p){return service.save(id,p);}
 /**
  * A {@code deactivate} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
  *
  * <p>A művelet a partnerkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param id a célobjektum vagy erőforrás azonosítója
  */
 @DeleteMapping("/{id}") @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.OPERATOR_WRITE) public void deactivate(@PathVariable Long id){service.deactivate(id);}
}
