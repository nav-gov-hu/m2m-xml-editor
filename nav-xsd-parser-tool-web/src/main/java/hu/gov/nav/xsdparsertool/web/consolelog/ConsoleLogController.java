package hu.gov.nav.xsdparsertool.web.consolelog;
import java.util.List; import org.springframework.web.bind.annotation.*; import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.security.access.prepost.PreAuthorize; import hu.gov.nav.xsdparsertool.core.security.AuthorizationRules;
/**
 * A webes végpontokat kiszolgáló vezérlő, amely a HTTP-kéréseket a megfelelő alkalmazási szolgáltatásokhoz irányítja.
 *
 * <p>A {@code ConsoleLogController} osztály a web modul alkalmazási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@RestController @RequestMapping("/api/admin/console-log") @PreAuthorize(AuthorizationRules.ADMIN_ONLY)
public class ConsoleLogController { private final ConsoleLogService service; /**
 * Létrehozza a {@code ConsoleLogController} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
 *
 * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
 * @param service a művelet bemeneti {@code service} értéke
 */
public ConsoleLogController(ConsoleLogService service){this.service=service;} /**
 * A {@code list} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
 *
 * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
 * @param limit a lapozási vagy mennyiségi korlátot meghatározó érték
 * @return a művelet eredményeként előállított elemek listája
 */
@GetMapping public List<ConsoleLogService.Entry> list(@RequestParam(defaultValue="500") int limit){if(limit<1||limit>3000)throw new IllegalArgumentException("A limit értéke 1 és 3000 közötti lehet.");return service.snapshot(limit);} /**
 * A {@code stream} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
 *
 * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
 * @return a művelet feldolgozási eredménye
 */
@GetMapping(path="/stream",produces="text/event-stream") public SseEmitter stream(){return service.subscribe();}}
