package hu.gov.nav.xsdparsertool.web.xmlindex.api;

import hu.gov.nav.xsdparsertool.web.xmlindex.dto.XmlIndexDtos.*;
import hu.gov.nav.xsdparsertool.web.xmlindex.service.XmlIndexConfigService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * A webes végpontokat kiszolgáló vezérlő, amely a HTTP-kéréseket a megfelelő alkalmazási szolgáltatásokhoz irányítja.
 *
 * <p>A {@code XmlIndexConfigController} osztály a web modul REST API területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@RestController
@RequestMapping("/api/xml-index-config")
@PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.XML_INDEX_CONFIG_MANAGE)
public class XmlIndexConfigController {
    private final XmlIndexConfigService service;

    /**
     * Létrehozza a {@code XmlIndexConfigController} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param service a művelet bemeneti {@code service} értéke
     */
    public XmlIndexConfigController(XmlIndexConfigService service) {
        this.service = service;
    }

    /**
     * A {@code forms} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a művelet feldolgozási eredménye
     */
    @GetMapping("/forms")
    public FormsResponse forms() {
        return service.listForms();
    }

    /**
     * A {@code structure} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param formName a feloldáshoz vagy azonosításhoz használt név
     * @param sourceVersion a művelet bemeneti {@code sourceVersion} értéke
     * @return a művelet feldolgozási eredménye
     */
    @GetMapping("/structure")
    public StructureResponse structure(@RequestParam("formName") String formName,
                                       @RequestParam(name = "sourceVersion", required = false) String sourceVersion) {
        return service.structure(formName, sourceVersion);
    }

    /**
     * A {@code save} művelet létrehozza vagy tartósítja a kért állapotváltozást.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param formName a feloldáshoz vagy azonosításhoz használt név
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet feldolgozási eredménye
     */
    @PostMapping("/forms/{formName}")
    public SaveResponse save(@PathVariable String formName, @RequestBody IndexFormConfigDto request) {
        request.setFormName(formName);
        return service.save(request);
    }
}
