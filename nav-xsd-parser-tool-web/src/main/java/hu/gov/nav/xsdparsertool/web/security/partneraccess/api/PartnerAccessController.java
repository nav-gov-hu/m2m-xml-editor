package hu.gov.nav.xsdparsertool.web.security.partneraccess.api;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import hu.gov.nav.xsdparsertool.core.security.AuthorizationRules;
import hu.gov.nav.xsdparsertool.web.partner.dto.PartnerDto;
import hu.gov.nav.xsdparsertool.web.security.partneraccess.dto.*;
import hu.gov.nav.xsdparsertool.web.security.partneraccess.service.PartnerAccessManagementService;
/**
 * A webes végpontokat kiszolgáló vezérlő, amely a HTTP-kéréseket a megfelelő alkalmazási szolgáltatásokhoz irányítja.
 *
 * <p>A {@code PartnerAccessController} osztály a web modul biztonsági és jogosultságkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@RestController @RequestMapping("/api/users/{userId}/partner-access") @PreAuthorize(AuthorizationRules.ADMIN_ONLY)
public class PartnerAccessController{
 private final PartnerAccessManagementService service; /**
  * Létrehozza a {@code PartnerAccessController} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
  *
  * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
  * @param service a művelet bemeneti {@code service} értéke
  */
 public PartnerAccessController(PartnerAccessManagementService service){this.service=service;}
 /**
  * A {@code get} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param userId a célobjektum vagy erőforrás azonosítója
  * @return a feloldott vagy lekért érték
  */
 @GetMapping public PartnerAccessConfigDto get(@PathVariable Long userId){return service.get(userId);}
 /**
  * A {@code save} művelet létrehozza vagy tartósítja a kért állapotváltozást.
  *
  * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param userId a célobjektum vagy erőforrás azonosítója
  * @param request a művelet bemeneti kérésadatait tartalmazó objektum
  * @return a művelet feldolgozási eredménye
  */
 @PutMapping public PartnerAccessConfigDto save(@PathVariable Long userId,@RequestBody PartnerAccessSaveRequest request){return service.save(userId,request);}
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
 @PostMapping("/test") public AccessTestPageDto test(@PathVariable Long userId,@RequestBody PartnerAccessSaveRequest request,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return service.test(userId,request,page,size);}
 /**
  * A {@code partners} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
  *
  * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param userId a célobjektum vagy erőforrás azonosítója
  * @param q a művelet bemeneti {@code q} értéke
  * @return a művelet eredményeként előállított elemek listája
  */
 @GetMapping("/partners") public List<PartnerDto> partners(@PathVariable Long userId,@RequestParam(defaultValue="")String q){return service.searchPartners(q);}
 /**
  * A {@code bad} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
  *
  * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param ex a művelet bemeneti {@code ex} értéke
  * @return a művelet feldolgozási eredménye
  */
 @ExceptionHandler({IllegalArgumentException.class,IllegalStateException.class}) public ResponseEntity<Map<String,String>> bad(RuntimeException ex){return ResponseEntity.badRequest().body(Map.of("message",ex.getMessage()));}
}
